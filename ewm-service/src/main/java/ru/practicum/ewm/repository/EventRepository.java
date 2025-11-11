package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import ru.practicum.ewm.model.ParticipationRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE " +
            "(:#{#users == null} = true OR e.initiator.id IN :users) AND " +
            "(:#{#states == null} = true OR e.state IN :states) AND " +
            "(:#{#categories == null} = true OR e.category.id IN :categories) AND " +
            "(:#{#rangeStart == null} = true OR e.eventDate >= :rangeStart) AND " +
            "(:#{#rangeEnd == null} = true OR e.eventDate <= :rangeEnd)")
    List<Event> findEventsWithFilters(@Param("users") List<Long> users,
                                      @Param("states") List<Event.EventState> states,
                                      @Param("categories") List<Long> categories,
                                      @Param("rangeStart") LocalDateTime rangeStart,
                                      @Param("rangeEnd") LocalDateTime rangeEnd,
                                      Pageable pageable);

    default List<Event> findEventsWithFilters(List<Long> users, List<Event.EventState> states, List<Long> categories,
                                              LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return findEventsWithFilters(users, states, categories, rangeStart, rangeEnd, pageable);
    }

    @Query("SELECT COUNT(pr) FROM ParticipationRequest pr WHERE pr.event.id = :eventId AND pr.status = :status")
    Integer countConfirmedRequestsByEventId(@Param("eventId") Long eventId,
                                            @Param("status") ParticipationRequest.Status status);

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN e.category c " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (COALESCE(:text, '') = '' OR " +
            "     (LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "      LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%')))) " +
            "AND (:categories IS NULL OR c.id IN :categories) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND e.eventDate > :currentTime")
    List<Event> findPublicEventsWithFilters(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("currentTime") LocalDateTime currentTime,
            Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.state = 'PUBLISHED'")
    Optional<Event> findPublishedEventById(@Param("id") Long id);

    List<Event> findByCategoryId(Long categoryId);
}