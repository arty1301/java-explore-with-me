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

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    @Mapping(source = "location", target = "location")
    @Mapping(source = "eventDate", target = "eventDate", qualifiedByName = "formatDateTime")
    @Mapping(source = "createdOn", target = "createdOn", qualifiedByName = "formatDateTime")
    @Mapping(source = "publishedOn", target = "publishedOn", qualifiedByName = "formatDateTime")
    @Mapping(source = "state", target = "state", qualifiedByName = "stateToString")
    EventFullDto toFullDto(Event event);

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    @Mapping(source = "eventDate", target = "eventDate", qualifiedByName = "formatDateTime")
    EventShortDto toShortDto(Event event);

    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(source = "category", target = "category.id")
    @Mapping(source = "eventDate", target = "eventDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    Event fromNewDto(NewEventDto dto);


    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String baseFormat = dateTime.format(formatter);

        return String.format(baseFormat);
    }

    @Named("stateToString")
    default String stateToString(Event.EventState state) {
        return state != null ? state.name() : null;
    }
}