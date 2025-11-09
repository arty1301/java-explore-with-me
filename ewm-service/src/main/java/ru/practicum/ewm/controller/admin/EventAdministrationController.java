package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventDetailedDto;
import ru.practicum.ewm.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.service.EventManagementService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class EventAdministrationController {

    private final EventManagementService eventService;

    @GetMapping
    public ResponseEntity<List<EventDetailedDto>> searchEventsWithAdminFilters(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Admin: Searching events with filters - users: {}, states: {}, categories: {}",
                users, states, categories);

        List<EventDetailedDto> events = eventService.findEventsForAdmin(
                users, states, categories, rangeStart, rangeEnd, from, size);

        log.info("Admin: Found {} events matching criteria", events.size());
        return ResponseEntity.ok(events);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventDetailedDto> updateEventByAdministrator(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventAdminRequest adminRequest) {

        log.info("Admin: Processing administrative update for event ID: {}", eventId);
        EventDetailedDto updatedEvent = eventService.updateEventByAdmin(eventId, adminRequest);
        log.info("Admin: Successfully updated event ID: {}", eventId);
        return ResponseEntity.ok(updatedEvent);
    }
}