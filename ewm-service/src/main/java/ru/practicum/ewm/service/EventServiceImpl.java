package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final ParticipationRequestMapper requestMapper;
    private final StatisticsClient statisticsClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto eventDto) {
        log.info("Creating event for user ID: {}", userId);

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Category category = categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        validateEventDate(eventDto.getEventDate(), 2);

        Event event = eventMapper.toEventEntity(eventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(Event.EventState.PENDING);
        event.setPaid(eventDto.getPaid() != null ? eventDto.getPaid() : false);
        event.setRequestModeration(eventDto.getRequestModeration() != null ? eventDto.getRequestModeration() : true);
        event.setParticipantLimit(eventDto.getParticipantLimit() != null ? eventDto.getParticipantLimit() : 0);

        Event savedEvent = eventRepository.save(event);
        log.info("Event created successfully with ID: {}", savedEvent.getId());

        return eventMapper.toEventFullDto(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEventsWithFilters(String text, List<Long> categories, Boolean paid,
                                                          String rangeStart, String rangeEnd, boolean onlyAvailable,
                                                          String sort, int from, int size, HttpServletRequest request) {
        log.info("Retrieving public events with filters");

        recordHitStatistics(request);

        LocalDateTime startTime = parseDateTime(rangeStart);
        LocalDateTime endTime = parseDateTime(rangeEnd);
        validateDateRange(startTime, endTime);

        Pageable pageable = createPageable(from, size, sort);
        LocalDateTime now = LocalDateTime.now();

        List<Event> events = eventRepository.findPublishedEvents(text, categories, paid, now, pageable).getContent();

        events = filterByDateRange(events, startTime, endTime);

        if (onlyAvailable) {
            events = events.stream()
                    .filter(this::isEventAvailable)
                    .collect(Collectors.toList());
        }

        Map<Long, Long> views = getEventsViews(events);
        Map<Long, Integer> confirmedRequests = getConfirmedRequestsCount(events);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .collect(Collectors.toList());

        if ("VIEWS".equals(sort)) {
            result.sort((e1, e2) -> Long.compare(e2.getViews(), e1.getViews()));
        } else if ("EVENT_DATE".equals(sort)) {
            result.sort((e1, e2) -> e2.getEventDate().compareTo(e1.getEventDate()));
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEventDetails(Long eventId, HttpServletRequest request) {
        log.info("Retrieving public event details for ID: {}", eventId);

        Event event = eventRepository.findByIdAndState(eventId, Event.EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event not found or not published"));

        recordHitStatistics(request);

        EventFullDto eventDto = eventMapper.toEventFullDto(event);
        eventDto.setConfirmedRequests(getConfirmedRequestCount(eventId));
        eventDto.setViews(getEventViews(eventId, request.getRequestURI()));

        return eventDto;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        log.info("Retrieving event ID: {} for user ID: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        EventFullDto eventDto = eventMapper.toEventFullDto(event);
        eventDto.setConfirmedRequests(getConfirmedRequestCount(eventId));

        return eventDto;
    }

    @Override
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Updating event ID: {} by user ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateUserOwnership(event, userId);
        validateEventStateForUpdate(event);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 2);
        }

        updateEventFields(event, updateRequest);
        handleUserStateAction(event, updateRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        EventFullDto result = eventMapper.toEventFullDto(updatedEvent);
        result.setConfirmedRequests(getConfirmedRequestCount(eventId));

        log.info("Event ID: {} updated successfully by user ID: {}", eventId, userId);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsByUser(Long userId, int from, int size) {
        log.info("Retrieving events for user ID: {}", userId);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();

        Map<Long, Integer> confirmedRequests = getConfirmedRequestsCount(events);
        Map<Long, Long> views = getEventsViews(events);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0));
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    if (dto.getDescription() == null) {
                        dto.setDescription(event.getDescription());
                    }
                    if (dto.getParticipantLimit() == null) {
                        dto.setParticipantLimit(event.getParticipantLimit());
                    }
                    if (dto.getRequestModeration() == null) {
                        dto.setRequestModeration(event.getRequestModeration());
                    }
                    if (dto.getLocation() == null && event.getLocation() != null) {
                        dto.setLocation(new LocationDto(event.getLocation().getLat(), event.getLocation().getLon()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getParticipationRequestsForEvent(Long userId, Long eventId) {
        log.info("Retrieving participation requests for event ID: {} by user ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateUserOwnership(event, userId);

        return requestRepository.findByEventId(eventId).stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult updateParticipationRequestStatus(Long userId, Long eventId,
                                                                           EventRequestStatusUpdateRequest statusUpdate) {
        log.info("Updating participation request status for event ID: {} by user ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateUserOwnership(event, userId);

        List<ParticipationRequest> requests = requestRepository.findByIdInAndEventId(
                statusUpdate.getRequestIds(), eventId);

        validateRequestsForStatusUpdate(requests);

        EventRequestStatusUpdateResult result = processRequestStatusUpdate(
                event, requests, statusUpdate.getStatus());

        log.info("Participation request status updated successfully for event ID: {}", eventId);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getEventsForAdministration(List<Long> userIds, List<String> states, List<Long> categories,
                                                         String rangeStart, String rangeEnd, int from, int size) {
        log.info("Retrieving events for administration");

        List<Event.EventState> eventStates = parseEventStates(states);
        LocalDateTime startTime = parseDateTime(rangeStart);
        LocalDateTime endTime = parseDateTime(rangeEnd);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findEventsWithFilters(
                userIds, eventStates, categories, startTime, endTime, pageable).getContent();

        Map<Long, Long> views = getEventsViews(events);
        Map<Long, Integer> confirmedRequests = getConfirmedRequestsCount(events);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toEventFullDto(event);
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0));
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto updateEventByAdministrator(Long eventId, UpdateEventAdminRequest adminRequest) {
        log.info("Updating event ID: {} by administrator", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateAdminUpdate(event, adminRequest);

        if (adminRequest.getEventDate() != null) {
            validateEventDate(adminRequest.getEventDate(), 1);
        }

        updateEventFields(event, adminRequest);
        handleAdminStateAction(event, adminRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        EventFullDto result = eventMapper.toEventFullDto(updatedEvent);
        result.setConfirmedRequests(getConfirmedRequestCount(eventId));
        result.setViews(getEventViews(eventId, "/events/" + eventId));

        log.info("Event ID: {} updated successfully by administrator", eventId);
        return result;
    }

    private void validateEventDate(String eventDateStr, int hoursBefore) {
        if (eventDateStr != null) {
            LocalDateTime eventDate = parseDateTime(eventDateStr);
            if (eventDate.isBefore(LocalDateTime.now().plusHours(hoursBefore))) {
                throw new ConditionsNotMetException(
                        String.format("Event date must be at least %d hours from now", hoursBefore));
            }
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ConditionsNotMetException("Start date must be before end date");
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return null;
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ConditionsNotMetException("Invalid date format. Use: yyyy-MM-dd HH:mm:ss");
        }
    }

    private void recordHitStatistics(HttpServletRequest request) {
        try {
            EndpointHit hit = EndpointHit.builder()
                    .app("ewm-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();
            statisticsClient.sendAccessRecord(hit);
        } catch (Exception e) {
            log.warn("Failed to record hit statistics: {}", e.getMessage());
        }
    }

    private boolean isEventAvailable(Event event) {
        if (event.getParticipantLimit() == 0) return true;
        Integer confirmed = getConfirmedRequestCount(event.getId());
        return confirmed < event.getParticipantLimit();
    }

    private Integer getConfirmedRequestCount(Long eventId) {
        return eventRepository.countConfirmedRequests(eventId, ParticipationRequest.Status.CONFIRMED);
    }

    private Map<Long, Integer> getConfirmedRequestsCount(List<Event> events) {
        return events.stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        event -> getConfirmedRequestCount(event.getId())
                ));
    }

    private Long getEventViews(Long eventId, String uri) {
        try {
            List<ViewStats> stats = statisticsClient.fetchAccessStatistics(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now(),
                    List.of(uri),
                    true
            );
            return stats.stream()
                    .findFirst()
                    .map(ViewStats::getHits)
                    .orElse(0L);
        } catch (Exception e) {
            log.warn("Failed to get event views: {}", e.getMessage());
            return 0L;
        }
    }

    private Map<Long, Long> getEventsViews(List<Event> events) {
        if (events.isEmpty()) return new HashMap<>();

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
                            stat -> Long.parseLong(stat.getUri().substring("/events/".length())),
                            ViewStats::getHits
                    ));
        } catch (Exception e) {
            log.warn("Failed to get events views: {}", e.getMessage());
            return events.stream()
                    .collect(Collectors.toMap(Event::getId, event -> 0L));
        }
    }

    private void validateUserOwnership(Event event, Long userId) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("User is not the owner of this event");
        }
    }

    private void validateEventStateForUpdate(Event event) {
        if (event.getState() != Event.EventState.PENDING &&
                event.getState() != Event.EventState.CANCELED) {
            throw new ConflictException("Only pending or canceled events can be updated");
        }
    }

    private Pageable createPageable(int from, int size, String sort) {
        Sort.Direction direction = Sort.Direction.ASC;
        String property = "eventDate";

        if ("EVENT_DATE".equals(sort)) {
            direction = Sort.Direction.DESC;
        }

        return PageRequest.of(from / size, size, Sort.by(direction, property));
    }

    private List<Event> filterByDateRange(List<Event> events, LocalDateTime start, LocalDateTime end) {
        return events.stream()
                .filter(event -> {
                    boolean afterStart = start == null || !event.getEventDate().isBefore(start);
                    boolean beforeEnd = end == null || !event.getEventDate().isAfter(end);
                    return afterStart && beforeEnd;
                })
                .collect(Collectors.toList());
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
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
            event.setEventDate(parseDateTime(updateRequest.getEventDate()));
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
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest adminRequest) {
        if (adminRequest.getAnnotation() != null) {
            event.setAnnotation(adminRequest.getAnnotation());
        }
        if (adminRequest.getCategory() != null) {
            Category category = categoryRepository.findById(adminRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (adminRequest.getDescription() != null) {
            event.setDescription(adminRequest.getDescription());
        }
        if (adminRequest.getEventDate() != null) {
            event.setEventDate(parseDateTime(adminRequest.getEventDate()));
        }
        if (adminRequest.getPaid() != null) {
            event.setPaid(adminRequest.getPaid());
        }
        if (adminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(adminRequest.getParticipantLimit());
        }
        if (adminRequest.getRequestModeration() != null) {
            event.setRequestModeration(adminRequest.getRequestModeration());
        }
        if (adminRequest.getTitle() != null) {
            event.setTitle(adminRequest.getTitle());
        }
        if (adminRequest.getLat() != null || adminRequest.getLon() != null) {
            Location location = event.getLocation() != null ? event.getLocation() : new Location();
            if (adminRequest.getLat() != null) location.setLat(adminRequest.getLat());
            if (adminRequest.getLon() != null) location.setLon(adminRequest.getLon());
            event.setLocation(location);
        }
    }

    private void handleUserStateAction(Event event, UpdateEventUserRequest.StateAction stateAction) {
        if (stateAction == null) return;

        switch (stateAction) {
            case CANCEL_REVIEW:
                event.setState(Event.EventState.CANCELED);
                break;
            case SEND_TO_REVIEW:
                event.setState(Event.EventState.PENDING);
                break;
            default:
                throw new ConditionsNotMetException("Unknown state action: " + stateAction);
        }
    }

    private void handleAdminStateAction(Event event, UpdateEventAdminRequest.StateAction stateAction) {
        if (stateAction == null) return;

        switch (stateAction) {
            case PUBLISH_EVENT:
                event.setState(Event.EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;
            case REJECT_EVENT:
                event.setState(Event.EventState.CANCELED);
                break;
            default:
                throw new ConditionsNotMetException("Unknown state action: " + stateAction);
        }
    }

    private void validateAdminUpdate(Event event, UpdateEventAdminRequest adminRequest) {
        if (adminRequest.getStateAction() == UpdateEventAdminRequest.StateAction.PUBLISH_EVENT) {
            if (event.getState() != Event.EventState.PENDING) {
                throw new ConflictException("Cannot publish event that is not in pending state");
            }
        } else if (adminRequest.getStateAction() == UpdateEventAdminRequest.StateAction.REJECT_EVENT) {
            if (event.getState() == Event.EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject already published event");
            }
        }
    }

    private List<Event.EventState> parseEventStates(List<String> states) {
        if (states == null || states.isEmpty()) return null;

        return states.stream()
                .map(stateStr -> {
                    try {
                        return Event.EventState.valueOf(stateStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConditionsNotMetException("Invalid event state: " + stateStr);
                    }
                })
                .collect(Collectors.toList());
    }

    private void validateRequestsForStatusUpdate(List<ParticipationRequest> requests) {
        for (ParticipationRequest request : requests) {
            if (request.getStatus() != ParticipationRequest.Status.PENDING) {
                throw new ConflictException("Cannot change status of non-pending request");
            }
        }
    }

    private EventRequestStatusUpdateResult processRequestStatusUpdate(Event event,
                                                                      List<ParticipationRequest> requests,
                                                                      EventRequestStatusUpdateRequest.Status newStatus) {
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (newStatus == EventRequestStatusUpdateRequest.Status.CONFIRMED) {
            processConfirmation(event, requests, confirmed, rejected);
        } else {
            processRejection(requests, rejected);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    private void processConfirmation(Event event, List<ParticipationRequest> requests,
                                     List<ParticipationRequestDto> confirmed,
                                     List<ParticipationRequestDto> rejected) {
        int participantLimit = event.getParticipantLimit() != null ? event.getParticipantLimit() : 0;
        int currentConfirmed = getConfirmedRequestCount(event.getId());
        int availableSlots = participantLimit - currentConfirmed;

        if (participantLimit > 0 && availableSlots <= 0) {
            throw new ConflictException("Event participant limit reached");
        }

        for (ParticipationRequest request : requests) {
            if (availableSlots > 0) {
                request.setStatus(ParticipationRequest.Status.CONFIRMED);
                confirmed.add(requestMapper.toParticipationRequestDto(request));
                availableSlots--;
            } else {
                request.setStatus(ParticipationRequest.Status.REJECTED);
                rejected.add(requestMapper.toParticipationRequestDto(request));
            }
        }

        requestRepository.saveAll(requests);

        // Автоматически отклоняем оставшиеся pending запросы если лимит достигнут
        if (participantLimit > 0 && availableSlots == 0) {
            autoRejectPendingRequests(event.getId());
        }
    }

    private void processRejection(List<ParticipationRequest> requests,
                                  List<ParticipationRequestDto> rejected) {
        for (ParticipationRequest request : requests) {
            request.setStatus(ParticipationRequest.Status.REJECTED);
            rejected.add(requestMapper.toParticipationRequestDto(request));
        }
        requestRepository.saveAll(requests);
    }

    private void autoRejectPendingRequests(Long eventId) {
        List<ParticipationRequest> pendingRequests = requestRepository.findByEventIdAndStatus(
                eventId, ParticipationRequest.Status.PENDING);

        for (ParticipationRequest request : pendingRequests) {
            request.setStatus(ParticipationRequest.Status.REJECTED);
        }

        if (!pendingRequests.isEmpty()) {
            requestRepository.saveAll(pendingRequests);
        }
    }
}