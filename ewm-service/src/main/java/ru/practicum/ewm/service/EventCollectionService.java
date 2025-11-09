package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.EventCollectionDto;
import ru.practicum.ewm.dto.CreateCollectionRequest;
import ru.practicum.ewm.dto.UpdateCollectionRequest;

import java.util.List;

public interface EventCollectionService {
    EventCollectionDto createCollection(CreateCollectionRequest request);
    EventCollectionDto updateCollection(Long collectionId, UpdateCollectionRequest request);
    void removeCollection(Long collectionId);
    List<EventCollectionDto> getCollections(int startingFrom, int pageSize, Boolean pinned);
    EventCollectionDto getCollectionById(Long collectionId);
}