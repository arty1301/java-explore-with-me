package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    void removeCategory(Long categoryId);

    CategoryDto modifyCategory(Long categoryId, CategoryDto categoryDto);

    List<CategoryDto> retrieveCategories(int from, int size);

    CategoryDto retrieveCategoryById(Long categoryId);

    boolean categoryExists(Long categoryId);
}