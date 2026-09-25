package com.monow.batch.stock.dailyprice.scheduler;

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
public class DailyStockPriceScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyStockPriceJob;

    public DailyStockPriceScheduler(
            JobLauncher jobLauncher,
            @Qualifier("dailyStockPriceJob") Job dailyStockPriceJob
    ) {
        this.jobLauncher = jobLauncher;
        this.dailyStockPriceJob = dailyStockPriceJob;
    }

    @Scheduled(cron = "0 10 16 * * MON-FRI", zone = "Asia/Seoul")
    public void runDailyStockPriceJob() throws Exception {
        LocalDate tradeDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tradeDate", tradeDate.toString())
                .toJobParameters();

        jobLauncher.run(dailyStockPriceJob, jobParameters);
    }

}
