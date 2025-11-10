package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.service.EventCategoryService;

@Slf4j
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryManagementController {

    private final EventCategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto addCategory(@Valid @RequestBody NewCategoryDto categoryRequest) {
        log.info("Admin: Creating new category with name: {}", categoryRequest.getName());
        CategoryDto createdCategory = categoryService.createCategory(categoryRequest);
        log.info("Admin: Successfully created category with ID: {}", createdCategory.getId());
        return createdCategory;
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteCategory(@PathVariable Long catId) {
        log.info("Admin: Removing category with ID: {}", catId);
        categoryService.removeCategory(catId);
        log.info("Admin: Successfully removed category with ID: {}", catId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{catId}")
    public CategoryDto updateCategory(
            @PathVariable Long catId,
            @Valid @RequestBody CategoryDto categoryUpdate) {

        log.info("Admin: Updating category with ID: {}", catId);
        CategoryDto updatedCategory = categoryService.modifyCategory(catId, categoryUpdate);
        log.info("Admin: Successfully updated category with ID: {}", catId);
        return updatedCategory;
    }
}