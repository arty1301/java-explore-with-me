package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.*;

import java.util.List;

public interface EventManagementService {
    EventFullDto createEvent(Long userId, NewEventDto request);

    List<EventShortDto> findPublicEvents(String searchText, List<Long> categories, Boolean paid,
                                         String rangeStart, String rangeEnd, boolean onlyAvailable,
                                         String sortBy, int startingFrom, int pageSize, HttpServletRequest httpRequest);

    EventFullDto getPublicEventDetails(Long eventId, HttpServletRequest httpRequest);

    EventFullDto getUserEventDetails(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventShortDto> getUserEvents(Long userId, int startingFrom, int pageSize);

    List<ParticipationRequestDto> getEventParticipations(Long userId, Long eventId);

    EventRequestStatusUpdateResult processParticipationStatusUpdate(
            Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdate);

    List<EventFullDto> findEventsForAdmin(List<Long> userIds, List<String> states, List<Long> categories,
                                          String rangeStart, String rangeEnd, int startingFrom, int pageSize);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);
}