package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;
import ru.practicum.ewm.service.EventCollectionService;

@Slf4j
@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class CompilationManagementController {

    private final EventCollectionService compilationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto saveCompilation(@Valid @RequestBody NewCompilationDto compilationRequest) {
        log.info("Admin: Creating new compilation with title: {}", compilationRequest.getTitle());
        CompilationDto createdCompilation = compilationService.createCollection(compilationRequest);
        log.info("Admin: Successfully created compilation with ID: {}", createdCompilation.getId());
        return createdCompilation;
    }

    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(
            @PathVariable Long compId,
            @Valid @RequestBody UpdateCompilationRequest updateRequest) {

        log.info("Admin: Updating compilation with ID: {}", compId);
        CompilationDto updatedCompilation = compilationService.updateCollection(compId, updateRequest);
        log.info("Admin: Successfully updated compilation with ID: {}", compId);
        return updatedCompilation;
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteCompilation(@PathVariable Long compId) {
        log.info("Admin: Removing compilation with ID: {}", compId);
        compilationService.removeCollection(compId);
        log.info("Admin: Successfully removed compilation with ID: {}", compId);
        return ResponseEntity.noContent().build();
    }
}