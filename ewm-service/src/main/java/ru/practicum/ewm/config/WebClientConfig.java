package ru.practicum.ewm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebClientConfig implements WebMvcConfigurer {

    @Value("${stats.service.url:http://localhost:9090}")
    private String statsServiceUrl;

    @Bean
    public String statsServiceBaseUrl() {
        return statsServiceUrl;
    }
}