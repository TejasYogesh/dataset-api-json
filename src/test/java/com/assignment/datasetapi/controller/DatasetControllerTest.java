package com.assignment.datasetapi.controller;

import com.assignment.datasetapi.dto.InsertRecordResponse;
import com.assignment.datasetapi.dto.QueryResponse;
import com.assignment.datasetapi.exception.DatasetNotFoundException;
import com.assignment.datasetapi.exception.InvalidQueryException;
import com.assignment.datasetapi.service.DatasetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CONTROLLER INTEGRATION TESTS using @WebMvcTest + MockMvc
 *
 * WHAT IS @WebMvcTest?
 * Loads ONLY the web layer (controllers, filters, etc.) — NOT the full app.
 * Much faster than @SpringBootTest for testing HTTP layer.
 *
 * WHAT IS MockMvc?
 * Simulates HTTP requests without starting a real HTTP server.
 * We can test request routing, status codes, and response JSON.
 *
 * @MockBean replaces the real DatasetService with a Mockito mock.
 * We control what the service returns so we can test controller behavior in isolation.
 *
 * TEST STRUCTURE: We test the HTTP layer here:
 * - Correct URL routing
 * - HTTP status codes (201, 200, 400, 404)
 * - Response JSON structure
 * - Error handling
 */
@WebMvcTest(DatasetController.class)
@DisplayName("DatasetController Integration Tests")
class DatasetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatasetService datasetService;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────
    // INSERT RECORD TESTS
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/dataset/{datasetName}/record")
    class InsertRecordControllerTests {

        @Test
        @DisplayName("Should return 201 Created on successful insert")
        void insertRecord_validRequest_returns201() throws Exception {
            // ARRANGE
            InsertRecordResponse mockResponse = InsertRecordResponse.builder()
                    .message("Record added successfully")
                    .dataset("employee_dataset")
                    .recordId(1)
                    .build();
            when(datasetService.insertRecord(eq("employee_dataset"), any()))
                    .thenReturn(mockResponse);

            String requestBody = """
                    {
                        "id": 1,
                        "name": "John Doe",
                        "age": 30,
                        "department": "Engineering"
                    }
                    """;

            // ACT + ASSERT
            mockMvc.perform(post("/api/dataset/employee_dataset/record")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    // Check HTTP status
                    .andExpect(status().isCreated())
                    // Check response body JSON fields
                    .andExpect(jsonPath("$.message").value("Record added successfully"))
                    .andExpect(jsonPath("$.dataset").value("employee_dataset"))
                    .andExpect(jsonPath("$.recordId").value(1));
        }

        @Test
        @DisplayName("Should return 400 Bad Request for empty body")
        void insertRecord_emptyBody_returns400() throws Exception {
            when(datasetService.insertRecord(any(), any()))
                    .thenThrow(new com.assignment.datasetapi.exception.InvalidRecordException(
                            "Request body cannot be empty"));

            mockMvc.perform(post("/api/dataset/employee_dataset/record")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("Should return 400 for malformed JSON")
        void insertRecord_malformedJson_returns400() throws Exception {
            mockMvc.perform(post("/api/dataset/employee_dataset/record")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("this is not json"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY TESTS
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/dataset/{datasetName}/query")
    class QueryControllerTests {

        @Test
        @DisplayName("Should return 200 with grouped records for groupBy query")
        void queryRecords_groupBy_returns200WithGroupedData() throws Exception {
            // ARRANGE
            QueryResponse mockResponse = QueryResponse.builder()
                    .groupedRecords(Map.of(
                            "Engineering", List.of(
                                    Map.of("id", 1, "name", "John", "department", "Engineering")
                            ),
                            "Marketing", List.of(
                                    Map.of("id", 2, "name", "Alice", "department", "Marketing")
                            )
                    ))
                    .build();
            when(datasetService.queryRecords(eq("employee_dataset"), any()))
                    .thenReturn(mockResponse);

            // ACT + ASSERT
            mockMvc.perform(get("/api/dataset/employee_dataset/query")
                            .param("groupBy", "department"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groupedRecords").exists())
                    .andExpect(jsonPath("$.groupedRecords.Engineering").isArray())
                    .andExpect(jsonPath("$.groupedRecords.Marketing").isArray())
                    // sortedRecords should NOT appear (it's null + @JsonInclude(NON_NULL))
                    .andExpect(jsonPath("$.sortedRecords").doesNotExist());
        }

        @Test
        @DisplayName("Should return 200 with sorted records for sortBy query")
        void queryRecords_sortBy_returns200WithSortedData() throws Exception {
            QueryResponse mockResponse = QueryResponse.builder()
                    .sortedRecords(List.of(
                            Map.of("id", 2, "name", "Jane", "age", 25),
                            Map.of("id", 1, "name", "John", "age", 30)
                    ))
                    .build();
            when(datasetService.queryRecords(eq("employee_dataset"), any()))
                    .thenReturn(mockResponse);

            mockMvc.perform(get("/api/dataset/employee_dataset/query")
                            .param("sortBy", "age")
                            .param("order", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sortedRecords").isArray())
                    .andExpect(jsonPath("$.sortedRecords[0].age").value(25))
                    .andExpect(jsonPath("$.groupedRecords").doesNotExist());
        }

        @Test
        @DisplayName("Should return 404 when dataset not found")
        void queryRecords_unknownDataset_returns404() throws Exception {
            when(datasetService.queryRecords(eq("unknown_dataset"), any()))
                    .thenThrow(new DatasetNotFoundException("unknown_dataset"));

            mockMvc.perform(get("/api/dataset/unknown_dataset/query")
                            .param("groupBy", "field"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("unknown_dataset")));
        }

        @Test
        @DisplayName("Should return 400 when no query params provided")
        void queryRecords_noParams_returns400() throws Exception {
            when(datasetService.queryRecords(any(), any()))
                    .thenThrow(new InvalidQueryException("At least one query parameter is required"));

            mockMvc.perform(get("/api/dataset/employee_dataset/query"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }
}
