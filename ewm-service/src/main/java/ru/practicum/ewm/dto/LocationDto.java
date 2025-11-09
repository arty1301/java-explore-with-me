package ru.practicum.ewm.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationDto {
    private Float lat;
    private Float lon;
}