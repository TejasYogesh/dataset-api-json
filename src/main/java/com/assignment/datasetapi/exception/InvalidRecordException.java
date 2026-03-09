package com.assignment.datasetapi.exception;

/**
 * 400 Bad Request Exception
 * Thrown when the request body is empty or malformed.
 */
public class InvalidRecordException extends RuntimeException {
    public InvalidRecordException(String message) {
        super(message);
    }
}
