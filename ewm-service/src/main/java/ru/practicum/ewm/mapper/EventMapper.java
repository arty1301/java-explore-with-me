package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.NewEventDto;
import ru.practicum.ewm.model.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", uses = {EventCategoryMapper.class, PlatformUserMapper.class})
public interface EventMapper {

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    @Mapping(source = "location", target = "location")
    @Mapping(source = "creationDate", target = "createdOn", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "eventDate", target = "eventDate", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "publicationDate", target = "publishedOn", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "status", target = "state", qualifiedByName = "eventStatusToString")
    EventFullDto convertToDetailedDto(Event event);

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    @Mapping(source = "eventDate", target = "eventDate", qualifiedByName = "formatLocalDateTime")
    EventShortDto convertToBriefDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "publicationDate", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(source = "eventDate", target = "eventDate", qualifiedByName = "stringToLocalDateTime")
    Event convertToEntity(NewEventDto request);

    @Named("formatLocalDateTime")
    default String formatLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    @Named("stringToLocalDateTime")
    default LocalDateTime stringToLocalDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeString, formatter);
    }

    @Named("eventStatusToString")
    default String eventStatusToString(Event.EventStatus status) {
        return status != null ? status.name() : null;
    }
}