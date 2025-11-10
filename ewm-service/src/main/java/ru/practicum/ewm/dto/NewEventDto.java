package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewEventDto {
    @NotBlank(message = "Event annotation cannot be blank")
    @Size(min = 20, max = 2000, message = "Annotation must be between 20 and 2000 characters")
    private String annotation;

    @NotNull(message = "Category ID cannot be null")
    private Long category;

    @NotBlank(message = "Event description cannot be blank")
    @Size(min = 20, max = 7000, message = "Description must be between 20 and 7000 characters")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "Event date cannot be null")
    @Future(message = "Event date must be in the future")
    private String eventDate;

    @NotNull(message = "Location cannot be null")
    private Location location;

    private Boolean paid = false;

    @PositiveOrZero(message = "Participant limit must be positive or zero")
    private Integer participantLimit = 0;

    private Boolean requestModeration = true;

    @NotBlank(message = "Event title cannot be blank")
    @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
    private String title;
}