package com.assignment.datasetapi.exception;

/**
 * 404 Not Found Exception
 * Thrown when a query targets a dataset that has no records.
 */
public class DatasetNotFoundException extends RuntimeException {
    public DatasetNotFoundException(String datasetName) {
        super("Dataset not found: '" + datasetName + "'. No records exist for this dataset.");
    }
}
