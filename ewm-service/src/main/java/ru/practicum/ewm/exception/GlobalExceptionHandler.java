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
import ru.practicum.ewm.dto.ApiError;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return new ApiError(
                Collections.emptyList(),
                ex.getMessage(),
                "The required object was not found.",
                "NOT_FOUND",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(DataConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataConflict(DataConflictException ex) {
        log.warn("Data conflict: {}", ex.getMessage());
        return new ApiError(
                Collections.emptyList(),
                ex.getMessage(),
                "For the requested operation the conditions are not met.",
                "CONFLICT",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return new ApiError(
                Collections.emptyList(),
                ex.getMessage(),
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return new ApiError(
                Collections.emptyList(),
                ex.getMessage(),
                "For the requested operation the conditions are not met.",
                "FORBIDDEN",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Service unavailable: {}", ex.getMessage());
        return new ApiError(
                Collections.emptyList(),
                ex.getMessage(),
                "Service unavailable",
                "SERVICE_UNAVAILABLE",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field '%s': %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        String errorMessage = errors.isEmpty() ? "Validation failed" : errors.get(0);

        log.warn("Method argument validation failed: {}", errorMessage);
        return new ApiError(
                errors,
                errorMessage,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> String.format("Parameter '%s': %s",
                        violation.getPropertyPath(), violation.getMessage()))
                .collect(Collectors.toList());

        String errorMessage = errors.isEmpty() ? "Constraint violation" : errors.get(0);

        log.warn("Constraint violation: {}", errorMessage);
        return new ApiError(
                errors,
                errorMessage,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String parameterName = ex.getParameter().getParameterName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String actualValue = ex.getValue() != null ? ex.getValue().toString() : "null";

        String errorMessage = String.format(
                "Failed to convert parameter '%s' with value '%s' to required type '%s'",
                parameterName, actualValue, requiredType
        );

        log.warn("Type mismatch: {}", errorMessage);
        return new ApiError(
                Collections.emptyList(),
                errorMessage,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        String errorMessage = String.format("Required request parameter '%s' is not present", ex.getParameterName());

        log.warn("Missing request parameter: {}", errorMessage);
        return new ApiError(
                Collections.emptyList(),
                errorMessage,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return new ApiError(
                Collections.emptyList(),
                ex.getMessage(),
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("HTTP message not readable: {}", ex.getMessage());

        String message = "Malformed JSON request";
        if (ex.getMessage() != null && ex.getMessage().contains("java.time.LocalDateTime")) {
            message = "Invalid date format. Expected: yyyy-MM-dd HH:mm:ss";
        }

        return new ApiError(
                Collections.emptyList(),
                message,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ApiError apiError = new ApiError(
                Collections.emptyList(),
                "An unexpected error occurred. Please try again later.",
                "Internal Server Error",
                "INTERNAL_SERVER_ERROR",
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());

        String message = "Integrity constraint violation";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("uq_category_name")) {
                message = "Category name already exists";
            } else if (ex.getMessage().contains("uq_email")) {
                message = "Email already exists";
            } else if (ex.getMessage().contains("uq_compilation_name")) {
                message = "Compilation title already exists";
            } else if (ex.getMessage().contains("uq_request")) {
                message = "Participation request already exists";
            }
        }

        return new ApiError(
                Collections.emptyList(),
                message,
                "Integrity constraint has been violated.",
                "CONFLICT",
                LocalDateTime.now()
        );
    }
}