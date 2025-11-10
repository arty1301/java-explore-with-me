package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.model.EventParticipation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface EventParticipationMapper {

    @Mapping(source = "requester.id", target = "requester")
    @Mapping(source = "event.id", target = "event")
    @Mapping(source = "status", target = "status", qualifiedByName = "participationStatusToString")
    @Mapping(source = "creationTime", target = "created", qualifiedByName = "formatLocalDateTime")
    ParticipationRequestDto convertToDto(EventParticipation participation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "creationTime", ignore = true)
    EventParticipation convertToEntity(ParticipationRequestDto dto);

    @Named("formatLocalDateTime")
    default String formatLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String baseFormat = dateTime.format(formatter);

        int nanos = dateTime.getNano();
        int millis = nanos / 1_000_000;

        return String.format("%s.%03d", baseFormat, millis);
    }

    @Named("participationStatusToString")
    default String participationStatusToString(EventParticipation.ParticipationStatus status) {
        return status != null ? status.name() : null;
    }

    default EventParticipation.ParticipationStatus stringToParticipationStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return EventParticipation.ParticipationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}