package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventCollectionDto;
import ru.practicum.ewm.dto.CreateCollectionRequest;
import ru.practicum.ewm.dto.UpdateCollectionRequest;
import ru.practicum.ewm.service.EventCollectionService;

@Slf4j
@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class CollectionManagementController {

    private final EventCollectionService collectionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventCollectionDto createEventCollection(@Valid @RequestBody CreateCollectionRequest collectionRequest) {
        log.info("Admin: Creating new event collection with title: {}", collectionRequest.getTitle());
        EventCollectionDto createdCollection = collectionService.createCollection(collectionRequest);
        log.info("Admin: Successfully created collection with ID: {}", createdCollection.getId());
        return createdCollection;
    }

    @PatchMapping("/{collectionId}")
    public ResponseEntity<EventCollectionDto> updateEventCollection(
            @PathVariable Long collectionId,
            @Valid @RequestBody UpdateCollectionRequest updateRequest) {

        log.info("Admin: Updating collection with ID: {}", collectionId);
        EventCollectionDto updatedCollection = collectionService.updateCollection(collectionId, updateRequest);
        log.info("Admin: Successfully updated collection with ID: {}", collectionId);
        return ResponseEntity.ok(updatedCollection);
    }

    @DeleteMapping("/{collectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> removeEventCollection(@PathVariable Long collectionId) {
        log.info("Admin: Removing collection with ID: {}", collectionId);
        collectionService.removeCollection(collectionId);
        log.info("Admin: Successfully removed collection with ID: {}", collectionId);
        return ResponseEntity.noContent().build();
    }
}