package ru.practicum.ewm.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCompilationRequest {
    @Size(min = 1, max = 50, message = "Title must be between 1 and 50 characters")
    private String title;

    private Boolean pinned;
    private Set<Long> events;
}