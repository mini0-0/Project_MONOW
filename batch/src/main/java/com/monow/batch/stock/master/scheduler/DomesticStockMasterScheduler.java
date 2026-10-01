package com.monow.batch.stock.master.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class DomesticStockMasterScheduler {

    private final JobLauncher jobLauncher;
    private final Job domesticStockMasterJob;

    public DomesticStockMasterScheduler(
        JobLauncher jobLauncher,
        @Qualifier("domesticStockMasterJob")  Job domesticStockMasterJob
    ) {
        this.jobLauncher = jobLauncher;
        this.domesticStockMasterJob = domesticStockMasterJob;
    }

    @Scheduled(cron = "0 0 1 1 * *", zone = "Asia/Seoul")
    public void runDomesticStockMasterJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("masterDate", LocalDate.now(ZoneId.of("Asia/Seoul")).toString())
                .toJobParameters();

        jobLauncher.run(domesticStockMasterJob, jobParameters);
    }

}
