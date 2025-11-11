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

    @Query("SELECT pr FROM ParticipationRequest pr WHERE pr.event.id = :eventId AND pr.requester.id = :requesterId")
    Optional<ParticipationRequest> findByEventIdAndRequesterId(@Param("eventId") Long eventId,
                                                               @Param("requesterId") Long requesterId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    @Query("SELECT COUNT(pr) FROM ParticipationRequest pr " +
            "WHERE pr.event.id = :eventId AND pr.status = :status")
    Integer countByEventIdAndStatus(@Param("eventId") Long eventId,
                                    @Param("status") ParticipationRequest.Status status);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "WHERE pr.event.id = :eventId AND pr.status = :status")
    List<ParticipationRequest> findByEventIdAndStatus(@Param("eventId") Long eventId,
                                                      @Param("status") ParticipationRequest.Status status);

    @Query("SELECT pr FROM ParticipationRequest pr WHERE pr.id IN :requestIds")
    List<ParticipationRequest> findByIdIn(@Param("requestIds") List<Long> requestIds);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "WHERE pr.event.id = :eventId AND pr.status = 'PENDING'")
    List<ParticipationRequest> findPendingRequestsByEventId(@Param("eventId") Long eventId);
}