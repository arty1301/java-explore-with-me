package ru.practicum.ewm.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCommentDto {

    @Size(min = 1, max = 2000, message = "Comment length must be between 1 and 2000 characters")
    private String text;
}