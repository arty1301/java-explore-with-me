package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.exception.*;
import ru.practicum.ewm.mapper.EventParticipationMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventParticipation;
import ru.practicum.ewm.model.PlatformUser;
import ru.practicum.ewm.repository.EventParticipationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.PlatformUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventParticipationServiceImpl implements EventParticipationService {

    private final EventParticipationRepository participationRepository;
    private final PlatformUserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventParticipationMapper participationMapper;

    @Override
    public ParticipationRequestDto createParticipationRequest(Long userId, Long eventId) {
        log.info("Creating participation request for user ID: {} on event ID: {}", userId, eventId);

        PlatformUser requester = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        validateParticipationRequest(requester, event);

        Optional<EventParticipation> existingParticipation = participationRepository
                .findByEventIdAndRequesterId(eventId, userId);
        if (existingParticipation.isPresent()) {
            throw new DataConflictException("Participation request already exists for this event");
        }

        EventParticipation.ParticipationStatus initialStatus = determineInitialStatus(event);

        EventParticipation participation = EventParticipation.builder()
                .creationTime(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(initialStatus)
                .build();

        EventParticipation savedParticipation = participationRepository.save(participation);
        log.info("Successfully created participation request with ID: {}", savedParticipation.getId());

        return participationMapper.convertToDto(savedParticipation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserParticipationRequests(Long userId) {
        log.info("Retrieving participation requests for user ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        return participationRepository.findUserParticipations(userId)
                .stream()
                .map(participationMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        log.info("Canceling participation request ID: {} for user ID: {}", requestId, userId);

        EventParticipation participation = participationRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Participation request not found with ID: " + requestId));

        validateParticipationCancellation(userId, participation);

        participation.setStatus(EventParticipation.ParticipationStatus.CANCELED);
        EventParticipation updatedParticipation = participationRepository.save(participation);

        log.info("Successfully canceled participation request ID: {}", requestId);
        return participationMapper.convertToDto(updatedParticipation);
    }

    private void validateParticipationRequest(PlatformUser requester, Event event) {
        if (event.getInitiator().getId().equals(requester.getId())) {
            throw new DataConflictException("Event initiator cannot participate in their own event");
        }

        if (!Event.EventStatus.PUBLISHED.equals(event.getStatus())) {
            throw new DataConflictException("Cannot participate in unpublished event");
        }

        if (event.getParticipantLimit() != null && event.getParticipantLimit() > 0) {
            Integer confirmedCount = participationRepository.countParticipationsByStatus(
                    event.getId(), EventParticipation.ParticipationStatus.CONFIRMED);
            if (confirmedCount != null && confirmedCount >= event.getParticipantLimit()) {
                throw new DataConflictException("Event participant limit reached");
            }
        }

        log.debug("Participation request validation passed for user ID: {} and event ID: {}",
                requester.getId(), event.getId());
    }

    private EventParticipation.ParticipationStatus determineInitialStatus(Event event) {
        if (event.getParticipantLimit() == null || event.getParticipantLimit() == 0) {
            return EventParticipation.ParticipationStatus.CONFIRMED;
        }

        if (Boolean.FALSE.equals(event.getRequestModeration())) {
            return EventParticipation.ParticipationStatus.CONFIRMED;
        }

        return EventParticipation.ParticipationStatus.PENDING;
    }

    private void validateParticipationCancellation(Long userId, EventParticipation participation) {
        if (!participation.getRequester().getId().equals(userId)) {
            throw new AccessDeniedException("User cannot cancel another user's participation request");
        }

        if (EventParticipation.ParticipationStatus.CONFIRMED.equals(participation.getStatus())) {
            throw new DataConflictException("Cannot cancel already confirmed participation request");
        }

        log.debug("Participation cancellation validation passed for request ID: {}", participation.getId());
    }
}