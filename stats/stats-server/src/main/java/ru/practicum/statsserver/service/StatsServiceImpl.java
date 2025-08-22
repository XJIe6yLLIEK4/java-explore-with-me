package ru.practicum.statsserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;
import ru.practicum.statsserver.mapper.HitMapper;
import ru.practicum.statsserver.model.Hit;
import ru.practicum.statsserver.model.ViewStatsRow;
import ru.practicum.statsserver.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final HitRepository hitRepository;
    private final HitMapper hitMapper;

    @Override
    public EndpointHitDto createHit(EndpointHitDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("EndpointHitDto must not be null");
        }
        Hit hit = hitMapper.toHit(dto);
        Hit saved = hitRepository.save(hit);
        return hitMapper.toDto(saved);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (start == null && end != null) {
            throw new IllegalArgumentException("Start date must be provided if end date is specified");
        }
        if (end == null && start != null) {
            throw new IllegalArgumentException("End date must be provided if start date is specified");
        }
        start = start != null ? start : LocalDateTime.of(2000, 1, 1, 0, 0);
        end = end != null ? end : LocalDateTime.now();
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        List<ViewStatsRow> statsRows = unique
                ? ((uris != null) ? hitRepository.findUniqueStats(start, end, uris) : hitRepository.findUniqueAllUriStats(start, end))
                : ((uris != null) ? hitRepository.findStats(start, end, uris) : hitRepository.findAllUriStats(start, end));

        return statsRows.stream().map(row -> ViewStatsDto.builder()
                        .app(row.getApp())
                        .uri(row.getUri())
                        .hits(row.getHits())
                        .build())
                .toList();
    }
}
