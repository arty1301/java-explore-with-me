package ru.practicum.ewm.controller.publicapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventCategoryDto;
import ru.practicum.ewm.service.EventCategoryService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final EventCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<EventCategoryDto>> retrieveAllCategories(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Public: Retrieving categories from: {}, size: {}", from, size);
        List<EventCategoryDto> categories = categoryService.retrieveCategories(from, size);
        log.info("Public: Retrieved {} categories", categories.size());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<EventCategoryDto> retrieveCategoryById(@PathVariable Long categoryId) {
        log.info("Public: Retrieving category by ID: {}", categoryId);
        EventCategoryDto category = categoryService.retrieveCategoryById(categoryId);
        log.info("Public: Retrieved category: {}", category.getName());
        return ResponseEntity.ok(category);
    }
}