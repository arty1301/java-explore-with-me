package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.*;

import java.util.List;

public interface EventService {
    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        String rangeStart, String rangeEnd, boolean onlyAvailable,
                                        String sort, int from, int size, HttpServletRequest request);

    EventFullDto getPublicEvent(Long id, HttpServletRequest request);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest requestDto);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    List<ParticipationRequestDto> getEventParticipationRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult changeParticipationRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdateRequest);

    List<EventFullDto> findEventsAdmin(List<Long> userIds, List<String> states, List<Long> categories,
                                       String rangeStart, String rangeEnd, int from, int size);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto);
}