package com.assignment.datasetapi.exception;

/**
 * 400 Bad Request Exception
 * Thrown when query parameters are invalid or missing.
 * e.g., neither groupBy nor sortBy was provided.
 */
public class InvalidQueryException extends RuntimeException {
    public InvalidQueryException(String message) {
        super(message);
    }
}
