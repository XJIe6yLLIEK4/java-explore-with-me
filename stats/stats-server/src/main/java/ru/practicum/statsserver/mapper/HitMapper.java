package ru.practicum.statsserver.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsserver.model.Hit;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface HitMapper {

    @Mapping(target = "id", ignore = true)
    Hit toHit(EndpointHitDto dto);

    EndpointHitDto toDto(Hit hit);
}
