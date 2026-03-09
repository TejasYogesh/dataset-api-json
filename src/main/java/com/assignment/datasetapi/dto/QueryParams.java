package com.assignment.datasetapi.dto;

import lombok.Data;

/**
 * 
 * This encapsulates all query parameters for the Query API.
 * Instead of having lots of method parameters, we group them in one object.
 *
 * DESIGN PATTERN: Value Object / Parameter Object Pattern
 * Keeps method signatures clean and makes adding new params easy in future.
 *
 * Query URL examples:
 * GET /api/dataset/employee_dataset/query?groupBy=department
 * GET /api/dataset/employee_dataset/query?sortBy=age&order=asc
 * GET /api/dataset/employee_dataset/query?sortBy=name&order=desc
 */
@Data
public class QueryParams {

    /**
     * Field to group records by.
     * e.g., groupBy=department → groups all records by their "department" value
     */
    private String groupBy;

    /**
     * Field to sort records by.
     * e.g., sortBy=age → sorts records by their "age" value
     */
    private String sortBy;

    /**
     * Sort direction: "asc" (ascending) or "desc" (descending).
     * Defaults to "asc" if not provided.
     */
    private String order = "asc";

    /**
     * Helper: checks if a groupBy operation was requested
     */
    public boolean hasGroupBy() {
        return groupBy != null && !groupBy.isBlank();
    }

    /**
     * Helper: checks if a sortBy operation was requested
     */
    public boolean hasSortBy() {
        return sortBy != null && !sortBy.isBlank();
    }

    /**
     * Helper: is the sort order descending?
     */
    public boolean isDescending() {
        return "desc".equalsIgnoreCase(order);
    }
}
