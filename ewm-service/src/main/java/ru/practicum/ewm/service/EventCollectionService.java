package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;

import java.util.List;

public interface EventCollectionService {
    CompilationDto createCollection(NewCompilationDto request);

    CompilationDto updateCollection(Long collectionId, UpdateCompilationRequest request);

    void removeCollection(Long collectionId);

    List<CompilationDto> getCollections(int startingFrom, int pageSize, Boolean pinned);

    CompilationDto getCollectionById(Long collectionId);
}