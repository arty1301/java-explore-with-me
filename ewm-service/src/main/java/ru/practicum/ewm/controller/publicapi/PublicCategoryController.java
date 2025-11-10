package ru.practicum.ewm.controller.publicapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.service.EventCategoryService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final EventCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Public: Retrieving categories from: {}, size: {}", from, size);
        List<CategoryDto> categories = categoryService.retrieveCategories(from, size);
        log.info("Public: Retrieved {} categories", categories.size());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{catId}")
    public CategoryDto getCategory(@PathVariable Long catId) {
        log.info("Public: Retrieving category by ID: {}", catId);
        CategoryDto category = categoryService.retrieveCategoryById(catId);
        log.info("Public: Retrieved category: {}", category.getName());
        return category;
    }
}