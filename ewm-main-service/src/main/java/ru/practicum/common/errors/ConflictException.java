package ru.practicum.common.errors;

/**
 * 409 Conflict семантика.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

