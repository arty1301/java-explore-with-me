package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.EventCollectionDto;
import ru.practicum.ewm.dto.CreateCollectionRequest;
import ru.practicum.ewm.model.EventCollection;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface EventCollectionMapper {

    @Mapping(source = "events", target = "events")
    EventCollectionDto convertToDto(EventCollection collection);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    EventCollection convertToEntity(CreateCollectionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    EventCollection convertToEntity(EventCollectionDto dto);

    default EventCollection updateEntityFromRequest(ru.practicum.ewm.dto.UpdateCollectionRequest request, EventCollection entity) {
        if (request == null) {
            return entity;
        }

        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            entity.setPinned(request.getPinned());
        }

        return entity;
    }
}