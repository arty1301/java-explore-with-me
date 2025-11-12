package ru.practicum.ewm.controller.pub;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<CompilationDto>> getCompilations(@RequestParam(defaultValue = "0") int from,
                                                                @RequestParam(defaultValue = "10") int size,
                                                                @RequestParam(required = false) Boolean pinned) {
        return ResponseEntity.ok(compilationService.getCompilations(from, size, pinned));
    }

    @GetMapping("/{compId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<CompilationDto> getCompilation(@PathVariable Long compId) {
        return ResponseEntity.ok(compilationService.getCompilation(compId));
    }
}