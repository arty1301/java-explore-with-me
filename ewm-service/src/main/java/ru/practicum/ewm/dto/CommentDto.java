package ru.practicum.ewm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {

    private Long id;
    private String text;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private String userName;
    private String created;
}