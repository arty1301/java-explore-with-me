package ru.practicum.ewm.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.client.EndpointHit;
import ru.practicum.ewm.stats.client.ViewStats;
import ru.practicum.ewm.stats.mapper.StatisticsMapper;
import ru.practicum.ewm.stats.model.EndpointAccess;
import ru.practicum.ewm.stats.repository.StatisticsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsRepository statisticsRepository;
    private final StatisticsMapper statisticsMapper;

    @Override
    @Transactional
    public void registerEndpointAccess(EndpointHit hit) {
        log.debug("Registering endpoint access: app={}, uri={}, ip={}",
                hit.getApp(), hit.getUri(), hit.getIp());

        EndpointAccess access = statisticsMapper.toEndpointAccess(hit);
        statisticsRepository.save(access);

        log.info("Endpoint access registered for app: {}", hit.getApp());
    }

    @Override
    public List<ViewStats> retrieveAccessStatistics(LocalDateTime start, LocalDateTime end,
                                                    List<String> uris, boolean unique) {
        log.debug("Retrieving access statistics: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        validateTimeRange(start, end);

        List<ViewStats> statistics;
        if (unique) {
            statistics = statisticsRepository.calculateUniqueAccessStatistics(start, end, uris);
        } else {
            statistics = statisticsRepository.calculateAccessStatistics(start, end, uris);
        }

        log.info("Retrieved {} statistics records", statistics.size());
        return statistics;
    }


    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }
}