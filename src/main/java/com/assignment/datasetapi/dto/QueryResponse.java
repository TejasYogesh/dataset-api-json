package com.assignment.datasetapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for the Query API.
 *
 * This is flexible — it returns EITHER groupedRecords OR sortedRecords
 * depending on the query parameters provided.
 *
 * @JsonInclude(NON_NULL): Fields that are null won't appear in JSON output.
 * So if groupedRecords is null, the JSON won't contain "groupedRecords" at all.
 *
 * GROUP-BY response:
 * {
 *   "groupedRecords": {
 *     "Engineering": [ {...}, {...} ],
 *     "Marketing":   [ {...} ]
 *   }
 * }
 *
 * SORT-BY response:
 * {
 *   "sortedRecords": [ {...}, {...}, {...} ]
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResponse {

    /**
     * For GROUP-BY: Map of groupKey → list of records
     * Key = value of the groupBy field (e.g., "Engineering", "Marketing")
     * Value = all records where that field equals the key
     */
    private Map<String, List<Map<String, Object>>> groupedRecords;

    /**
     * For SORT-BY: flat list of records sorted by the specified field
     */
    private List<Map<String, Object>> sortedRecords;
}
