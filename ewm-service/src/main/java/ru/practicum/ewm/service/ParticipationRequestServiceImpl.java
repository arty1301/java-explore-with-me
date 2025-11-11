package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
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
        log.info("Submitting participation request from user ID: {} for event ID: {}", userId, eventId);

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateRequestSubmission(requester, event);

        ParticipationRequest.Status initialStatus = determineInitialStatus(event);

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(initialStatus)
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Participation request submitted successfully with ID: {}", savedRequest.getId());

        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
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
        log.info("Canceling participation request ID: {} by user ID: {}", requestId, userId);

        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Participation request not found"));

        if (request.getStatus() == ParticipationRequest.Status.CONFIRMED) {
            throw new ConflictException("Cannot cancel confirmed participation request");
        }

        request.setStatus(ParticipationRequest.Status.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        log.info("Participation request ID: {} canceled successfully", requestId);
        return requestMapper.toParticipationRequestDto(updatedRequest);
    }

    private void validateRequestSubmission(User requester, Event event) {
        // Проверка, что пользователь не является инициатором события
        if (event.getInitiator().getId().equals(requester.getId())) {
            throw new ConflictException("Event initiator cannot submit participation request");
        }

        // Проверка, что событие опубликовано
        if (event.getState() != Event.EventState.PUBLISHED) {
            throw new ConflictException("Cannot submit request for unpublished event");
        }

        // Проверка на дублирование запроса
        if (requestRepository.existsByEventIdAndRequesterId(event.getId(), requester.getId())) {
            throw new ConflictException("Participation request already exists");
        }

        // Проверка лимита участников
        if (event.getParticipantLimit() > 0) {
            Long confirmedCount = requestRepository.countByEventIdAndStatus(
                    event.getId(), ParticipationRequest.Status.CONFIRMED);
            if (confirmedCount >= event.getParticipantLimit()) {
                throw new ConflictException("Event participant limit reached");
            }
        }
    }

    private ParticipationRequest.Status determineInitialStatus(Event event) {
        if (event.getParticipantLimit() == 0) {
            return ParticipationRequest.Status.CONFIRMED;
        }

        if (event.getRequestModeration() != null && !event.getRequestModeration()) {
            return ParticipationRequest.Status.CONFIRMED;
        }

        return ParticipationRequest.Status.PENDING;
    }
}