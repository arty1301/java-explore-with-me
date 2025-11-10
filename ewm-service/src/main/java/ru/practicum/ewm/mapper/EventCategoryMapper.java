package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.model.EventCategory;

@Mapper(componentModel = "spring")
public interface EventCategoryMapper {

    CategoryDto convertToDto(EventCategory category);

    @Mapping(target = "id", ignore = true)
    EventCategory convertToEntity(NewCategoryDto request);

    @Mapping(target = "id", ignore = true)
    EventCategory convertToEntity(CategoryDto dto);

    default EventCategory updateEntityFromDto(CategoryDto dto, EventCategory entity) {
        if (dto == null) {
            return entity;
        }

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }

        return entity;
    }
}