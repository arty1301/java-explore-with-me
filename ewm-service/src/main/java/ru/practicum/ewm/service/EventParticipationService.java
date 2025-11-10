package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.ParticipationRequestDto;

import java.util.List;

public interface EventParticipationService {
    ParticipationRequestDto createParticipationRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getUserParticipationRequests(Long userId);

    ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId);
}