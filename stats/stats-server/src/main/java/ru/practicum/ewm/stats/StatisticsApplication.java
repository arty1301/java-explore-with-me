package ru.practicum.ewm.stats;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class StatisticsApplication {

    public static void main(String[] args) {
        log.info("Starting Statistics Application...");
        try {
            SpringApplication.run(StatisticsApplication.class, args);
            log.info("Statistics Application started successfully");
        } catch (Exception e) {
            log.error("Failed to start Statistics Application", e);
            throw e;
        }
    }
}