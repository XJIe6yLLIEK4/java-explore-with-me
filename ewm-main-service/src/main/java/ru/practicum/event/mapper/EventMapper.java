package ru.practicum.event.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.Location;
import ru.practicum.event.model.Event;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.mapper.UserMapper;

@Component
public class EventMapper {

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public EventMapper(CategoryMapper categoryMapper, UserMapper userMapper) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    public EventFullDto toFullDto(Event e, long views, long confirmed) {
        if (e == null) return null;
        CategoryDto cat = e.getCategory() == null ? null : categoryMapper.toDto(e.getCategory());
        UserShortDto initiator = e.getInitiator() == null ? null : userMapper.toShort(e.getInitiator());
        Location loc = new Location(e.getLocationLat(), e.getLocationLon());
        return EventFullDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .annotation(e.getAnnotation())
                .description(e.getDescription())
                .category(cat)
                .initiator(initiator)
                .eventDate(e.getEventDate())
                .createdOn(e.getCreatedOn())
                .publishedOn(e.getPublishedOn())
                .paid(e.getPaid())
                .participantLimit(e.getParticipantLimit())
                .requestModeration(e.getRequestModeration())
                .location(loc)
                .views(views)
                .confirmedRequests(confirmed)
                .state(e.getState() == null ? null : e.getState().name())
                .build();
    }

    public EventShortDto toShortDto(Event e, long views, long confirmed) {
        if (e == null) return null;
        CategoryDto cat = e.getCategory() == null ? null : categoryMapper.toDto(e.getCategory());
        UserShortDto initiator = e.getInitiator() == null ? null : userMapper.toShort(e.getInitiator());
        return EventShortDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .annotation(e.getAnnotation())
                .category(cat)
                .initiator(initiator)
                .eventDate(e.getEventDate())
                .paid(e.getPaid())
                .views(views)
                .confirmedRequests(confirmed)
                .build();
    }
}
