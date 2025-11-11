package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.service.CategoryService;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createNewCategory(@Valid @RequestBody NewCategoryDto categoryRequest) {
        return categoryService.createCategory(categoryRequest);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> removeCategory(@PathVariable("categoryId") Long id) {
        categoryService.removeCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{categoryId}")
    public CategoryDto modifyCategory(@PathVariable("categoryId") Long id,
                                      @Valid @RequestBody CategoryDto categoryUpdate) {
        return categoryService.modifyCategory(id, categoryUpdate);
    }
}