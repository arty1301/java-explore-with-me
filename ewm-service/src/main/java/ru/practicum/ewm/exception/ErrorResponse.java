package ru.practicum.ewm.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ErrorResponse {
    private final String error;

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message);
    }
}