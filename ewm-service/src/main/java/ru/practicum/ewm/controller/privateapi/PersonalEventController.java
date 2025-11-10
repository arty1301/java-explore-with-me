package ru.practicum.ewm.controller.privateapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.service.EventManagementService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PersonalEventController {

    private final EventManagementService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto eventRequest) {

        log.info("User {}: Creating new event", userId);
        EventFullDto createdEvent = eventService.createEvent(userId, eventRequest);
        log.info("User {}: Successfully created event with ID: {}", userId, createdEvent.getId());
        return createdEvent;
    }

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        log.info("User {}: Retrieving events from: {}, size: {}", userId, from, size);
        List<EventShortDto> userEvents = eventService.getUserEvents(userId, from, size);
        log.info("User {}: Retrieved {} events", userId, userEvents.size());
        return ResponseEntity.ok(userEvents);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        log.info("User {}: Retrieving details for event ID: {}", userId, eventId);
        EventFullDto eventDetails = eventService.getUserEventDetails(userId, eventId);
        log.info("User {}: Retrieved details for event ID: {}", userId, eventId);
        return eventDetails;
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateRequest) {

        log.info("User {}: Updating event ID: {}", userId, eventId);
        EventFullDto updatedEvent = eventService.updateUserEvent(userId, eventId, updateRequest);
        log.info("User {}: Successfully updated event ID: {}", userId, eventId);
        return updatedEvent;
    }

    @GetMapping("/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getEventParticipants(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        log.info("User {}: Retrieving participation requests for event ID: {}", userId, eventId);
        List<ParticipationRequestDto> participations = eventService.getEventParticipations(userId, eventId);
        log.info("User {}: Retrieved {} participation requests", userId, participations.size());
        return ResponseEntity.ok(participations);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest statusUpdate) {

        log.info("User {}: Processing status updates for event ID: {}", userId, eventId);
        EventRequestStatusUpdateResult result = eventService.processParticipationStatusUpdate(
                userId, eventId, statusUpdate);
        log.info("User {}: Processed {} confirmations, {} rejections",
                userId, result.getConfirmedRequests().size(), result.getRejectedRequests().size());
        return result;
    }
}