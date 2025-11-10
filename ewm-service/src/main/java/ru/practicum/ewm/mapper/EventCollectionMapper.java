package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.model.EventCollection;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface EventCollectionMapper {

    @Mapping(source = "events", target = "events")
    CompilationDto convertToDto(EventCollection collection);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    EventCollection convertToEntity(NewCompilationDto request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    EventCollection convertToEntity(CompilationDto dto);

    default EventCollection updateEntityFromRequest(ru.practicum.ewm.dto.UpdateCompilationRequest request, EventCollection entity) {
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