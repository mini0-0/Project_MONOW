package com.monow.batch.stock.dailyprice.scheduler;


import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DailyStockPriceScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyStockPriceJob;

    @Scheduled(cron = "0 10 16 * * MON-FRI", zone = "Asia/Seoul")
    public void runDailyStockPriceJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(dailyStockPriceJob, jobParameters);
    }

}
