package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toCategoryDto(Category category);

    @Mapping(target = "id", ignore = true)
    Category toCategoryEntity(NewCategoryDto newCategoryDto);

    default Category updateCategoryFromDto(CategoryDto categoryDto, Category category) {
        if (categoryDto == null) {
            return category;
        }

        if (categoryDto.getName() != null) {
            category.setName(categoryDto.getName());
        }

        return category;
    }
}