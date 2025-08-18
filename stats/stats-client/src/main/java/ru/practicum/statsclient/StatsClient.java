package ru.practicum.statsclient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;

public class StatsClient {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestTemplate rt;

    public StatsClient(RestTemplate rt) {
        this.rt = rt;
    }

    public void hit(EndpointHitDto dto) {
        rt.postForEntity("/hit", dto, Void.class);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", start.format(FMT))
                .queryParam("end", end.format(FMT))
                .queryParam("unique", unique);

        if (!CollectionUtils.isEmpty(uris)) {
            for (String u : uris) b.queryParam("uris", u);
        }

        ResponseEntity<ViewStatsDto[]> resp = rt.getForEntity(b.toUriString(), ViewStatsDto[].class);
        ViewStatsDto[] arr = resp.getBody();
        return arr == null ? Collections.emptyList() : Arrays.asList(arr);
    }
}
