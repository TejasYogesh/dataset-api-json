package com.assignment.datasetapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for the Insert Record API.
 *
 * Always maps to:
 * {
 *   "message": "Record added successfully",
 *   "dataset": "employee_dataset",
 *   "recordId": 1
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsertRecordResponse {
    private String message;
    private String dataset;
    private Object recordId;
}


