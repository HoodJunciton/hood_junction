package com.thehoodjunction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.thehoodjunction.repository.jpa")
@EnableMongoRepositories(basePackages = "com.thehoodjunction.repository.mongodb")
@EnableAsync
@EnableScheduling
public class TheHoodJunctionApplication {

    public static void main(String[] args) {
        SpringApplication.run(TheHoodJunctionApplication.class, args);
    }
}
