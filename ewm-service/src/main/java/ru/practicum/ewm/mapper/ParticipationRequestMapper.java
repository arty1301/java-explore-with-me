package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.model.ParticipationRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    @Mapping(source = "requester.id", target = "requester")
    @Mapping(source = "event.id", target = "event")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "created", target = "created", qualifiedByName = "formatLocalDateTime")
    ParticipationRequestDto toParticipationRequestDto(ParticipationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "created", ignore = true)
    ParticipationRequest toParticipationRequestEntity(ParticipationRequestDto dto);

    @Named("formatLocalDateTime")
    default String formatLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String baseFormat = dateTime.format(formatter);
        int millis = dateTime.getNano() / 1_000_000;
        return String.format("%s.%03d", baseFormat, millis);
    }

    @Named("statusToString")
    default String statusToString(ParticipationRequest.Status status) {
        return status != null ? status.name() : null;
    }
}