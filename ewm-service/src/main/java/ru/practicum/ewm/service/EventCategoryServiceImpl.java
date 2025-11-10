package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.exception.DataConflictException;
import ru.practicum.ewm.exception.ResourceNotFoundException;
import ru.practicum.ewm.mapper.EventCategoryMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventCategory;
import ru.practicum.ewm.repository.EventCategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventCategoryServiceImpl implements EventCategoryService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final EventCategoryMapper categoryMapper;

    @Override
    public CategoryDto createCategory(NewCategoryDto request) {
        log.info("Creating new category with name: {}", request.getName());

        if (categoryRepository.existsByCategoryName(request.getName())) {
            throw new DataConflictException("Category name must be unique: " + request.getName());
        }

        EventCategory newCategory = categoryMapper.convertToEntity(request);
        EventCategory savedCategory = categoryRepository.save(newCategory);

        log.info("Successfully created category with ID: {}", savedCategory.getId());
        return categoryMapper.convertToDto(savedCategory);
    }

    @Override
    public void removeCategory(Long categoryId) {
        log.info("Attempting to remove category with ID: {}", categoryId);

        EventCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        List<Event> eventsInCategory = eventRepository.findEventsByCategory(categoryId);
        if (!eventsInCategory.isEmpty()) {
            throw new DataConflictException("Cannot delete category with associated events. Category ID: " + categoryId);
        }

        categoryRepository.delete(category);
        log.info("Successfully removed category with ID: {}", categoryId);
    }

    @Override
    public CategoryDto modifyCategory(Long categoryId, CategoryDto categoryDto) {
        log.info("Updating category with ID: {}", categoryId);

        EventCategory existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        if (!existingCategory.getName().equals(categoryDto.getName()) &&
                categoryRepository.existsByCategoryName(categoryDto.getName())) {
            throw new DataConflictException("Category name already exists: " + categoryDto.getName());
        }

        existingCategory.setName(categoryDto.getName());
        EventCategory updatedCategory = categoryRepository.save(existingCategory);

        log.info("Successfully updated category with ID: {}", categoryId);
        return categoryMapper.convertToDto(updatedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> retrieveCategories(int startingFrom, int pageSize) {
        log.info("Retrieving categories starting from {}, page size: {}", startingFrom, pageSize);

        Pageable pageable = PageRequest.of(startingFrom / pageSize, pageSize);
        return categoryRepository.findAllByOrderByIdAsc(pageable)
                .getContent()
                .stream()
                .map(categoryMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto retrieveCategoryById(Long categoryId) {
        log.info("Retrieving category by ID: {}", categoryId);

        return categoryRepository.findCategoryById(categoryId)
                .map(categoryMapper::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
    }
}