package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto createCompilation(NewCompilationDto newCompilationDto);
    CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest updateRequest);
    void deleteCompilation(Long compilationId);
    List<CompilationDto> getCompilationsFiltered(int from, int size, Boolean pinned);
    CompilationDto getCompilationById(Long compilationId);
}