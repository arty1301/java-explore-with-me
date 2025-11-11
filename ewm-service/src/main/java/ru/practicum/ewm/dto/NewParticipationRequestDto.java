package ru.practicum.ewm.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewParticipationRequestDto {
    private Long eventId;
}