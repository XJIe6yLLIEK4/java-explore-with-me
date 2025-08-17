package ru.practicum.statsserver.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ErrorRespone {
    private final String error;
}

