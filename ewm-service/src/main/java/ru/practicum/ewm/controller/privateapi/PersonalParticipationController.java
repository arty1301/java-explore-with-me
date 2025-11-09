package ru.practicum.ewm.controller.privateapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventParticipationDto;
import ru.practicum.ewm.service.EventParticipationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class PersonalParticipationController {

    private final EventParticipationService participationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventParticipationDto createParticipationRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {

        log.info("User {}: Creating participation request for event ID: {}", userId, eventId);
        EventParticipationDto participation = participationService.createParticipationRequest(userId, eventId);
        log.info("User {}: Successfully created participation request with ID: {}", userId, participation.getId());
        return participation;
    }

    @GetMapping
    public ResponseEntity<List<EventParticipationDto>> retrieveUserParticipationRequests(@PathVariable Long userId) {
        log.info("User {}: Retrieving all participation requests", userId);
        List<EventParticipationDto> participations = participationService.getUserParticipationRequests(userId);
        log.info("User {}: Retrieved {} participation requests", userId, participations.size());
        return ResponseEntity.ok(participations);
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<EventParticipationDto> cancelParticipationRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {

        log.info("User {}: Canceling participation request ID: {}", userId, requestId);
        EventParticipationDto canceledParticipation = participationService.cancelParticipationRequest(userId, requestId);
        log.info("User {}: Successfully canceled participation request ID: {}", userId, requestId);
        return ResponseEntity.ok(canceledParticipation);
    }
}