package com.monow.batch.stock.dailyprice.job;

import com.monow.batch.stock.dailyprice.processor.DailyStockPriceProcessor;
import com.monow.batch.stock.dailyprice.writer.DailyStockPriceWriter;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.entity.StockPriceDaily;
import com.monow.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DailyStockPriceJobConfig {
    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;

    private final PlatformTransactionManager transactionManager;

    private final StockRepository stockRepository;

    private final DailyStockPriceProcessor dailyStockPriceProcessor;

    private final DailyStockPriceWriter dailyStockPriceWriter;

    @Bean
    public Job dailyStockPriceJob(@Qualifier("dailyStockPriceStep") Step dailyStockPriceStep) {

        return new JobBuilder("dailyStockPriceJob", jobRepository)
                .start(dailyStockPriceStep)
                .build();

    }

    @Bean
    public Step dailyStockPriceStep( @Qualifier("dailyStockPriceReader") ItemReader<Stock> dailyStockPriceReader) {

        return new StepBuilder("dailyStockPriceStep", jobRepository)
                .<Stock, List<StockPriceDaily>>chunk(CHUNK_SIZE, transactionManager)
                .reader(dailyStockPriceReader)
                .processor(dailyStockPriceProcessor)
                .writer(dailyStockPriceWriter)
                .build();


    }


    @Bean
    @StepScope
    public ItemReader<Stock> dailyStockPriceReader() {
        return new ListItemReader<>(stockRepository.findAll());
    }

}
