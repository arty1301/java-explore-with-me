package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.ParticipationRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByInitiatorId(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    @Query("SELECT e FROM Event e WHERE " +
            "(:userIds IS NULL OR e.initiator.id IN :userIds) AND " +
            "(:states IS NULL OR e.state IN :states) AND " +
            "(:categoryIds IS NULL OR e.category.id IN :categoryIds) AND " +
            "(:rangeStart IS NULL OR e.eventDate >= :rangeStart) AND " +
            "(:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findEventsWithFilters(@Param("userIds") List<Long> userIds,
                                      @Param("states") List<Event.EventState> states,
                                      @Param("categoryIds") List<Long> categoryIds,
                                      @Param("rangeStart") LocalDateTime rangeStart,
                                      @Param("rangeEnd") LocalDateTime rangeEnd,
                                      Pageable pageable);

    @Query("SELECT COUNT(pr) FROM ParticipationRequest pr WHERE pr.event.id = :eventId AND pr.status = :status")
    Integer countConfirmedRequests(@Param("eventId") Long eventId,
                                   @Param("status") ParticipationRequest.Status status);

    @Query("SELECT e FROM Event e WHERE " +
            "e.state = 'PUBLISHED' AND " +
            "(:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) AND " +
            "(:categoryIds IS NULL OR e.category.id IN :categoryIds) AND " +
            "(:paid IS NULL OR e.paid = :paid) AND " +
            "e.eventDate > :currentTime")
    Page<Event> findPublishedEvents(@Param("text") String text,
                                    @Param("categoryIds") List<Long> categoryIds,
                                    @Param("paid") Boolean paid,
                                    @Param("currentTime") LocalDateTime currentTime,
                                    Pageable pageable);

    Optional<Event> findByIdAndState(Long id, Event.EventState state);

    List<Event> findByCategoryId(Long categoryId);

    @Query("SELECT e FROM Event e WHERE e.id IN :eventIds")
    List<Event> findByIdIn(@Param("eventIds") List<Long> eventIds);
}