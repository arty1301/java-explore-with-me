package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.ewm.dto.EventDetailedDto;
import ru.practicum.ewm.dto.EventBriefDto;
import ru.practicum.ewm.dto.CreateEventRequest;
import ru.practicum.ewm.model.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", uses = {EventCategoryMapper.class, PlatformUserMapper.class})
public interface EventMapper {

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    @Mapping(source = "location.latitude", target = "location.lat")
    @Mapping(source = "location.longitude", target = "location.lon")
    @Mapping(source = "creationDate", target = "createdOn", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "eventDate", target = "eventDate", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "publicationDate", target = "publishedOn", qualifiedByName = "formatLocalDateTime")
    @Mapping(source = "status", target = "state", qualifiedByName = "eventStatusToString")
    EventDetailedDto convertToDetailedDto(Event event);

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    @Mapping(source = "eventDate", target = "eventDate", qualifiedByName = "formatLocalDateTime")
    EventBriefDto convertToBriefDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "publicationDate", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(source = "eventDate", target = "eventDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(source = "location.lat", target = "location.latitude")
    @Mapping(source = "location.lon", target = "location.longitude")
    Event convertToEntity(CreateEventRequest request);

    @Named("formatLocalDateTime")
    default String formatLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    @Named("eventStatusToString")
    default String eventStatusToString(Event.EventStatus status) {
        return status != null ? status.name() : null;
    }

    default Event updateEntityFromAdminRequest(ru.practicum.ewm.dto.UpdateEventAdminRequest request, Event entity) {
        if (request == null) {
            return entity;
        }

        if (request.getAnnotation() != null) {
            entity.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getPaid() != null) {
            entity.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            entity.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            entity.setRequestModeration(request.getRequestModeration());
        }
        if (request.getEventDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            entity.setEventDate(LocalDateTime.parse(request.getEventDate(), formatter));
        }
        if (request.getLocation() != null) {
            if (entity.getLocation() == null) {
                entity.setLocation(new ru.practicum.ewm.model.EventLocation());
            }
            entity.getLocation().setLatitude(request.getLocation().getLat());
            entity.getLocation().setLongitude(request.getLocation().getLon());
        }

        return entity;
    }

    default Event updateEntityFromUserRequest(ru.practicum.ewm.dto.UpdateEventUserRequest request, Event entity) {
        if (request == null) {
            return entity;
        }

        if (request.getAnnotation() != null) {
            entity.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getPaid() != null) {
            entity.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            entity.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            entity.setRequestModeration(request.getRequestModeration());
        }
        if (request.getEventDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            entity.setEventDate(LocalDateTime.parse(request.getEventDate(), formatter));
        }
        if (request.getLocation() != null) {
            if (entity.getLocation() == null) {
                entity.setLocation(new ru.practicum.ewm.model.EventLocation());
            }
            entity.getLocation().setLatitude(request.getLocation().getLat());
            entity.getLocation().setLongitude(request.getLocation().getLon());
        }

        return entity;
    }
}