package ru.practicum.common.errors;

/**
 * 403 Forbidden семантика.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}

