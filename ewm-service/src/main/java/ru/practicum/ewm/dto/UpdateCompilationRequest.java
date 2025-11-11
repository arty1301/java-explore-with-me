package ru.practicum.ewm.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompilationRequest {
    @Size(max = 50, message = "Title cannot exceed 50 characters")
    private String title;

    private Boolean pinned;
    private Set<Long> events;
}