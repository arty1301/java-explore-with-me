package ru.practicum.ewm.controller.pub;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.service.CompilationService;

import java.util.List;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
public class CompilationPublicController {
    private final CompilationService compilationService;

    @GetMapping
    public ResponseEntity<List<CompilationDto>> getCompilations(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean pinned) {
        List<CompilationDto> compilations = compilationService.retrieveCompilations(from, size, pinned);
        return ResponseEntity.ok(compilations);
    }

    @GetMapping("/{compilationId}")
    public CompilationDto getCompilationById(@PathVariable("compilationId") Long id) {
        return compilationService.retrieveCompilationById(id);
    }
}