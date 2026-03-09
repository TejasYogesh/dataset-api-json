package com.assignment.datasetapi.service;

import com.assignment.datasetapi.dto.InsertRecordRequest;
import com.assignment.datasetapi.dto.InsertRecordResponse;
import com.assignment.datasetapi.dto.QueryParams;
import com.assignment.datasetapi.dto.QueryResponse;
import com.assignment.datasetapi.exception.DatasetNotFoundException;
import com.assignment.datasetapi.exception.InvalidQueryException;
import com.assignment.datasetapi.exception.InvalidRecordException;
import com.assignment.datasetapi.model.DatasetRecord;
import com.assignment.datasetapi.repository.DatasetRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 
 * The Service Implementation — where all the business logic lives.
 *
 * ANNOTATIONS EXPLAINED:
 * @Service     = Marks this as a Spring-managed service bean.
 *               Spring will create one instance and inject it wherever needed.
 *
 * @Slf4j       = Lombok gives us: private static final Logger log = ...
 *               Use: log.info("message"), log.error("message"), etc.
 *
 * @RequiredArgsConstructor = Lombok generates a constructor for all `final` fields.
 *               This is the RECOMMENDED way to do dependency injection
 *               (constructor injection > field injection).
 *
 * DESIGN PATTERN: Service Layer Pattern
 * Controllers handle HTTP. Repositories handle DB. Services handle BUSINESS LOGIC.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    // Spring injects these via constructor (thanks to @RequiredArgsConstructor)
    private final DatasetRecordRepository repository;

    /**
     * ObjectMapper is Jackson's core class.
     * It converts Java objects ↔ JSON strings.
     * - writeValueAsString(object) → turns object into JSON string
     * - readValue(string, type)    → turns JSON string into Java object
     */
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────
    // INSERT RECORD
    // ─────────────────────────────────────────────────────────────

    /**
     * @Transactional: wraps the entire method in a DB transaction.
     * If anything fails midway, the transaction rolls back (no partial saves).
     * This is crucial for data integrity.
     */
    @Override
    @Transactional
    public InsertRecordResponse insertRecord(String datasetName, InsertRecordRequest request) {
        log.info("Inserting record into dataset: {}", datasetName);

        // VALIDATION: Ensure request body is not empty
        validateInsertRequest(datasetName, request);

        // STEP 1: Serialize the dynamic JSON map → String for DB storage
        // { "id": 1, "name": "John" } → "{\"id\":1,\"name\":\"John\"}"
        String recordDataJson = serializeToJson(request.getFields());

        // STEP 2: Build the entity using the Builder pattern
        DatasetRecord record = DatasetRecord.builder()
                .datasetName(datasetName.trim())
                .recordData(recordDataJson)
                .build();

        // STEP 3: Persist to database (JPA handles the SQL INSERT)
        DatasetRecord savedRecord = repository.save(record);
        log.info("Record saved with DB id: {}", savedRecord.getId());

        // STEP 4: Extract the user-provided "id" field for the response
        // If they sent {"id": 1, "name": "John"}, we return recordId: 1
        // If no "id" field, we return the internal DB auto-generated id
        Object recordId = request.getFields().getOrDefault("id", savedRecord.getId());

        return InsertRecordResponse.builder()
                .message("Record added successfully")
                .dataset(datasetName.trim())
                .recordId(recordId)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY RECORDS
    // ─────────────────────────────────────────────────────────────

    /**
     * @Transactional(readOnly = true): tells JPA this is a read-only operation.
     * Benefits:
     * - Better performance (no dirty checking, no flush)
     * - Prevents accidental writes
     */
    @Override
    @Transactional(readOnly = true)
    public QueryResponse queryRecords(String datasetName, QueryParams params) {
        log.info("Querying dataset: {} with params: groupBy={}, sortBy={}, order={}",
                datasetName, params.getGroupBy(), params.getSortBy(), params.getOrder());

        // VALIDATION: Ensure at least one query operation is specified
        validateQueryParams(params);

        // STEP 1: Fetch all records for this dataset from DB
        List<DatasetRecord> records = repository.findByDatasetName(datasetName.trim());

        // STEP 2: Validate dataset exists (has at least one record)
        if (records.isEmpty()) {
            throw new DatasetNotFoundException(datasetName);
        }

        // STEP 3: Deserialize each stored JSON string → Map<String, Object>
        // This allows us to access fields dynamically (we don't know the schema upfront)
        List<Map<String, Object>> parsedRecords = deserializeRecords(records);

        // STEP 4: Apply the requested operation
        if (params.hasGroupBy()) {
            return performGroupBy(parsedRecords, params.getGroupBy());
        } else {
            return performSortBy(parsedRecords, params.getSortBy(), params.isDescending());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GROUP-BY LOGIC
    // ─────────────────────────────────────────────────────────────

    /**
     * Groups records by the value of the specified field.
     *
     * Example: groupBy = "department"
     * Input:  [{"name":"John","department":"Engineering"}, {"name":"Alice","department":"Marketing"}]
     * Output: {"Engineering": [{"name":"John",...}], "Marketing": [{"name":"Alice",...}]}
     *
     * HOW IT WORKS (Java Streams):
     * Collectors.groupingBy() is like SQL's GROUP BY.
     * It returns a Map where keys are distinct values of the field.
     */
    private QueryResponse performGroupBy(List<Map<String, Object>> records, String groupByField) {
        log.debug("Performing group-by on field: {}", groupByField);

        // Validate that the field exists in at least one record
        validateFieldExists(records, groupByField);

        // Java Stream: groupingBy collects records into groups
        // r.getOrDefault(..., "N/A") handles records that don't have this field
        Map<String, List<Map<String, Object>>> grouped = records.stream()
                .collect(Collectors.groupingBy(
                        record -> String.valueOf(record.getOrDefault(groupByField, "N/A")),
                        LinkedHashMap::new,   // Use LinkedHashMap to preserve insertion order
                        Collectors.toList()
                ));

        log.debug("Grouped into {} groups", grouped.size());

        return QueryResponse.builder()
                .groupedRecords(grouped)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // SORT-BY LOGIC
    // ─────────────────────────────────────────────────────────────

    /**
     * Sorts records by the value of the specified field.
     *
     * Example: sortBy = "age", order = "asc"
     * Input:  [{"name":"John","age":30}, {"name":"Jane","age":25}]
     * Output: [{"name":"Jane","age":25}, {"name":"John","age":30}]
     *
     * CHALLENGE: Values can be numbers OR strings — we must handle both!
     * - Numeric comparison: 25 < 30 (not "25" < "30" which is string comparison)
     * - String comparison: "Alice" < "John" (lexicographic)
     */
    private QueryResponse performSortBy(List<Map<String, Object>> records, String sortByField,
                                        boolean descending) {
        log.debug("Performing sort-by on field: {}, descending: {}", sortByField, descending);

        // Validate that the field exists in at least one record
        validateFieldExists(records, sortByField);

        // Create a new list (don't mutate the original!)
        List<Map<String, Object>> sortedRecords = new ArrayList<>(records);

        // Custom Comparator that handles both numbers and strings
        Comparator<Map<String, Object>> comparator = (a, b) -> {
            Object valA = a.get(sortByField);
            Object valB = b.get(sortByField);

            // Handle null values: nulls go last
            if (valA == null && valB == null) return 0;
            if (valA == null) return 1;
            if (valB == null) return -1;

            // NUMERIC COMPARISON: if both values are numbers, compare numerically
            // Jackson deserializes JSON numbers as Integer, Long, or Double
            if (valA instanceof Number numA && valB instanceof Number numB) {
                return Double.compare(numA.doubleValue(), numB.doubleValue());
            }

            // STRING COMPARISON: fall back to lexicographic comparison
            return String.valueOf(valA).compareToIgnoreCase(String.valueOf(valB));
        };

        // Apply descending order by reversing the comparator if needed
        if (descending) {
            comparator = comparator.reversed();
        }

        sortedRecords.sort(comparator);
        log.debug("Sorted {} records", sortedRecords.size());

        return QueryResponse.builder()
                .sortedRecords(sortedRecords)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPER METHODS
    // ─────────────────────────────────────────────────────────────

    /**
     * Converts each DB record's JSON string → Map<String, Object>
     * Skips records that can't be parsed (logs a warning instead of crashing)
     */
    private List<Map<String, Object>> deserializeRecords(List<DatasetRecord> records) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DatasetRecord record : records) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(
                        record.getRecordData(),
                        new TypeReference<Map<String, Object>>() {}
                );
                result.add(parsed);
            } catch (JsonProcessingException e) {
                log.warn("Could not parse record with id {}: {}", record.getId(), e.getMessage());
                // Skip corrupt records gracefully
            }
        }
        return result;
    }

    /**
     * Converts Map<String, Object> → JSON string for storage
     */
    private String serializeToJson(Map<String, Object> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize record data: {}", e.getMessage());
            throw new InvalidRecordException("Could not process the provided JSON record.");
        }
    }

    /**
     * Checks that the requested groupBy/sortBy field exists in at least one record.
     * Provides a helpful error message listing valid field names.
     */
    private void validateFieldExists(List<Map<String, Object>> records, String fieldName) {
        boolean fieldExists = records.stream()
                .anyMatch(record -> record.containsKey(fieldName));

        if (!fieldExists) {
            // Collect all available field names for a helpful error message
            Set<String> availableFields = records.stream()
                    .flatMap(r -> r.keySet().stream())
                    .collect(Collectors.toCollection(TreeSet::new)); // TreeSet = sorted

            throw new InvalidQueryException(
                    "Field '" + fieldName + "' does not exist in this dataset. " +
                    "Available fields: " + availableFields
            );
        }
    }

    private void validateInsertRequest(String datasetName, InsertRecordRequest request) {
        if (datasetName == null || datasetName.isBlank()) {
            throw new InvalidRecordException("Dataset name cannot be empty.");
        }
        if (request == null || request.isEmpty()) {
            throw new InvalidRecordException(
                    "Request body cannot be empty. Please provide a JSON object with at least one field.");
        }
    }

    private void validateQueryParams(QueryParams params) {
        if (!params.hasGroupBy() && !params.hasSortBy()) {
            throw new InvalidQueryException(
                    "At least one query parameter is required. " +
                    "Use 'groupBy=<field>' or 'sortBy=<field>&order=asc|desc'.");
        }
        if (params.hasGroupBy() && params.hasSortBy()) {
            throw new InvalidQueryException(
                    "Only one operation is allowed per request. " +
                    "Provide either 'groupBy' OR 'sortBy', not both.");
        }
        if (params.hasSortBy()) {
            String order = params.getOrder();
            if (!"asc".equalsIgnoreCase(order) && !"desc".equalsIgnoreCase(order)) {
                throw new InvalidQueryException(
                        "Invalid order value: '" + order + "'. Must be 'asc' or 'desc'.");
            }
        }
    }
}
