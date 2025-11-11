package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.ParticipationRequest;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    List<ParticipationRequest> findByEventId(Long eventId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    @Query("SELECT COALESCE(COUNT(pr), 0) FROM ParticipationRequest pr " +
            "WHERE pr.event.id = :eventId AND pr.status = :status")
    Integer countByEventIdAndStatus(@Param("eventId") Long eventId,
                                    @Param("status") ParticipationRequest.Status status);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "WHERE pr.event.id = :eventId AND pr.status = :status")
    List<ParticipationRequest> findByEventIdAndStatus(@Param("eventId") Long eventId,
                                                      @Param("status") ParticipationRequest.Status status);
}