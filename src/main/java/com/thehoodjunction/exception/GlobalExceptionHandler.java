package com.thehoodjunction.exception;

import com.google.firebase.auth.FirebaseAuthException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        
        logError(errorId, "INTERNAL_SERVER_ERROR", ex, path);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(ex.getMessage())
                .path(path)
                .errorId(errorId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(FirebaseAuthException.class)
    public ResponseEntity<ErrorResponse> handleFirebaseAuthException(FirebaseAuthException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        
        logError(errorId, "FIREBASE_AUTH_ERROR", ex, path);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Firebase Authentication Error")
                .message(ex.getMessage())
                .path(path)
                .errorId(errorId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        
        logError(errorId, "AUTHENTICATION_ERROR", ex, path);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Error")
                .message("Invalid credentials")
                .path(path)
                .errorId(errorId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        
        logError(errorId, "ACCESS_DENIED", ex, path);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("You don't have permission to access this resource")
                .path(path)
                .errorId(errorId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        logError(errorId, "VALIDATION_ERROR", ex, path, errors);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message("Validation failed for request parameters")
                .path(path)
                .errorId(errorId)
                .details(errors)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        
        logError(errorId, "CONSTRAINT_VIOLATION", ex, path);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(ex.getMessage())
                .path(path)
                .errorId(errorId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    private void logError(String errorId, String errorType, Exception ex, String path) {
        logError(errorId, errorType, ex, path, null);
    }
    
    private void logError(String errorId, String errorType, Exception ex, String path, Map<String, String> details) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n┌─────────────────────────────────────────────────────────────────────────────────┐\n");
        logMessage.append(String.format("│ 🔥 ERROR [%s] %s                                                      \n", errorId, errorType));
        logMessage.append("├─────────────────────────────────────────────────────────────────────────────────┤\n");
        logMessage.append(String.format("│ 🔍 Path: %s                                                           \n", path));
        logMessage.append("├─────────────────────────────────────────────────────────────────────────────────┤\n");
        logMessage.append(String.format("│ ❌ Message: %s                                                        \n", ex.getMessage()));
        
        if (details != null && !details.isEmpty()) {
            logMessage.append("├─────────────────────────────────────────────────────────────────────────────────┤\n");
            logMessage.append("│ 📋 Details:                                                                     \n");
            details.forEach((key, value) -> 
                logMessage.append(String.format("│   - %s: %s                                                       \n", key, value))
            );
        }
        
        logMessage.append("├─────────────────────────────────────────────────────────────────────────────────┤\n");
        logMessage.append("│ 📚 Stack trace:                                                                  \n");
        
        // Format stack trace
        StackTraceElement[] stackTrace = ex.getStackTrace();
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
            logMessage.append(String.format("│   at %s                                                             \n", stackTrace[i]));
        }
        
        if (stackTrace.length > 5) {
            logMessage.append("│   ... (truncated)                                                            \n");
        }
        
        logMessage.append("└─────────────────────────────────────────────────────────────────────────────────┘");
        
        log.error(logMessage.toString());
    }
}
