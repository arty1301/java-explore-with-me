package ru.practicum.ewm.controller.priv;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class EventPrivateController {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Long userId,
                                    @Valid @RequestBody NewEventDto eventData) {
        return eventService.createEvent(userId, eventData);
    }

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getUserEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        List<EventShortDto> events = eventService.retrieveUserEvents(userId, from, size);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventDetails(@PathVariable Long userId,
                                        @PathVariable Long eventId) {
        return eventService.retrieveUserEventDetails(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateUserEvent(@PathVariable Long userId,
                                        @PathVariable Long eventId,
                                        @Valid @RequestBody UpdateEventUserRequest updateRequest) {
        return eventService.updateEventByInitiator(userId, eventId, updateRequest);
    }

    @GetMapping("/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getEventParticipationRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        List<ParticipationRequestDto> requests = eventService.getEventParticipationRequests(userId, eventId);
        return ResponseEntity.ok(requests);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest statusUpdate) {
        return eventService.updateParticipationRequestStatus(userId, eventId, statusUpdate);
    }
}