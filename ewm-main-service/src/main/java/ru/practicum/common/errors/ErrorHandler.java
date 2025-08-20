package ru.practicum.common.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.common.dto.ApiError;

import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@RestControllerAdvice
public class ErrorHandler {

    private ResponseEntity<ApiError> build(HttpStatus status, String reason, String message, List<String> errors) {
        ApiError body = ApiError.builder()
                .status(status.name())
                .reason(reason)
                .message(message)
                .errors(errors == null ? Collections.emptyList() : errors)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND,
                "The required object was not found.",
                ex.getMessage(), null);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenOperationException ex) {
        return build(HttpStatus.FORBIDDEN,
                "For the requested operation the conditions are not met.",
                ex.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT,
                "Integrity constraint has been violated.",
                ex.getMessage(), null);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            BindException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        List<String> errors;
        if (ex instanceof MethodArgumentNotValidException manve) {
            errors = manve.getBindingResult()
                    .getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(Collectors.toList());
        } else if (ex instanceof BindException be) {
            errors = be.getBindingResult()
                    .getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(Collectors.toList());
        } else {
            errors = Collections.singletonList(ex.getMessage());
        }
        return build(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                "Validation failed", errors);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiError> handleUnexpected(Throwable ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error.",
                ex.getMessage(), Collections.singletonList(ex.toString()));
    }
}
