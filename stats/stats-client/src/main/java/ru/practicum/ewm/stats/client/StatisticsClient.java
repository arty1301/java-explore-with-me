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
        String url = statisticsServiceUrl + "/hit";

        log.debug("Sending access record to statistics service: {}", hit);

        restTemplate.postForEntity(url, hit, Void.class);

        log.info("Access record sent successfully for app: {}", hit.getApp());
    }

    public List<ViewStats> fetchAccessStatistics(LocalDateTime start, LocalDateTime end,
                                                 List<String> uris, boolean unique) {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(statisticsServiceUrl + "/stats")
                .queryParam("start", start.format(DATE_FORMATTER))
                .queryParam("end", end.format(DATE_FORMATTER))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }

        String url = builder.build().toUriString();

        log.debug("Fetching statistics from: {}", url);

        ResponseEntity<List<ViewStats>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ViewStats>>() {});

        log.info("Retrieved {} statistics records", response.getBody() != null ? response.getBody().size() : 0);
        return response.getBody();
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