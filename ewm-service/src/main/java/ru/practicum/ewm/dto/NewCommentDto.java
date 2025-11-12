package ru.practicum.ewm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCommentDto {

    @NotBlank(message = "Comment text cannot be empty")
    @Size(min = 1, max = 2000, message = "Comment length must be between 1 and 2000 characters")
    private String text;

    @NotNull(message = "Event ID must be specified")
    private Long eventId;
}