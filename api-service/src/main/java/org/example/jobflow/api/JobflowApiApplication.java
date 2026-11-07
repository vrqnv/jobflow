package org.example.jobflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobflowApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobflowApiApplication.class, args);
    }
}
