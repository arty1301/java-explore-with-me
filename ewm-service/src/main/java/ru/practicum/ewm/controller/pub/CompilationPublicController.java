package ru.practicum.ewm.controller.pub;

import lombok.RequiredArgsConstructor;
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
    public List<CompilationDto> getCompilations(@RequestParam(defaultValue = "0") int from,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) Boolean pinned) {
        return compilationService.getCompilationsFiltered(from, size, pinned);
    }

    @GetMapping("/{compilationId}")
    public CompilationDto getCompilation(@PathVariable Long compilationId) {
        return compilationService.getCompilationById(compilationId);
    }
}