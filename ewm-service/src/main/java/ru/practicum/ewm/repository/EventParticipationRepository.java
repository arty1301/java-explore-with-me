package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.EventParticipation;

import java.util.List;

public interface EventParticipationRepository extends JpaRepository<EventParticipation, Long> {

    @Query("SELECT p FROM EventParticipation p WHERE p.requester.id = :userId ORDER BY p.creationTime DESC")
    List<EventParticipation> findUserParticipations(@Param("userId") Long userId);

    @Query("SELECT p FROM EventParticipation p WHERE p.event.id = :eventId ORDER BY p.creationTime DESC")
    List<EventParticipation> findEventParticipations(@Param("eventId") Long eventId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM EventParticipation p WHERE p.event.id = :eventId AND p.requester.id = :userId")
    boolean existsParticipationByUserAndEvent(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM EventParticipation p " +
            "WHERE p.event.id = :eventId AND p.status = :status")
    Integer countParticipationsByStatus(@Param("eventId") Long eventId,
                                        @Param("status") EventParticipation.ParticipationStatus status);

    @Query("SELECT p FROM EventParticipation p " +
            "WHERE p.event.id = :eventId AND p.status = :status")
    List<EventParticipation> findParticipationsByStatus(@Param("eventId") Long eventId,
                                                        @Param("status") EventParticipation.ParticipationStatus status);

    @Query("SELECT p FROM EventParticipation p WHERE p.id IN :participationIds")
    List<EventParticipation> findParticipationsByIdList(@Param("participationIds") List<Long> participationIds);
}