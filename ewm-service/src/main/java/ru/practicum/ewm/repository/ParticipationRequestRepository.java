package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByEventIdAndStatus(Long eventId, ParticipationRequest.Status status);

    @Query("SELECT pr FROM ParticipationRequest pr WHERE pr.event.id = :eventId AND pr.status = 'CONFIRMED'")
    List<ParticipationRequest> findConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long requesterId);

    @Query("SELECT COUNT(pr) FROM ParticipationRequest pr WHERE pr.event.id = :eventId AND pr.status = :status")
    Long countByEventIdAndStatus(@Param("eventId") Long eventId,
                                 @Param("status") ParticipationRequest.Status status);

    @Query("SELECT pr FROM ParticipationRequest pr WHERE pr.id IN :requestIds AND pr.event.id = :eventId")
    List<ParticipationRequest> findByIdInAndEventId(@Param("requestIds") List<Long> requestIds,
                                                    @Param("eventId") Long eventId);
}