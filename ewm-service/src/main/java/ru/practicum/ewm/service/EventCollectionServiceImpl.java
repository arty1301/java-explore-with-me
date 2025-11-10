package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;
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
    public CompilationDto createCollection(NewCompilationDto request) {
        log.info("Creating new event compilation with title: {}", request.getTitle());

        if (collectionRepository.existsByCollectionTitle(request.getTitle())) {
            throw new ru.practicum.ewm.exception.DataConflictException("Compilation title must be unique: " + request.getTitle());
        }

        Set<Event> compilationEvents = resolveEventSet(request.getEvents());

        EventCollection newCollection = collectionMapper.convertToEntity(request);
        configureCollectionDefaults(newCollection);
        newCollection.setEvents(compilationEvents);

        EventCollection savedCollection = collectionRepository.save(newCollection);
        log.info("Successfully created compilation with ID: {}", savedCollection.getId());

        return collectionMapper.convertToDto(savedCollection);
    }

    @Override
    public CompilationDto updateCollection(Long collectionId, UpdateCompilationRequest request) {
        log.info("Updating event compilation with ID: {}", collectionId);

        EventCollection existingCollection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Event compilation not found with ID: " + collectionId));

        if (request.getTitle() != null && !request.getTitle().equals(existingCollection.getTitle()) &&
                collectionRepository.existsByCollectionTitle(request.getTitle())) {
            throw new ru.practicum.ewm.exception.DataConflictException("Compilation title already exists: " + request.getTitle());
        }

        applyCollectionUpdates(existingCollection, request);

        if (request.getEvents() != null) {
            Set<Event> updatedEvents = resolveEventSet(request.getEvents());
            existingCollection.setEvents(updatedEvents);
        }

        EventCollection updatedCollection = collectionRepository.save(existingCollection);
        log.info("Successfully updated compilation with ID: {}", collectionId);

        return collectionMapper.convertToDto(updatedCollection);
    }

    @Override
    public void removeCollection(Long collectionId) {
        log.info("Removing event compilation with ID: {}", collectionId);

        if (!collectionRepository.existsById(collectionId)) {
            throw new ResourceNotFoundException("Event compilation not found with ID: " + collectionId);
        }

        collectionRepository.deleteById(collectionId);
        log.info("Successfully removed compilation with ID: {}", collectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCollections(int startingFrom, int pageSize, Boolean pinned) {
        log.info("Retrieving compilations - from: {}, size: {}, pinned: {}", startingFrom, pageSize, pinned);

        Pageable pageable = PageRequest.of(startingFrom / pageSize, pageSize);
        List<EventCollection> compilations;

        if (pinned == null) {
            compilations = collectionRepository.findAllCollections(pageable).getContent();
        } else {
            compilations = collectionRepository.findCollectionsByPinnedStatus(pinned, pageable).getContent();
        }

        return compilations.stream()
                .map(collectionMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCollectionById(Long collectionId) {
        log.info("Retrieving compilation by ID: {}", collectionId);

        return collectionRepository.findCollectionWithEvents(collectionId)
                .map(collectionMapper::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Event compilation not found with ID: " + collectionId));
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

    private void applyCollectionUpdates(EventCollection collection, UpdateCompilationRequest request) {
        if (request.getTitle() != null) {
            collection.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            collection.setPinned(request.getPinned());
        }
    }
}