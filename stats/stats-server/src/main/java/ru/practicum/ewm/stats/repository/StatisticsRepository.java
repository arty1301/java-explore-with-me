package ru.practicum.ewm.stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.stats.model.EndpointAccess;
import ru.practicum.ewm.stats.client.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatisticsRepository extends JpaRepository<EndpointAccess, Long> {

    @Query("SELECT new ru.practicum.ewm.stats.dto.ViewStats(" +
            "ea.application, ea.uri, COUNT(ea.ipAddress)) " +
            "FROM EndpointAccess ea " +
            "WHERE ea.accessedAt BETWEEN :startTime AND :endTime " +
            "AND (COALESCE(:uris) IS NULL OR ea.uri IN :uris) " +
            "GROUP BY ea.application, ea.uri " +
            "ORDER BY COUNT(ea.ipAddress) DESC")
    List<ViewStats> calculateAccessStatistics(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("uris") List<String> uris);

    @Query("SELECT new ru.practicum.ewm.stats.dto.ViewStats(" +
            "ea.application, ea.uri, COUNT(DISTINCT ea.ipAddress)) " +
            "FROM EndpointAccess ea " +
            "WHERE ea.accessedAt BETWEEN :startTime AND :endTime " +
            "AND (COALESCE(:uris) IS NULL OR ea.uri IN :uris) " +
            "GROUP BY ea.application, ea.uri " +
            "ORDER BY COUNT(DISTINCT ea.ipAddress) DESC")
    List<ViewStats> calculateUniqueAccessStatistics(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("uris") List<String> uris);
}