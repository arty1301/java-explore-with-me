package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.ConditionsNotMetException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.stats.client.StatisticsClient;
import ru.practicum.ewm.stats.client.EndpointHit;
import ru.practicum.ewm.stats.client.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final ParticipationRequestRepository participationRequestRepository;
    private final ParticipationRequestMapper participationRequestMapper;
    private final StatisticsClient statisticsClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto eventDto) {
        log.info("Creating event for user ID: {}", userId);

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Category category = categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        validateEventCreationParameters(eventDto);

        Event event = eventMapper.toEventEntity(eventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(Event.EventState.PENDING);

        Event savedEvent = eventRepository.save(event);
        log.info("Event created successfully with ID: {}", savedEvent.getId());

        return eventMapper.toEventFullDto(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> searchPublicEvents(String text, List<Long> categories, Boolean paid,
                                                  String rangeStart, String rangeEnd, boolean onlyAvailable,
                                                  String sort, int from, int size, HttpServletRequest request) {
        log.info("Searching public events with filters - text: {}, categories: {}", text, categories);

        recordEndpointHit(request);

        LocalDateTime startTime = parseDateTime(rangeStart);
        LocalDateTime endTime = parseDateTime(rangeEnd);
        validateDateRange(startTime, endTime);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = findPublicEventsWithFilters(text, categories, paid, startTime, endTime, pageable);

        if (onlyAvailable) {
            events = filterAvailableEvents(events);
        }

        Map<Long, Long> eventViews = getEventsViews(events);
        List<EventShortDto> eventDtos = convertToShortDtos(events, eventViews);

        return sortEvents(eventDtos, sort);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto retrievePublicEvent(Long eventId, HttpServletRequest request) {
        log.info("Retrieving public event with ID: {}", eventId);

        recordEndpointHit(request);

        Event event = eventRepository.findPublishedEventById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        EventFullDto eventDto = eventMapper.toEventFullDto(event);
        enrichEventWithStatistics(eventDto, request);

        return eventDto;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto retrieveUserEventDetails(Long userId, Long eventId) {
        log.info("Retrieving event details for user ID: {}, event ID: {}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateEventOwnership(event, userId);

        return eventMapper.toEventFullDto(event);
    }

    @Override
    public EventFullDto updateEventByInitiator(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Updating event by initiator - user ID: {}, event ID: {}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateEventOwnership(event, userId);
        validateEventUpdateTiming(event);
        validateEventStateForUpdate(event);

        applyEventUpdates(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);

        log.info("Event updated successfully by initiator");
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> retrieveUserEvents(Long userId, int from, int size) {
        log.info("Retrieving events for user ID: {}", userId);

        Pageable pageable = PageRequest.of(from / size, size);
        return eventRepository.findByInitiatorId(userId, pageable).stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipationRequests(Long userId, Long eventId) {
        log.info("Retrieving participation requests for event ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateEventOwnership(event, userId);

        return participationRequestRepository.findByEventId(eventId).stream()
                .map(participationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult updateParticipationRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdate) {
        log.info("Updating participation request status for event ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateEventOwnership(event, userId);

        List<ParticipationRequest> requests = validateAndRetrieveRequests(statusUpdate.getRequestIds(), eventId);
        return processRequestStatusUpdate(event, requests, statusUpdate.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> searchEventsByAdmin(List<Long> userIds, List<String> states, List<Long> categories,
                                                  String rangeStart, String rangeEnd, int from, int size) {
        log.info("Admin event search with filters - users: {}, states: {}", userIds, states);

        LocalDateTime start = parseDateTime(rangeStart);
        LocalDateTime end = parseDateTime(rangeEnd);

        List<Event.EventState> eventStates = parseEventStates(states);
        List<Event> events = eventRepository.findEventsWithFilters(userIds, eventStates, categories, start, end, from, size);

        return events.stream()
                .map(this::enrichEventWithAdminData)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto updateEventByAdministrator(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Admin updating event with ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateAdminEventUpdate(event, updateRequest);
        applyAdminEventUpdates(event, updateRequest);

        Event updatedEvent = eventRepository.save(event);
        log.info("Event updated successfully by administrator");

        return enrichEventWithAdminData(updatedEvent);
    }

    private void validateEventCreationParameters(NewEventDto eventDto) {
        if (eventDto.getEventDate() != null) {
            LocalDateTime eventDate = parseDateTime(eventDto.getEventDate());
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ConditionsNotMetException("Event date must be at least 2 hours from now");
            }
        }

        if (eventDto.getParticipantLimit() == null) {
            eventDto.setParticipantLimit(0);
        }
        if (eventDto.getRequestModeration() == null) {
            eventDto.setRequestModeration(true);
        }
        if (eventDto.getPaid() == null) {
            eventDto.setPaid(false);
        }
    }

    private void recordEndpointHit(HttpServletRequest request) {
        try {
            EndpointHit hit = EndpointHit.builder()
                    .app("ewm-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();
            statisticsClient.sendAccessRecord(hit);
        } catch (Exception e) {
            log.warn("Failed to record endpoint hit: {}", e.getMessage());
        }
    }

    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ConditionsNotMetException("Invalid date format. Expected: yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ConditionsNotMetException("Start date must be before end date");
        }
    }

    private List<Event> findPublicEventsWithFilters(String text, List<Long> categories, Boolean paid,
                                                    LocalDateTime start, LocalDateTime end, Pageable pageable) {
        LocalDateTime effectiveStart = start != null ? start : LocalDateTime.now();

        List<Event> events = eventRepository.findPublishedEventsWithFilters(
                text, categories, paid, effectiveStart, pageable).getContent();

        if (end != null) {
            events = events.stream()
                    .filter(event -> !event.getEventDate().isAfter(end))
                    .collect(Collectors.toList());
        }

        return events;
    }

    private List<Event> filterAvailableEvents(List<Event> events) {
        return events.stream()
                .filter(this::isEventAvailable)
                .collect(Collectors.toList());
    }

    private boolean isEventAvailable(Event event) {
        if (event.getParticipantLimit() == null || event.getParticipantLimit() == 0) {
            return true;
        }
        Integer confirmedRequests = eventRepository.countConfirmedRequestsForEvent(
                event.getId(), ParticipationRequest.Status.CONFIRMED);
        return confirmedRequests < event.getParticipantLimit();
    }

    private Map<Long, Long> getEventsViews(List<Event> events) {
        if (events.isEmpty()) {
            return new HashMap<>();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStats> stats = statisticsClient.fetchAccessStatistics(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now(),
                    uris,
                    true
            );

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> extractEventIdFromUri(stat.getUri()),
                            ViewStats::getHits
                    ));
        } catch (Exception e) {
            log.warn("Failed to retrieve event views: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            return Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
        } catch (Exception e) {
            return 0L;
        }
    }

    private List<EventShortDto> convertToShortDtos(List<Event> events, Map<Long, Long> views) {
        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<EventShortDto> sortEvents(List<EventShortDto> events, String sort) {
        if ("VIEWS".equalsIgnoreCase(sort)) {
            events.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        } else {
            events.sort(Comparator.comparing(EventShortDto::getEventDate));
        }
        return events;
    }

    private void enrichEventWithStatistics(EventFullDto eventDto, HttpServletRequest request) {
        Integer confirmedRequests = eventRepository.countConfirmedRequestsForEvent(
                eventDto.getId(), ParticipationRequest.Status.CONFIRMED);
        eventDto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);

        try {
            List<ViewStats> stats = statisticsClient.fetchAccessStatistics(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now(),
                    List.of(request.getRequestURI()),
                    true
            );

            Long views = stats.stream()
                    .filter(stat -> stat.getUri().equals(request.getRequestURI()))
                    .findFirst()
                    .map(ViewStats::getHits)
                    .orElse(0L);
            eventDto.setViews(views);
        } catch (Exception e) {
            log.warn("Failed to retrieve event views: {}", e.getMessage());
            eventDto.setViews(0L);
        }
    }

    private void validateEventOwnership(Event event, Long userId) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("User is not the owner of this event");
        }
    }

    private void validateEventUpdateTiming(Event event) {
        if (LocalDateTime.now().plusHours(2).isAfter(event.getEventDate())) {
            throw new ConditionsNotMetException("Event cannot be updated less than 2 hours before start");
        }
    }

    private void validateEventStateForUpdate(Event event) {
        if (event.getState() != Event.EventState.PENDING && event.getState() != Event.EventState.CANCELED) {
            throw new ConflictException("Only pending or canceled events can be updated");
        }
    }

    private void applyEventUpdates(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            LocalDateTime newEventDate = parseDateTime(updateRequest.getEventDate());
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ConditionsNotMetException("Event date must be at least 2 hours from now");
            }
            event.setEventDate(newEventDate);
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case CANCEL_REVIEW:
                    event.setState(Event.EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(Event.EventState.PENDING);
                    break;
            }
        }
    }

    private List<ParticipationRequest> validateAndRetrieveRequests(List<Long> requestIds, Long eventId) {
        List<ParticipationRequest> requests = participationRequestRepository.findByIdIn(requestIds);

        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Request does not belong to this event");
            }
            if (request.getStatus() != ParticipationRequest.Status.PENDING) {
                throw new ConflictException("Cannot change status of non-pending request");
            }
        }

        return requests;
    }

    private EventRequestStatusUpdateResult processRequestStatusUpdate(
            Event event, List<ParticipationRequest> requests, EventRequestStatusUpdateRequest.Status newStatus) {

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        Integer currentConfirmed = participationRequestRepository.countByEventIdAndStatus(
                event.getId(), ParticipationRequest.Status.CONFIRMED);
        int confirmedCount = currentConfirmed != null ? currentConfirmed : 0;

        Integer participantLimit = event.getParticipantLimit();
        boolean hasLimit = participantLimit != null && participantLimit > 0;

        if (!hasLimit || Boolean.FALSE.equals(event.getRequestModeration())) {
            for (ParticipationRequest request : requests) {
                request.setStatus(ParticipationRequest.Status.CONFIRMED);
                confirmed.add(participationRequestMapper.toParticipationRequestDto(request));
            }
            participationRequestRepository.saveAll(requests);
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(confirmed)
                    .rejectedRequests(rejected)
                    .build();
        }

        for (ParticipationRequest request : requests) {
            if (newStatus == EventRequestStatusUpdateRequest.Status.CONFIRMED) {
                if (confirmedCount >= participantLimit) {
                    throw new ConflictException("Participant limit reached");
                }
                request.setStatus(ParticipationRequest.Status.CONFIRMED);
                confirmed.add(participationRequestMapper.toParticipationRequestDto(request));
                confirmedCount++;
            } else {
                request.setStatus(ParticipationRequest.Status.REJECTED);
                rejected.add(participationRequestMapper.toParticipationRequestDto(request));
            }
        }

        participationRequestRepository.saveAll(requests);

        if (confirmedCount >= participantLimit) {
            rejectPendingRequests(event.getId());
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    private void rejectPendingRequests(Long eventId) {
        List<ParticipationRequest> pendingRequests = participationRequestRepository
                .findPendingRequestsByEventId(eventId);

        for (ParticipationRequest request : pendingRequests) {
            request.setStatus(ParticipationRequest.Status.REJECTED);
        }

        participationRequestRepository.saveAll(pendingRequests);
    }

    private List<Event.EventState> parseEventStates(List<String> states) {
        if (states == null || states.isEmpty()) {
            return null;
        }
        return states.stream()
                .map(Event.EventState::valueOf)
                .collect(Collectors.toList());
    }

    private EventFullDto enrichEventWithAdminData(Event event) {
        EventFullDto dto = eventMapper.toEventFullDto(event);

        Integer confirmedRequests = eventRepository.countConfirmedRequestsForEvent(
                event.getId(), ParticipationRequest.Status.CONFIRMED);
        dto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);

        try {
            List<ViewStats> stats = statisticsClient.fetchAccessStatistics(
                    event.getCreatedOn(),
                    LocalDateTime.now(),
                    List.of("/events/" + event.getId()),
                    true
            );
            dto.setViews(stats.isEmpty() ? 0L : stats.get(0).getHits());
        } catch (Exception e) {
            log.warn("Failed to retrieve views for event {}: {}", event.getId(), e.getMessage());
            dto.setViews(0L);
        }

        return dto;
    }

    private void validateAdminEventUpdate(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != Event.EventState.PENDING) {
                        throw new ConflictException("Only pending events can be published");
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConditionsNotMetException("Event must start at least 1 hour from now");
                    }
                    break;
                case REJECT_EVENT:
                    if (event.getState() == Event.EventState.PUBLISHED) {
                        throw new ConflictException("Published events cannot be rejected");
                    }
                    break;
            }
        }
    }

    private void applyAdminEventUpdates(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) event.setAnnotation(updateRequest.getAnnotation());
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) event.setDescription(updateRequest.getDescription());
        if (updateRequest.getEventDate() != null) {
            LocalDateTime newEventDate = parseDateTime(updateRequest.getEventDate());
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConditionsNotMetException("Event date must be at least 1 hour from now");
            }
            event.setEventDate(newEventDate);
        }
        if (updateRequest.getPaid() != null) event.setPaid(updateRequest.getPaid());
        if (updateRequest.getParticipantLimit() != null) event.setParticipantLimit(updateRequest.getParticipantLimit());
        if (updateRequest.getRequestModeration() != null) event.setRequestModeration(updateRequest.getRequestModeration());
        if (updateRequest.getTitle() != null) event.setTitle(updateRequest.getTitle());

        if (updateRequest.getLat() != null || updateRequest.getLon() != null) {
            if (event.getLocation() == null) {
                event.setLocation(new Location());
            }
            if (updateRequest.getLat() != null) event.getLocation().setLat(updateRequest.getLat());
            if (updateRequest.getLon() != null) event.getLocation().setLon(updateRequest.getLon());
        }

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case PUBLISH_EVENT:
                    event.setState(Event.EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    event.setState(Event.EventState.CANCELED);
                    break;
            }
        }
    }
}