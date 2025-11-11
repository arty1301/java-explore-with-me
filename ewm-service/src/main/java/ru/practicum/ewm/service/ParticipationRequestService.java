package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {
    ParticipationRequestDto submitRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> retrieveUserRequests(Long userId);

    ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId);
}