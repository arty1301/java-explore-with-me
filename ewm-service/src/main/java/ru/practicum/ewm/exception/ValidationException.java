package ru.practicum.ewm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String field, String constraint) {
        super(String.format("Validation failed for field '%s': %s", field, constraint));
    }

    public ValidationException(String field, Object value, String constraint) {
        super(String.format("Validation failed for field '%s' with value '%s': %s", field, value, constraint));
    }
}