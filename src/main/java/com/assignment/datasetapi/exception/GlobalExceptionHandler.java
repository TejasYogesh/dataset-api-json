package com.assignment.datasetapi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * A Global Exception Handler using Spring's @RestControllerAdvice.
 *
 * HOW IT WORKS:
 * Instead of wrapping every controller method in try-catch blocks,
 * we centralize ALL exception handling here.
 *
 * When ANY exception is thrown anywhere in the app, Spring automatically
 * calls the matching @ExceptionHandler method here.
 *
 * DESIGN PATTERN: Centralized Error Handling (AOP-style cross-cutting concern)
 *
 * WHY @RestControllerAdvice?
 * = @ControllerAdvice + @ResponseBody
 * Means: "Apply this advice to all controllers, and return JSON responses"
 *
 * @Slf4j (from Lombok): gives us a `log` object for logging without boilerplate
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─────────────────────────────────────────────────────────────
    // 404 NOT FOUND
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(DatasetNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDatasetNotFound(DatasetNotFoundException ex) {
        log.warn("Dataset not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ─────────────────────────────────────────────────────────────
    // 400 BAD REQUEST
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidQuery(InvalidQueryException ex) {
        log.warn("Invalid query: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidRecordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRecord(InvalidRecordException ex) {
        log.warn("Invalid record: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles malformed JSON in request body
     * e.g., sending "not json" as the body to the insert API
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON in request body: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Request body is not valid JSON. Please provide a valid JSON object.");
    }

    /**
     * Handles type mismatch in path variables or query params
     * e.g., passing a non-numeric value where a number is expected
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'.");
    }

    // ─────────────────────────────────────────────────────────────
    // 500 INTERNAL SERVER ERROR — catch-all for unexpected errors
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log the full stack trace for debugging
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPER: builds a consistent error response structure
    // ─────────────────────────────────────────────────────────────

    /**
     * All error responses follow the same structure:
     * {
     *   "status": 404,
     *   "error": "Not Found",
     *   "message": "Dataset not found: 'xyz'",
     *   "timestamp": "2024-01-01T10:00:00"
     * }
     *
     * Consistent error responses are essential for good REST API design!
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("status", status.value());
        errorBody.put("error", status.getReasonPhrase());
        errorBody.put("message", message);
        errorBody.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(errorBody);
    }
}
