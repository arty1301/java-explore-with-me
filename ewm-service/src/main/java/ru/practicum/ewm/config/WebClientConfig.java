package ru.practicum.ewm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebClientConfig implements WebMvcConfigurer {

    @Value("${stats.service.url:http://localhost:9090}")
    private String statsServiceUrl;

    @Value("${stats.service.timeout.connect:3000}")
    private int connectTimeout;

    @Value("${stats.service.timeout.read:5000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public String statsServiceBaseUrl() {
        return statsServiceUrl;
    }
}