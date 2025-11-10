package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e " +
            "WHERE (:userIds IS NULL OR e.initiator.id IN :userIds) " +
            "AND (:states IS NULL OR e.status IN :states) " +
            "AND (:categoryIds IS NULL OR e.category.id IN :categoryIds) " +
            "AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart) " +
            "AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findEventsWithAdminFilters(
            @Param("userIds") List<Long> userIds,
            @Param("states") List<Event.EventStatus> states,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE e.status = 'PUBLISHED' " +
            "AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:categoryIds IS NULL OR e.category.id IN :categoryIds) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart) " +
            "AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findPublishedEventsWithFilters(
            @Param("text") String text,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.id = :eventId AND e.status = 'PUBLISHED'")
    Optional<Event> findPublishedEventById(@Param("eventId") Long eventId);

    @Query("SELECT e FROM Event e WHERE e.category.id = :categoryId")
    List<Event> findEventsByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT e FROM Event e WHERE e.initiator.id = :userId")
    Page<Event> findEventsByInitiator(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM EventParticipation p " +
            "WHERE p.event.id = :eventId AND p.status = 'CONFIRMED'")
    Integer countConfirmedParticipations(@Param("eventId") Long eventId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
            "FROM Event e WHERE e.id = :eventId AND e.initiator.id = :userId")
    boolean existsEventByInitiator(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Query("SELECT e FROM Event e WHERE e.id IN :eventIds")
    List<Event> findEventsByIdList(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' " +
            "AND e.eventDate > :currentTime " +
            "ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingPublishedEvents(@Param("currentTime") LocalDateTime currentTime, Pageable pageable);
}