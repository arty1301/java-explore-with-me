package ru.practicum.ewm.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationStatusUpdateResult {
    private List<EventParticipationDto> confirmedRequests;
    private List<EventParticipationDto> rejectedRequests;
}