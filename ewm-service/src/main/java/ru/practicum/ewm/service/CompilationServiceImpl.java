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
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    public CompilationDto createCompilation(NewCompilationDto compilationDto) {
        log.info("Creating new compilation with title: {}", compilationDto.getTitle());

        Set<Event> events = resolveEvents(compilationDto.getEvents());

        Compilation compilation = compilationMapper.toCompilationEntity(compilationDto);
        compilation.setEvents(events);

        if (compilation.getPinned() == null) {
            compilation.setPinned(false);
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Compilation created successfully with ID: {}", savedCompilation.getId());

        return compilationMapper.toCompilationDto(savedCompilation);
    }

    @Override
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest updateRequest) {
        log.info("Updating compilation with ID: {}", compilationId);

        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getEvents() != null) {
            Set<Event> events = resolveEvents(updateRequest.getEvents());
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Compilation with ID: {} updated successfully", compilationId);

        return compilationMapper.toCompilationDto(updatedCompilation);
    }

    @Override
    public void deleteCompilation(Long compilationId) {
        log.info("Deleting compilation with ID: {}", compilationId);

        if (!compilationRepository.existsById(compilationId)) {
            throw new NotFoundException("Compilation not found");
        }

        compilationRepository.deleteById(compilationId);
        log.info("Compilation with ID: {} deleted successfully", compilationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> retrieveCompilations(int from, int size, Boolean pinned) {
        log.info("Retrieving compilations - from: {}, size: {}, pinned: {}", from, size, pinned);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations;

        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable).getContent();
        } else {
            compilations = compilationRepository.findCompilationsByPinnedStatus(pinned, pageable).getContent();
        }

        return compilations.stream()
                .map(compilationMapper::toCompilationDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto retrieveCompilationById(Long compilationId) {
        log.info("Retrieving compilation with ID: {}", compilationId);

        return compilationRepository.findById(compilationId)
                .map(compilationMapper::toCompilationDto)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));
    }

    private Set<Event> resolveEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }

        return eventIds.stream()
                .map(id -> eventRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id)))
                .collect(Collectors.toSet());
    }
}