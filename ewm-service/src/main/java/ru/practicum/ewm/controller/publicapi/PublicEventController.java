package ru.practicum.ewm.controller.publicapi;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventBriefDto;
import ru.practicum.ewm.dto.EventDetailedDto;
import ru.practicum.ewm.service.EventManagementService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventManagementService eventService;

    @GetMapping
    public ResponseEntity<List<EventBriefDto>> searchPublicEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "false") boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        log.info("Public: Searching events - text: {}, categories: {}, paid: {}, sort: {}",
                text, categories, paid, sort);
        log.info("Client IP: {}, Endpoint: {}", request.getRemoteAddr(), request.getRequestURI());

        List<EventBriefDto> events = eventService.findPublicEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, request);

        log.info("Public: Found {} events matching search criteria", events.size());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailedDto> retrievePublicEventDetails(
            @PathVariable Long eventId,
            HttpServletRequest request) {

        log.info("Public: Retrieving details for event ID: {}", eventId);
        log.info("Client IP: {}, Endpoint: {}", request.getRemoteAddr(), request.getRequestURI());

        EventDetailedDto eventDetails = eventService.getPublicEventDetails(eventId, request);
        log.info("Public: Retrieved details for event ID: {}", eventId);
        return ResponseEntity.ok(eventDetails);
    }
}