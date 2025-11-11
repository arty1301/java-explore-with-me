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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final ParticipationRequestRepository participationRequestRepository;
    private final ParticipationRequestMapper participationRequestMapper;
    private final StatisticsClient statsClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (dto.getParticipantLimit() == null) {
            dto.setParticipantLimit(0);
        }

        if (dto.getRequestModeration() == null) {
            dto.setRequestModeration(true);
        }

        if (dto.getPaid() == null) {
            dto.setPaid(false);
        }

        Event event = eventMapper.fromNewDto(dto);

        if (LocalDateTime.now().plusHours(2).isAfter(event.getEventDate())) {
            throw new ConditionsNotMetException("Event date must be at least 2 hours from now");
        }

        event.setInitiator(initiator);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(Event.EventState.PENDING);

        Event saved = eventRepository.save(event);
        return eventMapper.toFullDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd, boolean onlyAvailable,
                                               String sort, int from, int size, HttpServletRequest request) {

        LocalDateTime startDate = parseDateTime(rangeStart);
        LocalDateTime endDate = parseDateTime(rangeEnd);
        LocalDateTime now = LocalDateTime.now();

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ConditionsNotMetException("Invalid date range: start date after end date");
        }

        EndpointHit hitRequest = EndpointHit.builder()
                .app("ewm-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();

        statsClient.sendAccessRecord(hitRequest);

        int maxSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(from / maxSize, Math.max(maxSize, 10));

        List<Event> events = eventRepository.findPublicEventsWithFilters(
                text, categories, paid, now, pageable);

        if (startDate != null || endDate != null) {
            events = events.stream()
                    .filter(event -> {
                        LocalDateTime eventDate = event.getEventDate();
                        boolean afterStart = startDate == null || !eventDate.isBefore(startDate);
                        boolean beforeEnd = endDate == null || !eventDate.isAfter(endDate);
                        return afterStart && beforeEnd;
                    })
                    .collect(Collectors.toList());
        }

        if (onlyAvailable) {
            events = events.stream()
                    .filter(this::isEventAvailable)
                    .collect(Collectors.toList());
        }

        Map<String, Long> viewsMap = getViewsForEvents(events);

        List<EventShortDto> eventDtos = events.stream()
                .map(event -> {
                    EventShortDto shortDto = eventMapper.toShortDto(event);
                    String eventUri = "/events/" + event.getId();
                    shortDto.setViews(viewsMap.getOrDefault(eventUri, 0L));

                    Integer confirmedRequests = eventRepository.countConfirmedRequestsByEventId(
                            event.getId(), ParticipationRequest.Status.CONFIRMED);
                    shortDto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);

                    return shortDto;
                })
                .collect(Collectors.toList());

        if ("VIEWS".equals(sort)) {
            eventDtos.sort((e1, e2) -> Long.compare(e2.getViews(), e1.getViews()));
        } else {
            eventDtos.sort((e1, e2) -> e1.getEventDate().compareTo(e2.getEventDate()));
        }

        return eventDtos;
    }

    private boolean isEventAvailable(Event event) {
        if (event.getParticipantLimit() == null || event.getParticipantLimit() == 0) {
            return true;
        }

        Integer confirmedRequests = eventRepository.countConfirmedRequestsByEventId(event.getId(),
                ParticipationRequest.Status.CONFIRMED);
        return confirmedRequests < event.getParticipantLimit();
    }

    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            throw new ConditionsNotMetException("Invalid date format. Expected: yyyy-MM-dd HH:mm:ss");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEvent(Long id, HttpServletRequest request) {
        Event event = eventRepository.findPublishedEventById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        EndpointHit hitDto = EndpointHit.builder()
                .app("ewm-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();

        statsClient.sendAccessRecord(hitDto);

        Integer confirmedRequests = eventRepository.countConfirmedRequestsByEventId(id,
                ParticipationRequest.Status.CONFIRMED);

        List<ViewStats> statsResponse = statsClient.fetchAccessStatistics(
                event.getCreatedOn(),
                LocalDateTime.now(),
                List.of(request.getRequestURI()),
                true);

        Long views = statsResponse.stream()
                .filter(stat -> stat.getUri().equals(request.getRequestURI()))
                .findFirst()
                .map(ViewStats::getHits)
                .orElse(0L);

        EventFullDto eventDto = eventMapper.toFullDto(event);
        eventDto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);
        eventDto.setViews(views);

        return eventDto;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Cannot access another user's event");
        }

        EventFullDto eventDto = eventMapper.toFullDto(event);
        Integer confirmedRequests = eventRepository.countConfirmedRequestsByEventId(eventId,
                ParticipationRequest.Status.CONFIRMED);
        eventDto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);

        return eventDto;
    }

    @Override
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest requestDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Cannot update another user's event");
        }

        if (Event.EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Cannot update published event");
        }

        if (requestDto.getEventDate() != null) {
            LocalDateTime newEventDate = parseDateTime(requestDto.getEventDate());
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ConditionsNotMetException("Event date must be at least 2 hours from now");
            }
            event.setEventDate(newEventDate);
        } else if (LocalDateTime.now().plusHours(2).isAfter(event.getEventDate())) {
            throw new ConditionsNotMetException("Event date must be at least 2 hours from now");
        }

        if (requestDto.getAnnotation() != null) {
            event.setAnnotation(requestDto.getAnnotation());
        }

        if (requestDto.getCategory() != null) {
            Category category = categoryRepository.findById(requestDto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }

        if (requestDto.getDescription() != null) {
            event.setDescription(requestDto.getDescription());
        }

        if (requestDto.getPaid() != null) {
            event.setPaid(requestDto.getPaid());
        }

        if (requestDto.getParticipantLimit() != null) {
            event.setParticipantLimit(requestDto.getParticipantLimit());
        }

        if (requestDto.getRequestModeration() != null) {
            event.setRequestModeration(requestDto.getRequestModeration());
        }

        if (requestDto.getTitle() != null) {
            event.setTitle(requestDto.getTitle());
        }

        if (requestDto.getLocation() != null) {
            event.setLocation(requestDto.getLocation());
        }

        if (requestDto.getStateAction() != null) {
            switch (requestDto.getStateAction()) {
                case CANCEL_REVIEW:
                    event.setState(Event.EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(Event.EventState.PENDING);
                    break;
                default:
                    throw new ConditionsNotMetException("Unknown state action");
            }
        }

        Event savedEvent = eventRepository.save(event);
        EventFullDto eventDto = eventMapper.toFullDto(savedEvent);

        Integer confirmedRequests = eventRepository.countConfirmedRequestsByEventId(eventId,
                ParticipationRequest.Status.CONFIRMED);
        eventDto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);

        return eventDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);

        // Используем существующий метод или создаем stream фильтрацию
        List<Event> events = eventRepository.findAll().stream()
                .filter(event -> event.getInitiator().getId().equals(userId))
                .sorted((e1, e2) -> e2.getEventDate().compareTo(e1.getEventDate()))
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());

        if (events.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Long> viewsMap = getViewsForEvents(events);

        return events.stream()
                .map(event -> {
                    EventShortDto shortDto = eventMapper.toShortDto(event);
                    String eventUri = "/events/" + event.getId();
                    shortDto.setViews(viewsMap.getOrDefault(eventUri, 0L));

                    Integer confirmedRequests = eventRepository.countConfirmedRequestsByEventId(
                            event.getId(), ParticipationRequest.Status.CONFIRMED);
                    shortDto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);

                    return shortDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipationRequests(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Cannot access another user's event requests");
        }

        return participationRequestRepository.findByEventId(eventId).stream()
                .map(participationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult changeParticipationRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdateRequest) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Cannot update another user's event");
        }

        List<Long> requestIds = statusUpdateRequest.getRequestIds();
        EventRequestStatusUpdateRequest.Status newStatus = statusUpdateRequest.getStatus();

        List<ParticipationRequest> requests = participationRequestRepository.findAllById(requestIds);

        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Request does not belong to this event");
            }
            if (!ParticipationRequest.Status.PENDING.equals(request.getStatus())) {
                throw new ConflictException("Cannot change status of non-pending request");
            }
        }

        Integer currentConfirmedCount = participationRequestRepository
                .countByEventIdAndStatus(eventId, ParticipationRequest.Status.CONFIRMED);
        int confirmedCount = currentConfirmedCount != null ? currentConfirmedCount : 0;

        Integer participantLimit = event.getParticipantLimit();
        boolean hasLimit = participantLimit != null && participantLimit > 0;

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        if (!hasLimit || Boolean.FALSE.equals(event.getRequestModeration())) {
            for (ParticipationRequest request : requests) {
                request.setStatus(ParticipationRequest.Status.CONFIRMED);
                confirmedRequests.add(participationRequestMapper.toDto(request));
            }
            participationRequestRepository.saveAll(requests);
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(confirmedRequests)
                    .rejectedRequests(rejectedRequests)
                    .build();
        }

        for (ParticipationRequest request : requests) {
            if (EventRequestStatusUpdateRequest.Status.CONFIRMED.equals(newStatus)) {
                if (confirmedCount >= participantLimit) {
                    throw new ConflictException("Participant limit reached for this event");
                }
                request.setStatus(ParticipationRequest.Status.CONFIRMED);
                confirmedRequests.add(participationRequestMapper.toDto(request));
                confirmedCount++;
            } else if (EventRequestStatusUpdateRequest.Status.REJECTED.equals(newStatus)) {
                request.setStatus(ParticipationRequest.Status.REJECTED);
                rejectedRequests.add(participationRequestMapper.toDto(request));
            }
        }

        participationRequestRepository.saveAll(requests);

        if (confirmedCount >= participantLimit) {
            List<ParticipationRequest> pendingRequests = participationRequestRepository
                    .findByEventIdAndStatus(eventId, ParticipationRequest.Status.PENDING);

            for (ParticipationRequest pendingRequest : pendingRequests) {
                pendingRequest.setStatus(ParticipationRequest.Status.REJECTED);
                rejectedRequests.add(participationRequestMapper.toDto(pendingRequest));
            }

            if (!pendingRequests.isEmpty()) {
                participationRequestRepository.saveAll(pendingRequests);
            }
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> findEventsAdmin(List<Long> userIds, List<String> states, List<Long> categories,
                                              String rangeStart, String rangeEnd, int from, int size) {

        LocalDateTime start = parseDateTime(rangeStart);
        LocalDateTime end = parseDateTime(rangeEnd);

        List<Event.EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = states.stream()
                    .map(Event.EventState::valueOf)
                    .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findEventsWithFilters(userIds, eventStates, categories, start, end, pageable);

        if (events.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Long> viewsMap = getViewsForEvents(events);

        return events.stream()
                .map(event -> mapToEventFullDto(event, viewsMap))
                .collect(Collectors.toList());
    }

    private EventFullDto mapToEventFullDto(Event event, Map<String, Long> viewsMap) {
        EventFullDto dto = eventMapper.toFullDto(event);

        Integer confirmedRequests = eventRepository.countConfirmedRequestsByEventId(
                event.getId(),
                ParticipationRequest.Status.CONFIRMED
        );
        dto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);

        String eventUri = "/events/" + event.getId();
        Long views = viewsMap.getOrDefault(eventUri, 0L);
        dto.setViews(views);

        return dto;
    }

    private Map<String, Long> getViewsForEvents(List<Event> events) {
        if (events.isEmpty()) {
            return new HashMap<>();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        LocalDateTime end = LocalDateTime.now();

        try {
            List<ViewStats> stats = statsClient.fetchAccessStatistics(start, end, uris, true);

            Map<String, Long> viewsMap = new HashMap<>();

            for (ViewStats stat : stats) {
                viewsMap.put(stat.getUri(), stat.getHits());
            }

            for (String uri : uris) {
                viewsMap.putIfAbsent(uri, 0L);
            }

            return viewsMap;
        } catch (Exception e) {
            log.warn("Failed to get views statistics", e);
            Map<String, Long> viewsMap = new HashMap<>();
            for (Event event : events) {
                viewsMap.put("/events/" + event.getId(), 0L);
            }
            return viewsMap;
        }
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (dto.getStateAction() != null) {
            if (UpdateEventAdminRequest.StateAction.PUBLISH_EVENT.equals(dto.getStateAction())) {
                if (!Event.EventState.PENDING.equals(event.getState())) {
                    throw new ConflictException("Cannot publish event that is not in pending state");
                }
                if (LocalDateTime.now().plusHours(1).isAfter(event.getEventDate())) {
                    throw new ConditionsNotMetException("Event date must be at least 1 hour from now for publication");
                }
                event.setState(Event.EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (UpdateEventAdminRequest.StateAction.REJECT_EVENT.equals(dto.getStateAction())) {
                if (Event.EventState.PUBLISHED.equals(event.getState())) {
                    throw new ConflictException("Cannot reject published event");
                }
                event.setState(Event.EventState.CANCELED);
            }
        }

        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }

        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }

        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }

        if (dto.getEventDate() != null) {
            LocalDateTime newEventDate = parseDateTime(dto.getEventDate());
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConditionsNotMetException("Event date must be at least 1 hour from now");
            }
            event.setEventDate(newEventDate);
        }

        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
        Event updated = eventRepository.save(event);
        return mapToEventFullDto(updated, getViewsForEvents(List.of(updated)));
    }
}