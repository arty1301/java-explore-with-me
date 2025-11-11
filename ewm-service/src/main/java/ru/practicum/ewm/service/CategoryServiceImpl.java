package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Creating new category: {}", newCategoryDto.getName());

        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Category name must be unique");
        }

        Category category = categoryMapper.toCategoryEntity(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);

        log.info("Category created successfully with ID: {}", savedCategory.getId());
        return categoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    public void removeCategory(Long categoryId) {
        log.info("Removing category with ID: {}", categoryId);

        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category not found");
        }

        if (!eventRepository.findByCategoryId(categoryId).isEmpty()) {
            throw new ConflictException("Cannot delete category with associated events");
        }

        categoryRepository.deleteById(categoryId);
        log.info("Category with ID {} removed successfully", categoryId);
    }

    @Override
    public CategoryDto modifyCategory(Long categoryId, CategoryDto categoryDto) {
        log.info("Updating category with ID: {}", categoryId);

        Category existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (!existingCategory.getName().equals(categoryDto.getName()) &&
                categoryRepository.existsByName(categoryDto.getName())) {
            throw new ConflictException("Category name must be unique");
        }

        Category updatedCategory = categoryMapper.updateCategoryFromDto(categoryDto, existingCategory);
        Category savedCategory = categoryRepository.save(updatedCategory);

        log.info("Category with ID {} updated successfully", categoryId);
        return categoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoriesPaginated(int from, int size) {
        log.info("Retrieving categories from {} with size {}", from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable).stream()
                .map(categoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long categoryId) {
        log.info("Retrieving category with ID: {}", categoryId);

        return categoryRepository.findById(categoryId)
                .map(categoryMapper::toCategoryDto)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }
}