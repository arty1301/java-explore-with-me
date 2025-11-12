package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Comment;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByUserIdOrderByCreatedDesc(Long userId, Pageable pageable);

    List<Comment> findByEventIdOrderByCreatedDesc(Long eventId, Pageable pageable);

    Long countByEventId(Long eventId);

    boolean existsByIdAndUserId(Long commentId, Long userId);

    @Query("SELECT c FROM Comment c " +
            "WHERE (:#{#users == null} = true OR c.user.id IN :users) AND " +
            "(:#{#events == null} = true OR c.event.id IN :events) AND " +
            "(:#{#rangeStart == null} = true OR c.created >= :rangeStart) AND " +
            "(:#{#rangeEnd == null} = true OR c.created <= :rangeEnd) " +
            "ORDER BY c.created DESC")
    List<Comment> findCommentsWithFilters(@Param("users") List<Long> users,
                                          @Param("events") List<Long> events,
                                          @Param("rangeStart") LocalDateTime rangeStart,
                                          @Param("rangeEnd") LocalDateTime rangeEnd,
                                          Pageable pageable);

    @Query("SELECT c FROM Comment c " +
            "JOIN c.event e " +
            "WHERE e.state = 'PUBLISHED' AND " +
            "(:#{#events == null} = true OR c.event.id IN :events) AND " +
            "(:#{#users == null} = true OR c.user.id IN :users) AND " +
            "(:#{#rangeStart == null} = true OR c.created >= :rangeStart) AND " +
            "(:#{#rangeEnd == null} = true OR c.created <= :rangeEnd) " +
            "ORDER BY c.created DESC")
    List<Comment> findPublishedCommentsWithFilters(@Param("events") List<Long> events,
                                                   @Param("users") List<Long> users,
                                                   @Param("rangeStart") LocalDateTime rangeStart,
                                                   @Param("rangeEnd") LocalDateTime rangeEnd,
                                                   Pageable pageable);
}