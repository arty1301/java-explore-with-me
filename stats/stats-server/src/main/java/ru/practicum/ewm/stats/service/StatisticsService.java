package ru.practicum.ewm.stats.service;

import ru.practicum.ewm.stats.client.EndpointHit;
import ru.practicum.ewm.stats.client.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsService {
    void registerEndpointAccess(EndpointHit hit);
    List<ViewStats> retrieveAccessStatistics(LocalDateTime start, LocalDateTime end,
                                             List<String> uris, boolean unique);
}