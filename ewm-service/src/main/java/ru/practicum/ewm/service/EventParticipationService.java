package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.EventParticipationDto;

import java.util.List;

public interface EventParticipationService {
    EventParticipationDto createParticipationRequest(Long userId, Long eventId);

    List<EventParticipationDto> getUserParticipationRequests(Long userId);

    EventParticipationDto cancelParticipationRequest(Long userId, Long requestId);
}