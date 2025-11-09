package ru.practicum.ewm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String action, String resource) {
        super(String.format("Access denied for %s on %s", action, resource));
    }

    public AccessDeniedException(Long userId, String resource) {
        super(String.format("User ID %d is not authorized to access %s", userId, resource));
    }
}