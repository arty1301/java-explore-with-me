package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.*;

import java.util.List;

public interface EventManagementService {
    EventDetailedDto createEvent(Long userId, CreateEventRequest request);

    List<EventBriefDto> findPublicEvents(String searchText, List<Long> categories, Boolean paid,
                                         String rangeStart, String rangeEnd, boolean onlyAvailable,
                                         String sortBy, int startingFrom, int pageSize, HttpServletRequest httpRequest);

    EventDetailedDto getPublicEventDetails(Long eventId, HttpServletRequest httpRequest);

    EventDetailedDto getUserEventDetails(Long userId, Long eventId);

    EventDetailedDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventBriefDto> getUserEvents(Long userId, int startingFrom, int pageSize);

    List<EventParticipationDto> getEventParticipations(Long userId, Long eventId);

    ParticipationStatusUpdateResult processParticipationStatusUpdate(
            Long userId, Long eventId, ParticipationStatusUpdateRequest statusUpdate);

    List<EventDetailedDto> findEventsForAdmin(List<Long> userIds, List<String> states, List<Long> categories,
                                              String rangeStart, String rangeEnd, int startingFrom, int pageSize);

    EventDetailedDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);
}