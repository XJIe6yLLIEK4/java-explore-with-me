package ru.practicum.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;

@Component
public class ParticipationMapper {

    public ParticipationRequestDto toDto(ParticipationRequest entity) {
        if (entity == null) return null;
        return ParticipationRequestDto.builder()
                .id(entity.getId())
                .event(entity.getEvent().getId())
                .requester(entity.getRequester().getId())
                .status(entity.getStatus().name())
                .created(entity.getCreated())
                .build();
    }
}
