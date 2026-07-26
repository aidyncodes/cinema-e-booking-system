package edu.uga.ces.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catches errors thrown anywhere in the app and turns them into the JSON
 * error shape (in our API contract)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(MovieNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(ShowroomNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleShowroomNotFound(ShowroomNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(ShowtimeConflictException.class)
    public ResponseEntity<Map<String, String>> handleShowtimeConflict(ShowtimeConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "SHOWTIME_CONFLICT", "message", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "EMAIL_ALREADY_EXISTS", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_CREDENTIALS", "message", ex.getMessage()));
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<Map<String, String>> handleAccountNotActive(AccountNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCOUNT_NOT_ACTIVE", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_TOKEN", "message", ex.getMessage()));
    }

    @ExceptionHandler(AdminAccessRequiredException.class)
    public ResponseEntity<Map<String, String>> handleAdminAccessRequired(AdminAccessRequiredException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ADMIN_ACCESS_REQUIRED", "message", ex.getMessage()));
    }

    // Triggered by @Valid on request DTOs (RegisterRequest, LoginRequest, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "VALIDATION_FAILED", "fields", fieldErrors));
    }
}
