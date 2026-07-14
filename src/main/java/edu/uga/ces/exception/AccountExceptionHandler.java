package edu.uga.ces.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Error mappings owned by Allen's account/profile endpoints. */
@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationRequired(AuthenticationRequiredException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "AUTHENTICATION_REQUIRED", "message", ex.getMessage()));
    }

    @ExceptionHandler(AccountOperationException.class)
    public ResponseEntity<Map<String, String>> handleAccountOperation(AccountOperationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "ACCOUNT_OPERATION_FAILED", "message", ex.getMessage()));
    }
}
