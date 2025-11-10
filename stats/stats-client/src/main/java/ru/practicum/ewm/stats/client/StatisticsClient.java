package ru.practicum.ewm.stats.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsClient {

    private final RestTemplate restTemplate;

    @Value("${stats.service.url:http://localhost:9090}")
    private String statisticsServiceUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void sendAccessRecord(EndpointHit hit) {
        try {
            String url = statisticsServiceUrl + "/hit";
            log.debug("Sending access record to statistics service: {}", hit);
            restTemplate.postForEntity(url, hit, Void.class);
            log.info("Access record sent successfully for app: {}", hit.getApp());
        } catch (Exception e) {
            log.warn("Failed to send access record: {}", e.getMessage());
        }
    }

    public List<ViewStats> fetchAccessStatistics(LocalDateTime start, LocalDateTime end,
                                                 List<String> uris, boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(statisticsServiceUrl + "/stats")
                    .queryParam("start", start.format(DATE_FORMATTER))
                    .queryParam("end", end.format(DATE_FORMATTER))
                    .queryParam("unique", unique);

            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", String.join(",", uris));
            }

            String url = builder.build().toUriString();
            log.debug("Fetching statistics from: {}", url);

            ResponseEntity<List<ViewStats>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<ViewStats>>() {});

            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch statistics: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean checkServiceHealth() {
        try {
            String url = statisticsServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Statistics service health check failed: {}", e.getMessage());
            return false;
        }
    }
}