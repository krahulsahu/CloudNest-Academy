package com.cloudnest.academy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AcademyApplication {
    public static void main(String[] args) {
        SpringApplication.run(AcademyApplication.class, args);
    }
}
