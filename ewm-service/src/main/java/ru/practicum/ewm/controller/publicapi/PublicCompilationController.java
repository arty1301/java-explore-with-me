package ru.practicum.ewm.controller.publicapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.service.EventCollectionService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
public class PublicCompilationController {

    private final EventCollectionService compilationService;

    @GetMapping
    public ResponseEntity<List<CompilationDto>> getCompilations(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean pinned) {

        log.info("Public: Retrieving compilations - from: {}, size: {}, pinned: {}", from, size, pinned);
        List<CompilationDto> compilations = compilationService.getCollections(from, size, pinned);
        log.info("Public: Retrieved {} compilations", compilations.size());
        return ResponseEntity.ok(compilations);
    }

    @GetMapping("/{compId}")
    public CompilationDto getCompilation(@PathVariable Long compId) {
        log.info("Public: Retrieving compilation by ID: {}", compId);
        CompilationDto compilation = compilationService.getCollectionById(compId);
        log.info("Public: Retrieved compilation: {}", compilation.getTitle());
        return compilation;
    }
}