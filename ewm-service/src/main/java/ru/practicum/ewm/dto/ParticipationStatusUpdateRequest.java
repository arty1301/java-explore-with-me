package ru.practicum.ewm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationStatusUpdateRequest {
    private List<Long> requestIds;

    @NotNull(message = "Status cannot be null")
    private String status;

}