package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.*;
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

@Service
@RequiredArgsConstructor
@Transactional
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    public CompilationDto addCompilation(NewCompilationDto dto) {
        Set<Event> events = new HashSet<>();
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            events = dto.getEvents().stream()
                    .map(id -> eventRepository.findById(id).orElseThrow(() ->
                            new NotFoundException("Событие не найдено")))
                    .collect(Collectors.toSet());
        }

        Compilation compilation = compilationMapper.fromNewDto(dto);

        if (compilation.getPinned() == null) {
            compilation.setPinned(false);
        }

        compilation.setEvents(events);
        Compilation saved = compilationRepository.save(compilation);
        return compilationMapper.toDto(saved);
    }

    @Override
    public CompilationDto updateCompilation(Long id, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Подборка не найдена"));

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
        if (dto.getEvents() != null) {
            Set<Event> events = dto.getEvents().stream()
                    .map(eid -> eventRepository.findById(eid).orElseThrow(() ->
                            new NotFoundException("Событие не найдено")))
                    .collect(Collectors.toSet());
            compilation.setEvents(events);
        }

        Compilation updated = compilationRepository.save(compilation);
        return compilationMapper.toDto(updated);
    }

    @Override
    public void deleteCompilation(Long id) {
        if (!compilationRepository.existsById(id)) {
            throw new NotFoundException("Подборка не найдена");
        }
        compilationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(int from, int size, Boolean pinned) {
        Pageable pageable = PageRequest.of(from / size, size);

        List<Compilation> compilations;
        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable).getContent();
        } else {
            compilations = compilationRepository.findByPinned(pinned, pageable);
        }

        return compilations.stream()
                .map(compilationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilation(Long id) {
        return compilationRepository.findById(id)
                .map(compilationMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Подборка не найдена"));
    }
}