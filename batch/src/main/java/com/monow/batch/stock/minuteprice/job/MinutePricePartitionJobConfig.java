package com.monow.batch.stock.minuteprice.job;

import com.monow.batch.stock.minuteprice.partition.MinutePricePartitionService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.YearMonth;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "monow.batch.minute-partition", name = "enabled", havingValue = "true")
public class MinutePricePartitionJobConfig {
    @Bean
    public MinutePricePartitionService minutePartitionMaintenanceService(JdbcTemplate jdbcTemplate) {
        return new MinutePricePartitionService(jdbcTemplate);
    }

    @Bean
    public Step minutePricePartitionStep(JobRepository repository, PlatformTransactionManager transactionManager,
            @Qualifier("minutePartitionMaintenanceService") MinutePricePartitionService service) {
        return new StepBuilder("minutePricePartitionStep", repository)
                .tasklet((contribution, context) -> {
                    String month = contribution.getStepExecution().getJobExecution().getJobParameters().getString("month");
                    service.ensurePartition(YearMonth.parse(month));
                    return RepeatStatus.FINISHED;
                }, transactionManager).allowStartIfComplete(true).build();
    }

    @Bean
    public Job minutePricePartitionJob(JobRepository repository,
            @Qualifier("minutePricePartitionStep") Step step) {
        return new JobBuilder("minutePricePartitionJob", repository)
                .validator(parameters -> {
                    try {
                        int year = YearMonth.parse(parameters.getString("month")).getYear();
                        if (year < 1900 || year > 9998) {
                            throw new IllegalArgumentException();
                        }
                    } catch (RuntimeException exception) {
                        throw new JobParametersInvalidException("month는 유효한 YYYY-MM 형식이어야 합니다.");
                    }
                }).start(step).build();
    }
}
