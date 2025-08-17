package ru.practicum.statsdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ViewStatsDto {
    private Long hits;
    private String app;
    private String uri;
}
