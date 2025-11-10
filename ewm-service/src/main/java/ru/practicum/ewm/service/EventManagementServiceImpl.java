package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.*;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.EventParticipationMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.*;
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
public class EventManagementServiceImpl implements EventManagementService {

    private final EventRepository eventRepository;
    private final PlatformUserRepository userRepository;
    private final EventCategoryRepository categoryRepository;
    private final EventParticipationRepository participationRepository;
    private final EventMapper eventMapper;
    private final EventParticipationMapper participationMapper;
    private final StatisticsClient statisticsClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MIN_HOURS_BEFORE_EVENT = 2;
    private static final int ADMIN_MIN_HOURS_BEFORE_EVENT = 1;

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto request) {
        log.info("Creating event for user ID: {}", userId);

        PlatformUser initiator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        EventCategory category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategory()));

        validateEventCreationTime(request.getEventDate());

        Event newEvent = eventMapper.convertToEntity(request);

        newEvent.setInitiator(initiator);
        newEvent.setCategory(category);
        newEvent.setCreationDate(LocalDateTime.now());
        newEvent.setStatus(Event.EventStatus.PENDING);

        if (request.getLocation() != null) {
            EventLocation location = new EventLocation();
            location.setLatitude(request.getLocation().getLat());
            location.setLongitude(request.getLocation().getLon());
            newEvent.setLocation(location);
        }

        if (newEvent.getPaid() == null) {
            newEvent.setPaid(false);
        }
        if (newEvent.getParticipantLimit() == null) {
            newEvent.setParticipantLimit(0);
        }
        if (newEvent.getRequestModeration() == null) {
            newEvent.setRequestModeration(true);
        }

        Event savedEvent = eventRepository.save(newEvent);
        log.info("Successfully created event with ID: {} for user ID: {}", savedEvent.getId(), userId);

        return createEventFullDtoWithStats(savedEvent, retrieveEventsViewCounts(List.of(savedEvent)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> findPublicEvents(String searchText, List<Long> categories, Boolean paid,
                                                String rangeStart, String rangeEnd, boolean onlyAvailable,
                                                String sortBy, int startingFrom, int pageSize, HttpServletRequest httpRequest) {
        log.info("Searching public events with filters - text: {}, categories: {}, paid: {}",
                searchText, categories, paid);

        recordEndpointAccess(httpRequest);

        LocalDateTime startTime = parseDateTimeString(rangeStart);
        LocalDateTime endTime = parseDateTimeString(rangeEnd);
        validateDateTimeRange(startTime, endTime);

        if (startTime == null) {
            startTime = LocalDateTime.now();
        }

        int actualPageSize = Math.min(pageSize, 1000);
        Pageable pageable = createPageable(startingFrom, actualPageSize, sortBy);

        Page<Event> eventsPage = eventRepository.findPublishedEventsWithFilters(
                searchText, categories, paid, startTime, endTime, pageable);
        List<Event> events = eventsPage.getContent();

        events = applyAdditionalFilters(events, startTime, endTime, onlyAvailable);
        Map<Long, Long> eventViews = retrieveEventsViewCounts(events);

        List<EventShortDto> resultEvents = events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.convertToBriefDto(event);
                    Integer confirmedCount = eventRepository.countConfirmedParticipations(event.getId());
                    dto.setConfirmedRequests(confirmedCount != null ? confirmedCount.longValue() : 0L);
                    dto.setViews(eventViews.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());

        return sortEvents(resultEvents, sortBy);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEventDetails(Long eventId, HttpServletRequest httpRequest) {
        log.info("Retrieving public event details for ID: {}", eventId);

        Event event = eventRepository.findPublishedEventById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Published event not found with ID: " + eventId));

        recordEndpointAccess(httpRequest);

        EventFullDto eventDto = createEventFullDtoWithStats(event, retrieveEventsViewCounts(List.of(event)));
        log.info("Retrieved event details for ID: {}", eventId);
        return eventDto;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEventDetails(Long userId, Long eventId) {
        log.info("Retrieving event details for user ID: {}, event ID: {}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new AccessDeniedException("User is not the initiator of this event");
        }

        return createEventFullDtoWithStats(event, retrieveEventsViewCounts(List.of(event)));
    }

    @Override
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Updating event ID: {} for user ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        validateUserEventAccess(userId, event);
        validateEventModificationTime(event);

        if (!Event.EventStatus.PENDING.equals(event.getStatus()) &&
                !Event.EventStatus.CANCELED.equals(event.getStatus())) {
            throw new DataConflictException("Only pending or canceled events can be modified");
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getEventDate() != null) {
            LocalDateTime newEventDate = parseDateTimeString(request.getEventDate());
            validateEventModificationTime(newEventDate);
            event.setEventDate(newEventDate);
        }

        if (request.getCategory() != null) {
            EventCategory newCategory = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            event.setCategory(newCategory);
        }

        if (request.getLocation() != null) {
            EventLocation location = new EventLocation();
            location.setLatitude(request.getLocation().getLat());
            location.setLongitude(request.getLocation().getLon());
            event.setLocation(location);
        }

        if (request.getStateAction() != null) {
            processUserStateAction(event, request.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Successfully updated event ID: {} for user ID: {}", eventId, userId);

        return createEventFullDtoWithStats(updatedEvent, retrieveEventsViewCounts(List.of(updatedEvent)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, int startingFrom, int pageSize) {
        log.info("Retrieving events for user ID: {}, from: {}, size: {}", userId, startingFrom, pageSize);

        Pageable pageable = PageRequest.of(startingFrom / pageSize, pageSize);
        Page<Event> userEventsPage = eventRepository.findEventsByInitiator(userId, pageable);
        List<Event> userEvents = userEventsPage.getContent();

        Map<Long, Long> eventViews = retrieveEventsViewCounts(userEvents);

        return userEvents.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.convertToBriefDto(event);
                    Integer confirmedCount = eventRepository.countConfirmedParticipations(event.getId());
                    dto.setConfirmedRequests(confirmedCount != null ? confirmedCount.longValue() : 0L);
                    dto.setViews(eventViews.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipations(Long userId, Long eventId) {
        log.info("Retrieving participations for event ID: {} by user ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        validateUserEventAccess(userId, event);

        return participationRepository.findEventParticipations(eventId)
                .stream()
                .map(participationMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult processParticipationStatusUpdate(
            Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdate) {

        log.info("Processing participation status update for event ID: {} by user ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        validateUserEventAccess(userId, event);

        List<EventParticipation> participations = participationRepository
                .findParticipationsByIdList(statusUpdate.getRequestIds());

        validateParticipationUpdate(participations, eventId);

        Integer currentConfirmed = participationRepository.countParticipationsByStatus(
                eventId, EventParticipation.ParticipationStatus.CONFIRMED);

        return processStatusUpdate(participations, statusUpdate.getStatus(), event, currentConfirmed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> findEventsForAdmin(List<Long> userIds, List<String> states, List<Long> categories,
                                                 String rangeStart, String rangeEnd, int startingFrom, int pageSize) {

        log.info("Admin event search - users: {}, states: {}, categories: {}", userIds, states, categories);

        LocalDateTime startTime = parseDateTimeString(rangeStart);
        LocalDateTime endTime = parseDateTimeString(rangeEnd);

        List<Event.EventStatus> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = states.stream()
                    .map(Event.EventStatus::valueOf)
                    .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(startingFrom / pageSize, pageSize);
        Page<Event> eventsPage = eventRepository.findEventsWithAdminFilters(
                userIds, eventStates, categories, startTime, endTime, pageable);
        List<Event> events = eventsPage.getContent();

        Map<Long, Long> eventViews = retrieveEventsViewCounts(events);

        return events.stream()
                .map(event -> createEventFullDtoWithStats(event, eventViews))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("Admin updating event ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        validateAdminEventUpdate(event, request);

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getEventDate() != null) {
            LocalDateTime newEventDate = parseDateTimeString(request.getEventDate());
            validateAdminEventModificationTime(newEventDate);
            event.setEventDate(newEventDate);
        }

        if (request.getCategory() != null) {
            EventCategory newCategory = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            event.setCategory(newCategory);
        }

        if (request.getLocation() != null) {
            if (event.getLocation() == null) {
                event.setLocation(new EventLocation());
            }
            event.getLocation().setLatitude(request.getLocation().getLat());
            event.getLocation().setLongitude(request.getLocation().getLon());
        }

        if (request.getStateAction() != null) {
            processAdminStateAction(event, request.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Admin successfully updated event ID: {}", eventId);

        return createEventFullDtoWithStats(updatedEvent, retrieveEventsViewCounts(List.of(updatedEvent)));
    }

    private EventRequestStatusUpdateResult processStatusUpdate(
            List<EventParticipation> participations,
            String action,
            Event event,
            Integer currentConfirmed) {

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        Integer participantLimit = event.getParticipantLimit();
        boolean hasLimit = participantLimit != null && participantLimit > 0;
        boolean requiresModeration = Boolean.TRUE.equals(event.getRequestModeration());

        if (!hasLimit || !requiresModeration) {
            for (EventParticipation participation : participations) {
                participation.setStatus(EventParticipation.ParticipationStatus.CONFIRMED);
                confirmed.add(participationMapper.convertToDto(participation));
            }
            participationRepository.saveAll(participations);
            return new EventRequestStatusUpdateResult(confirmed, rejected);
        }

        long confirmedCount = currentConfirmed != null ? currentConfirmed.longValue() : 0L;

        for (EventParticipation participation : participations) {
            if ("CONFIRMED".equals(action)) {
                if (confirmedCount >= participantLimit) {
                    throw new DataConflictException("Participant limit reached for event");
                }
                participation.setStatus(EventParticipation.ParticipationStatus.CONFIRMED);
                confirmed.add(participationMapper.convertToDto(participation));
                confirmedCount++;
            } else if ("REJECTED".equals(action)) {
                participation.setStatus(EventParticipation.ParticipationStatus.REJECTED);
                rejected.add(participationMapper.convertToDto(participation));
            }
        }

        participationRepository.saveAll(participations);

        if (confirmedCount >= participantLimit && "CONFIRMED".equals(action)) {
            List<EventParticipation> pendingParticipations = participationRepository
                    .findParticipationsByStatus(event.getId(), EventParticipation.ParticipationStatus.PENDING);

            for (EventParticipation pending : pendingParticipations) {
                pending.setStatus(EventParticipation.ParticipationStatus.REJECTED);
                rejected.add(participationMapper.convertToDto(pending));
            }

            if (!pendingParticipations.isEmpty()) {
                participationRepository.saveAll(pendingParticipations);
            }
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    private EventFullDto createEventFullDtoWithStats(Event event, Map<Long, Long> eventViews) {
        EventFullDto dto = eventMapper.convertToDetailedDto(event);

        Integer confirmedCount = eventRepository.countConfirmedParticipations(event.getId());
        dto.setConfirmedRequests(confirmedCount != null ? confirmedCount.longValue() : 0L);

        dto.setViews(eventViews.getOrDefault(event.getId(), 0L));

        return dto;
    }

    private void validateEventCreationTime(String eventDateString) {
        LocalDateTime eventDateTime = parseDateTimeString(eventDateString);
        if (eventDateTime.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Event must be scheduled at least " + MIN_HOURS_BEFORE_EVENT + " hours from now");
        }
    }

    private void recordEndpointAccess(HttpServletRequest request) {
        try {
            EndpointHit hit = EndpointHit.builder()
                    .app("ewm-main-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();
            statisticsClient.sendAccessRecord(hit);
        } catch (Exception e) {
            log.warn("Failed to record endpoint access statistics", e);
        }
    }

    private LocalDateTime parseDateTimeString(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid datetime format. Expected: yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateDateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException("Start datetime cannot be after end datetime");
        }
    }

    private Pageable createPageable(int from, int size, String sort) {
        Sort sorting = Sort.by(Sort.Direction.ASC, "eventDate");
        if ("VIEWS".equals(sort)) {
            sorting = Sort.by(Sort.Direction.DESC, "views");
        }
        return PageRequest.of(from / size, size, sorting);
    }

    private List<Event> applyAdditionalFilters(List<Event> events, LocalDateTime start, LocalDateTime end, boolean onlyAvailable) {
        return events.stream()
                .filter(event -> !onlyAvailable || isEventAvailable(event))
                .collect(Collectors.toList());
    }

    private boolean isEventAvailable(Event event) {
        Integer participantLimit = event.getParticipantLimit();
        if (participantLimit == null || participantLimit == 0) {
            return true;
        }
        Integer confirmedCount = eventRepository.countConfirmedParticipations(event.getId());
        return confirmedCount < participantLimit;
    }

    private Map<Long, Long> retrieveEventsViewCounts(List<Event> events) {
        if (events.isEmpty()) return new HashMap<>();

        List<String> eventUris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStats> stats = statisticsClient.fetchAccessStatistics(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now(),
                    eventUris,
                    true);

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> extractEventIdFromUri(stat.getUri()),
                            ViewStats::getHits
                    ));
        } catch (Exception e) {
            log.warn("Failed to retrieve view statistics", e);
            return new HashMap<>();
        }
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            return Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
        } catch (Exception e) {
            log.warn("Failed to extract event ID from URI: {}", uri);
            return -1L;
        }
    }

    private List<EventShortDto> sortEvents(List<EventShortDto> events, String sortBy) {
        if ("VIEWS".equals(sortBy)) {
            events.sort((e1, e2) -> Long.compare(e2.getViews(), e1.getViews()));
        }
        return events;
    }

    private void validateUserEventAccess(Long userId, Event event) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new AccessDeniedException("User is not authorized to modify this event");
        }
    }

    private void validateEventModificationTime(Event event) {
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Event cannot be modified less than " + MIN_HOURS_BEFORE_EVENT + " hours before start");
        }
    }

    private void validateEventModificationTime(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Event must be at least " + MIN_HOURS_BEFORE_EVENT + " hours in the future");
        }
    }

    private void validateAdminEventModificationTime(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(ADMIN_MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Event must be at least " + ADMIN_MIN_HOURS_BEFORE_EVENT + " hours in the future for admin updates");
        }
    }

    private void validateAdminEventUpdate(Event event, UpdateEventAdminRequest request) {
        if (request.getStateAction() == UpdateEventAdminRequest.AdminAction.PUBLISH_EVENT) {
            if (Event.EventStatus.PUBLISHED.equals(event.getStatus())) {
                throw new DataConflictException("Cannot publish already published event");
            }
            if (Event.EventStatus.CANCELED.equals(event.getStatus())) {
                throw new DataConflictException("Cannot publish canceled event");
            }
            if (!Event.EventStatus.PENDING.equals(event.getStatus())) {
                throw new ValidationException("Only pending events can be published");
            }
        }

        if (request.getStateAction() == UpdateEventAdminRequest.AdminAction.REJECT_EVENT) {
            if (Event.EventStatus.PUBLISHED.equals(event.getStatus())) {
                throw new DataConflictException("Cannot reject published event");
            }
        }
    }

    private void validateParticipationUpdate(List<EventParticipation> participations, Long eventId) {
        for (EventParticipation participation : participations) {
            if (!participation.getEvent().getId().equals(eventId)) {
                throw new DataConflictException("Participation does not belong to the specified event");
            }
            if (!EventParticipation.ParticipationStatus.PENDING.equals(participation.getStatus())) {
                throw new DataConflictException("Cannot update non-pending participation request");
            }
        }
    }

    private void processUserStateAction(Event event, UpdateEventUserRequest.UserAction action) {
        switch (action) {
            case CANCEL_REVIEW:
                event.setStatus(Event.EventStatus.CANCELED);
                break;
            case SEND_TO_REVIEW:
                if (Event.EventStatus.CANCELED.equals(event.getStatus())) {
                    event.setStatus(Event.EventStatus.PENDING);
                }
                break;
            default:
                throw new ValidationException("Unknown user action: " + action);
        }
    }

    private void processAdminStateAction(Event event, UpdateEventAdminRequest.AdminAction action) {
        switch (action) {
            case PUBLISH_EVENT:
                event.setStatus(Event.EventStatus.PUBLISHED);
                event.setPublicationDate(LocalDateTime.now());
                break;
            case REJECT_EVENT:
                event.setStatus(Event.EventStatus.CANCELED);
                break;
            default:
                throw new ValidationException("Unknown admin action: " + action);
        }
    }
}