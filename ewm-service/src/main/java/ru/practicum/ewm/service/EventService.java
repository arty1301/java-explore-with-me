package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.*;

import java.util.List;

public interface EventService {
    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> searchPublicEvents(String text, List<Long> categories, Boolean paid,
                                           String rangeStart, String rangeEnd, boolean onlyAvailable,
                                           String sort, int from, int size, HttpServletRequest request);

    EventFullDto retrievePublicEvent(Long eventId, HttpServletRequest request);

    EventFullDto retrieveUserEventDetails(Long userId, Long eventId);

    EventFullDto updateEventByInitiator(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<EventShortDto> retrieveUserEvents(Long userId, int from, int size);

    List<ParticipationRequestDto> getEventParticipationRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateParticipationRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdate);

    List<EventFullDto> searchEventsByAdmin(List<Long> userIds, List<String> states, List<Long> categories,
                                           String rangeStart, String rangeEnd, int from, int size);

    EventFullDto updateEventByAdministrator(Long eventId, UpdateEventAdminRequest updateRequest);
}