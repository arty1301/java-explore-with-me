package ru.practicum.ewm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return new ErrorResponse(
                Collections.emptyList(),
                ex.getMessage(),
                "The required object was not found.",
                "NOT_FOUND",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(DataConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataConflict(DataConflictException ex) {
        log.warn("Data conflict: {}", ex.getMessage());
        return new ErrorResponse(
                Collections.emptyList(),
                ex.getMessage(),
                "For the requested operation the conditions are not met.",
                "CONFLICT",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return new ErrorResponse(
                Collections.emptyList(),
                ex.getMessage(),
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return new ErrorResponse(
                Collections.emptyList(),
                ex.getMessage(),
                "For the requested operation the conditions are not met.",
                "FORBIDDEN",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Service unavailable: {}", ex.getMessage());
        return new ErrorResponse(
                Collections.emptyList(),
                ex.getMessage(),
                "Service unavailable",
                "SERVICE_UNAVAILABLE",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field '%s': %s", error.getField(), error.getDefaultMessage()))
                .findFirst()
                .orElse("Validation failed");

        log.warn("Method argument validation failed: {}", errorMessage);
        return new ErrorResponse(
                Collections.emptyList(),
                errorMessage,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> String.format("Parameter '%s': %s",
                        violation.getPropertyPath(), violation.getMessage()))
                .findFirst()
                .orElse("Constraint violation");

        log.warn("Constraint violation: {}", errorMessage);
        return new ErrorResponse(
                Collections.emptyList(),
                errorMessage,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String parameterName = ex.getParameter().getParameterName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String actualValue = ex.getValue() != null ? ex.getValue().toString() : "null";

        String errorMessage = String.format(
                "Failed to convert value of type java.lang.String to required type %s; nested exception is java.lang.NumberFormatException: For input string: %s",
                requiredType, actualValue
        );

        log.warn("Type mismatch: {}", errorMessage);
        return new ErrorResponse(
                Collections.emptyList(),
                errorMessage,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        String errorMessage = String.format("Failed to convert value of type java.lang.String to required type long; nested exception is java.lang.NumberFormatException: For input string: %s",
                ex.getParameterName());

        log.warn("Missing request parameter: {}", errorMessage);
        return new ErrorResponse(
                Collections.emptyList(),
                errorMessage,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return new ErrorResponse(
                Collections.emptyList(),
                ex.getMessage(),
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("HTTP message not readable: {}", ex.getMessage());
        return new ErrorResponse(
                Collections.emptyList(),
                "Malformed JSON request",
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                Collections.emptyList(),
                "An unexpected error occurred. Please try again later.",
                "Internal Server Error",
                "INTERNAL_SERVER_ERROR",
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return new ErrorResponse(
                Collections.emptyList(),
                "could not execute statement; SQL [n/a]; constraint [uq_category_name]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement",
                "Integrity constraint has been violated.",
                "CONFLICT",
                LocalDateTime.now()
        );
    }

    // Внутренний класс ErrorResponse
    public static class ErrorResponse {
        private List<String> errors;
        private String message;
        private String reason;
        private String status;
        private LocalDateTime timestamp;

        public ErrorResponse(List<String> errors, String message, String reason, String status, LocalDateTime timestamp) {
            this.errors = errors;
            this.message = message;
            this.reason = reason;
            this.status = status;
            this.timestamp = timestamp;
        }

        public ErrorResponse(List<String> errors, String message, String reason, String status) {
            this.errors = errors;
            this.message = message;
            this.reason = reason;
            this.status = status;
            this.timestamp = LocalDateTime.now();
        }

        // Getters and Setters
        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}