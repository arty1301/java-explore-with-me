package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.ParticipationRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {
    @Mapping(source = "requester.id", target = "requester")
    @Mapping(source = "event.id", target = "event")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "created", target = "created", qualifiedByName = "formatDateTime")
    ParticipationRequestDto toDto(ParticipationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "event", ignore = true)
    ParticipationRequest fromDto(ParticipationRequestDto dto);


    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String baseFormat = dateTime.format(formatter);

        int nanos = dateTime.getNano();
        int millis = nanos / 1_000_000;

        return String.format("%s.%03d", baseFormat, millis);
    }

    @Named("stateToString")
    default String stateToString(Event.EventState state) {
        return state != null ? state.name() : null;
    }
}