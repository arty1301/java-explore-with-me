package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.EventCollectionDto;
import ru.practicum.ewm.dto.CreateCollectionRequest;
import ru.practicum.ewm.dto.UpdateCollectionRequest;
import ru.practicum.ewm.exception.ResourceNotFoundException;
import ru.practicum.ewm.mapper.EventCollectionMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventCollection;
import ru.practicum.ewm.repository.EventCollectionRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventCollectionServiceImpl implements EventCollectionService {

    private final EventCollectionRepository collectionRepository;
    private final EventRepository eventRepository;
    private final EventCollectionMapper collectionMapper;

    @Override
    public EventCollectionDto createCollection(CreateCollectionRequest request) {
        log.info("Creating new event collection with title: {}", request.getTitle());

        Set<Event> collectionEvents = resolveEventSet(request.getEvents());

        EventCollection newCollection = collectionMapper.convertToEntity(request);
        configureCollectionDefaults(newCollection);
        newCollection.setEvents(collectionEvents);

        EventCollection savedCollection = collectionRepository.save(newCollection);
        log.info("Successfully created collection with ID: {}", savedCollection.getId());

        return collectionMapper.convertToDto(savedCollection);
    }

    @Override
    public EventCollectionDto updateCollection(Long collectionId, UpdateCollectionRequest request) {
        log.info("Updating event collection with ID: {}", collectionId);

        EventCollection existingCollection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Event collection not found with ID: " + collectionId));

        applyCollectionUpdates(existingCollection, request);

        if (request.getEvents() != null) {
            Set<Event> updatedEvents = resolveEventSet(request.getEvents());
            existingCollection.setEvents(updatedEvents);
        }

        EventCollection updatedCollection = collectionRepository.save(existingCollection);
        log.info("Successfully updated collection with ID: {}", collectionId);

        return collectionMapper.convertToDto(updatedCollection);
    }

    @Override
    public void removeCollection(Long collectionId) {
        log.info("Removing event collection with ID: {}", collectionId);

        if (!collectionRepository.existsById(collectionId)) {
            throw new ResourceNotFoundException("Event collection not found with ID: " + collectionId);
        }

        collectionRepository.deleteById(collectionId);
        log.info("Successfully removed collection with ID: {}", collectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventCollectionDto> getCollections(int startingFrom, int pageSize, Boolean pinned) {
        log.info("Retrieving collections - from: {}, size: {}, pinned: {}", startingFrom, pageSize, pinned);

        Pageable pageable = PageRequest.of(startingFrom / pageSize, pageSize);
        List<EventCollection> collections;

        if (pinned == null) {
            collections = collectionRepository.findAllCollections(pageable).getContent();
        } else {
            collections = collectionRepository.findCollectionsByPinnedStatus(pinned, pageable).getContent();
        }

        return collections.stream()
                .map(collectionMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventCollectionDto getCollectionById(Long collectionId) {
        log.info("Retrieving collection by ID: {}", collectionId);

        return collectionRepository.findCollectionWithEvents(collectionId)
                .map(collectionMapper::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Event collection not found with ID: " + collectionId));
    }

    private Set<Event> resolveEventSet(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }

        return eventIds.stream()
                .map(eventId -> eventRepository.findById(eventId)
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId)))
                .collect(Collectors.toSet());
    }

    private void configureCollectionDefaults(EventCollection collection) {
        if (collection.getPinned() == null) {
            collection.setPinned(false);
        }
    }

    private void applyCollectionUpdates(EventCollection collection, UpdateCollectionRequest request) {
        if (request.getTitle() != null) {
            collection.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            collection.setPinned(request.getPinned());
        }
    }
}