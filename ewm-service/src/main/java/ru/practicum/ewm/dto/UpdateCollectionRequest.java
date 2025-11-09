package ru.practicum.ewm.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCollectionRequest {
    @Size(max = 50, message = "Title must be up to 50 characters")
    private String title;

    private Boolean pinned;
    private Set<Long> events;
}