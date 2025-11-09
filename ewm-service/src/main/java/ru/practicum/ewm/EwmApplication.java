package ru.practicum.ewm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
public class EwmApplication {

    public static void main(String[] args) {
        log.info("Starting ExploreWithMe Main Application...");
        try {
            SpringApplication.run(EwmApplication.class, args);
            log.info("ExploreWithMe Main Application started successfully");
        } catch (Exception e) {
            log.error("Failed to start ExploreWithMe Main Application", e);
            throw e;
        }
    }
}