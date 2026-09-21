package com.monow.batch.stock.master.job;

import com.monow.batch.stock.master.processor.DomesticStockMasterProcessor;
import com.monow.batch.stock.master.reader.DomesticStockMasterReader;
import com.monow.batch.stock.master.writer.DomesticStockMasterWriter;
import com.monow.domain.stock.entity.Stock;
import com.monow.batch.stock.master.dto.DomesticStockMasterItem;
import lombok.RequiredArgsConstructor;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


@Configuration
@RequiredArgsConstructor
public class DomesticStockMasterJobConfig {

    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;

    private final PlatformTransactionManager transactionManager;

    private final DomesticStockMasterReader domesticStockMasterReader;

    private final DomesticStockMasterProcessor domesticStockMasterProcessor;

    private final DomesticStockMasterWriter domesticStockMasterWriter;

    @Bean
    public Job domesticStockMasterJob(@Qualifier("domesticStockMasterStep") Step domesticStockMasterStep) {

        return new JobBuilder("domesticStockMasterJob", jobRepository)
                .start(domesticStockMasterStep)
                .build();
    }

    @Bean
    public Step domesticStockMasterStep() {

        return new StepBuilder("domesticStockMasterStep", jobRepository)
                .<DomesticStockMasterItem, Stock>chunk(CHUNK_SIZE, transactionManager)
                .reader(domesticStockMasterReader)
                .processor(domesticStockMasterProcessor)
                .writer(domesticStockMasterWriter)
                .build();

    }

}
