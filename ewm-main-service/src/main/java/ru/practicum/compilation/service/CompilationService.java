package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.errors.ConflictException;
import ru.practicum.common.errors.NotFoundException;
import ru.practicum.common.util.PageRequestUtil;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper mapper;

    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new ConflictException("Compilation title must not be blank.");
        }
        Compilation c = new Compilation();
        c.setTitle(dto.getTitle());
        c.setPinned(dto.getPinned() != null ? dto.getPinned() : Boolean.FALSE);
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
            if (events.size() != new HashSet<>(dto.getEvents()).size()) {
                throw new NotFoundException("One or more events not found for compilation");
            }
            c.setEvents(events);
        }
        return mapper.toDto(compilationRepository.save(c));
    }

    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        Compilation c = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new ConflictException("Compilation title must not be blank.");
            }
            c.setTitle(dto.getTitle());
        }
        if (dto.getPinned() != null) {
            c.setPinned(dto.getPinned());
        }
        if (dto.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
            if (events.size() != new HashSet<>(dto.getEvents()).size()) {
                throw new NotFoundException("One or more events not found for compilation");
            }
            c.setEvents(events);
        }
        return mapper.toDto(compilationRepository.save(c));
    }

    @Transactional
    public void delete(Long compId) {
        Compilation c = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        compilationRepository.delete(c);
    }

    @Transactional(readOnly = true)
    public CompilationDto get(Long compId) {
        Compilation c = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        c.getEvents().size();
        return mapper.toDto(c);
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        var pageable = PageRequestUtil.of(from, size, Sort.by("id").ascending());
        if (pinned == null) {
            return compilationRepository.findAll(pageable)
                    .stream().map(mapper::toDto).collect(Collectors.toList());
        } else {
            return compilationRepository.findAllByPinned(pinned, pageable)
                    .stream().map(mapper::toDto).collect(Collectors.toList());
        }
    }
}
