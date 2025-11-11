package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestMapper requestMapper;

    @Override
    public ParticipationRequestDto submitRequest(Long userId, Long eventId) {
        log.info("Submitting participation request - user ID: {}, event ID: {}", userId, eventId);

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateRequestSubmission(requester, event);

        ParticipationRequest.Status initialStatus = determineInitialStatus(event);

        ParticipationRequest request = new ParticipationRequest();
        request.setCreated(LocalDateTime.now());
        request.setEvent(event);
        request.setRequester(requester);
        request.setStatus(initialStatus);

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Participation request submitted successfully with ID: {}", savedRequest.getId());

        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> retrieveUserRequests(Long userId) {
        log.info("Retrieving participation requests for user ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }

        return requestRepository.findByRequesterId(userId).stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        log.info("Canceling participation request - user ID: {}, request ID: {}", userId, requestId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Participation request not found"));

        validateRequestCancellation(request, userId);

        request.setStatus(ParticipationRequest.Status.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        log.info("Participation request canceled successfully");
        return requestMapper.toParticipationRequestDto(updatedRequest);
    }

    private void validateRequestSubmission(User requester, Event event) {
        if (event.getInitiator().getId().equals(requester.getId())) {
            throw new ConflictException("Event initiator cannot submit participation request for their own event");
        }

        if (event.getState() != Event.EventState.PUBLISHED) {
            throw new ConflictException("Cannot submit request for unpublished event");
        }

        if (requestRepository.existsByEventIdAndRequesterId(event.getId(), requester.getId())) {
            throw new ConflictException("Participation request already exists for this event");
        }

        if (event.getParticipantLimit() != null && event.getParticipantLimit() > 0) {
            Integer confirmedCount = requestRepository.countByEventIdAndStatus(
                    event.getId(), ParticipationRequest.Status.CONFIRMED);
            if (confirmedCount >= event.getParticipantLimit()) {
                throw new ConflictException("Participant limit reached for this event");
            }
        }
    }

    private ParticipationRequest.Status determineInitialStatus(Event event) {
        if (event.getParticipantLimit() == null || event.getParticipantLimit() == 0) {
            return ParticipationRequest.Status.CONFIRMED;
        }

        if (Boolean.FALSE.equals(event.getRequestModeration())) {
            return ParticipationRequest.Status.CONFIRMED;
        }

        return ParticipationRequest.Status.PENDING;
    }

    private void validateRequestCancellation(ParticipationRequest request, Long userId) {
        if (!request.getRequester().getId().equals(userId)) {
            throw new ForbiddenException("User can only cancel their own participation requests");
        }
    }
}