package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.EventCategoryDto;
import ru.practicum.ewm.dto.CreateCategoryRequest;

import java.util.List;

public interface EventCategoryService {
    EventCategoryDto createCategory(CreateCategoryRequest request);

    void removeCategory(Long categoryId);

    EventCategoryDto modifyCategory(Long categoryId, EventCategoryDto categoryDto);

    List<EventCategoryDto> retrieveCategories(int startingFrom, int pageSize);

    EventCategoryDto retrieveCategoryById(Long categoryId);
}