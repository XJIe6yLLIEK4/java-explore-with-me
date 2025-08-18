package ru.practicum.statsserver.model;

public interface ViewStatsRow {
    String getApp();

    String getUri();

    Long getHits();
}

