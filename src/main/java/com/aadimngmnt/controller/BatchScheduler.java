package com.aadimngmnt.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BatchScheduler {
// if u want to save data through time scheduling then u can use this
    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job csvToMongoJob;

    // 🔁 This runs the job every minute
    @Scheduled(cron = "0 * * * * *")
    public void runJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(csvToMongoJob, params);
            System.out.println("✅ Batch job triggered at: " + new java.util.Date());
        } catch (Exception e) {
            System.err.println("❌ Error running batch job: " + e.getMessage());
        }
    }
}

