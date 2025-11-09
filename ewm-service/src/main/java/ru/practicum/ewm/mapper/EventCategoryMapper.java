package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.EventCategoryDto;
import ru.practicum.ewm.dto.CreateCategoryRequest;
import ru.practicum.ewm.model.EventCategory;

@Mapper(componentModel = "spring")
public interface EventCategoryMapper {

    EventCategoryDto convertToDto(EventCategory category);

    @Mapping(target = "id", ignore = true)
    EventCategory convertToEntity(CreateCategoryRequest request);

    @Mapping(target = "id", ignore = true)
    EventCategory convertToEntity(EventCategoryDto dto);

    default EventCategory updateEntityFromDto(EventCategoryDto dto, EventCategory entity) {
        if (dto == null) {
            return entity;
        }

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }

        return entity;
    }
}