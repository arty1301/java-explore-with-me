package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;

import java.util.List;

public interface EventCategoryService {
    CategoryDto createCategory(NewCategoryDto request);

    void removeCategory(Long categoryId);

    CategoryDto modifyCategory(Long categoryId, CategoryDto categoryDto);

    List<CategoryDto> retrieveCategories(int startingFrom, int pageSize);

    CategoryDto retrieveCategoryById(Long categoryId);
}