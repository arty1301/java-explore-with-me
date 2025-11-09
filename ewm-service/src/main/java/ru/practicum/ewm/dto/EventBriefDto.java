package ru.practicum.ewm.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventBriefDto {
    private Long id;
    private String annotation;
    private EventCategoryDto category;
    private Long confirmedRequests;
    private String eventDate;
    private UserBriefDto initiator;
    private Boolean paid;
    private String title;
    private Long views;
}