package ru.practicum.ewm.dto;

import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCollectionDto {
    private Long id;
    private String title;
    private Boolean pinned;
    private Set<EventBriefDto> events;
}