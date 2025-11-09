package ru.practicum.ewm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventLocation {
    @Column(name = "lat")
    private Float latitude;

    @Column(name = "lon")
    private Float longitude;
}