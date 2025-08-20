package ru.practicum.compilation.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Event;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.user.dto.UserShortDto;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CompilationMapper {

    public CompilationDto toDto(Compilation entity) {
        if (entity == null) return null;
        List<EventShortDto> events = entity.getEvents() == null ? List.of() :
                entity.getEvents().stream().map(this::toShort).collect(Collectors.toList());
        return CompilationDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .pinned(entity.getPinned())
                .events(events)
                .build();
    }

    private EventShortDto toShort(Event e) {
        CategoryDto cat = e.getCategory() == null ? null :
                new CategoryDto(e.getCategory().getId(), e.getCategory().getName());
        UserShortDto initiator = e.getInitiator() == null ? null :
                new UserShortDto(e.getInitiator().getId(), e.getInitiator().getName());
        return EventShortDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .annotation(e.getAnnotation())
                .category(cat)
                .initiator(initiator)
                .eventDate(e.getEventDate())
                .paid(e.getPaid())
                .build();
    }
}
