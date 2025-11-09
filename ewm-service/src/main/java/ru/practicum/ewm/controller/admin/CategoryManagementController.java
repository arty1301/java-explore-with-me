package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventCategoryDto;
import ru.practicum.ewm.dto.CreateCategoryRequest;
import ru.practicum.ewm.service.EventCategoryService;

@Slf4j
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryManagementController {

    private final EventCategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventCategoryDto createEventCategory(@Valid @RequestBody CreateCategoryRequest categoryRequest) {
        log.info("Admin: Creating new category with name: {}", categoryRequest.getName());
        EventCategoryDto createdCategory = categoryService.createCategory(categoryRequest);
        log.info("Admin: Successfully created category with ID: {}", createdCategory.getId());
        return createdCategory;
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> removeEventCategory(@PathVariable Long categoryId) {
        log.info("Admin: Removing category with ID: {}", categoryId);
        categoryService.removeCategory(categoryId);
        log.info("Admin: Successfully removed category with ID: {}", categoryId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<EventCategoryDto> updateEventCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody EventCategoryDto categoryUpdate) {

        log.info("Admin: Updating category with ID: {}", categoryId);
        EventCategoryDto updatedCategory = categoryService.modifyCategory(categoryId, categoryUpdate);
        log.info("Admin: Successfully updated category with ID: {}", categoryId);
        return ResponseEntity.ok(updatedCategory);
    }
}