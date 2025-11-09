package ru.practicum.ewm.controller.publicapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventCollectionDto;
import ru.practicum.ewm.service.EventCollectionService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
public class PublicCollectionController {

    private final EventCollectionService collectionService;

    @GetMapping
    public ResponseEntity<List<EventCollectionDto>> retrieveAllCollections(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean pinned) {

        log.info("Public: Retrieving collections - from: {}, size: {}, pinned: {}", from, size, pinned);
        List<EventCollectionDto> collections = collectionService.getCollections(from, size, pinned);
        log.info("Public: Retrieved {} collections", collections.size());
        return ResponseEntity.ok(collections);
    }

    @GetMapping("/{collectionId}")
    public ResponseEntity<EventCollectionDto> retrieveCollectionById(@PathVariable Long collectionId) {
        log.info("Public: Retrieving collection by ID: {}", collectionId);
        EventCollectionDto collection = collectionService.getCollectionById(collectionId);
        log.info("Public: Retrieved collection: {}", collection.getTitle());
        return ResponseEntity.ok(collection);
    }
}