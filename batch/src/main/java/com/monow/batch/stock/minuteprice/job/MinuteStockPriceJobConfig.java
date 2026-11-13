package com.monow.batch.stock.minuteprice.job;

import com.monow.batch.stock.minuteprice.application.MinuteStockPriceRetryService;
import com.monow.batch.stock.minuteprice.dto.MinutePriceWindow;
import com.monow.batch.stock.minuteprice.dto.MinuteStockTarget;
import com.monow.batch.stock.minuteprice.mapper.KisMinutePriceMapper;
import com.monow.batch.stock.minuteprice.partition.MinutePricePartitionService;
import com.monow.batch.stock.minuteprice.processor.MinuteStockPriceProcessor;
import com.monow.batch.stock.minuteprice.reader.MinuteStockTargetReader;
import com.monow.batch.stock.minuteprice.writer.MinuteStockPriceWriter;
import com.monow.domain.stock.model.StockPriceMinute;
import com.monow.external.kis.auth.application.KisAccessTokenProvider;
import com.monow.external.kis.config.KisProperties;
import com.monow.external.kis.stock.client.KisMinutePriceClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableRetry
@ConditionalOnProperty(prefix = "monow.batch.minute-price", name = "enabled", havingValue = "true")
public class MinuteStockPriceJobConfig {

    @Bean
    @ConditionalOnMissingBean(KisMinutePriceClient.class)
    public KisMinutePriceClient kisMinutePriceClient(KisProperties properties) {
        return new KisMinutePriceClient(properties);
    }

    @Bean
    public MinuteStockPriceRetryService minuteStockPriceRetryService(
            KisAccessTokenProvider tokenProvider, KisMinutePriceClient client,
            @Value("${monow.batch.minute-price.request-interval-ms:200}") long interval) {
        if (interval < 50) {
            throw new IllegalArgumentException("수집 호출 간격은 최소 50ms이며 실제 KIS 허용량에 맞춰야 합니다.");
        }
        return new MinuteStockPriceRetryService(tokenProvider, client, interval);
    }

    @Bean
    public MinutePricePartitionService minutePricePartitionService(JdbcTemplate jdbcTemplate) {
        return new MinutePricePartitionService(jdbcTemplate);
    }

    @Bean
    public MinutePriceExecutionLock minutePriceExecutionLock(DataSource dataSource) {
        return new MinutePriceExecutionLock(dataSource);
    }

    @Bean
    @StepScope
    public MinuteStockTargetReader minuteStockTargetReader(JdbcTemplate jdbcTemplate) {
        return new MinuteStockTargetReader(jdbcTemplate);
    }

    @Bean
    @StepScope
    public MinuteStockPriceProcessor minuteStockPriceProcessor(MinuteStockPriceRetryService retryService,
            Clock clock, @Value("#{stepExecution.jobExecution}") JobExecution execution) {
        return new MinuteStockPriceProcessor(retryService, new KisMinutePriceMapper(),
                MinutePriceWindow.from(execution.getJobParameters(), clock), clock);
    }

    @Bean
    public MinuteStockPriceWriter minuteStockPriceWriter(DataSource dataSource) {
        return new MinuteStockPriceWriter(dataSource);
    }

    @Bean
    public Step minutePricePreflightStep(JobRepository repository, PlatformTransactionManager transactionManager,
            @Qualifier("minutePricePartitionService") MinutePricePartitionService partitions, Clock clock) {
        return new StepBuilder("minutePricePreflightStep", repository)
                .tasklet((contribution, context) -> {
                    MinutePriceWindow window = MinutePriceWindow.from(
                            contribution.getStepExecution().getJobExecution().getJobParameters(), clock);
                    partitions.requirePartition(YearMonth.from(window.tradeDate()));
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step minutePriceCollectionStep(JobRepository repository, PlatformTransactionManager transactionManager,
            MinuteStockTargetReader reader, MinuteStockPriceProcessor processor, MinuteStockPriceWriter writer) {
        return new StepBuilder("minutePriceCollectionStep", repository)
                .<MinuteStockTarget, List<StockPriceMinute>>chunk(1, transactionManager)
                .reader(reader).processor(processor).writer(writer)
                .build();
    }

    @Bean
    public Job minuteStockPriceJob(JobRepository repository, Clock clock, MinutePriceExecutionLock executionLock,
            @Qualifier("minutePricePreflightStep") Step preflight,
            @Qualifier("minutePriceCollectionStep") Step collection) {
        return new JobBuilder("minuteStockPriceJob", repository)
                .validator(parameters -> {
                    try {
                        MinutePriceWindow.from(parameters, clock);
                    } catch (RuntimeException exception) {
                        throw new JobParametersInvalidException("분봉 수집 파라미터 확인 필요: " + exception.getMessage());
                    }
                })
                .listener(executionLock).start(preflight).next(collection).build();
    }
}
