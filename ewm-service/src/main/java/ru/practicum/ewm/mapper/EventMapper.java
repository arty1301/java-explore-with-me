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

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class})
public interface EventMapper {

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    @Mapping(source = "eventDate", target = "eventDate", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "createdOn", target = "createdOn", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "publishedOn", target = "publishedOn", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "state", target = "state", qualifiedByName = "stateToString")
    EventFullDto toEventFullDto(Event event);

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    @Mapping(source = "eventDate", target = "eventDate", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "createdOn", target = "createdOn", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "publishedOn", target = "publishedOn", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "state", target = "state", qualifiedByName = "stateToString")
    @Mapping(source = "location", target = "location")
    EventShortDto toEventShortDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(source = "category", target = "category.id")
    @Mapping(source = "eventDate", target = "eventDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    Event toEventEntity(NewEventDto newEventDto);

    @Named("formatLocalDateTime")
    default String formatLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    @Named("stateToString")
    default String stateToString(Event.EventState state) {
        return state != null ? state.name() : null;
    }
}