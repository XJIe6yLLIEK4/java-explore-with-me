package ru.practicum.statsserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;
import ru.practicum.statsserver.formatter.MyDataTimeFormatter;
import ru.practicum.statsserver.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping
public class StatsController {

    private static final String DATE_TIME_PATTERN = MyDataTimeFormatter.DATE_TIME_FORMAT;

    private final StatsService statsService;

    @PostMapping(path = "/hit", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody @Valid EndpointHitDto dto) {
        statsService.createHit(dto);
    }

    @GetMapping(path = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ViewStatsDto> getStats(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = DATE_TIME_PATTERN)
            LocalDateTime start,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = DATE_TIME_PATTERN)
            LocalDateTime end,

            @RequestParam(required = false)
            List<String> uris,

            @RequestParam(defaultValue = "false")
            boolean unique
    ) {
        return statsService.getStats(start, end, uris, unique);
    }
}
