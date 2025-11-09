package ru.practicum.ewm.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBriefDto {
    private Long id;
    private String name;
}