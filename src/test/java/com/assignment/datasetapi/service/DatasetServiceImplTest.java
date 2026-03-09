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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS for DatasetServiceImpl
 *
 * WHAT IS UNIT TESTING?
 * Tests a single class in ISOLATION. External dependencies (like the DB repository)
 * are MOCKED — we replace them with fake objects that we control.
 *
 * KEY ANNOTATIONS:
 * @ExtendWith(MockitoExtension.class) → enables Mockito in JUnit 5
 * @Mock  → creates a fake/mock object (no real DB calls)
 * @InjectMocks → creates the real class and injects the mocks into it
 *
 * TDD APPROACH:
 * Each test follows AAA pattern:
 * Arrange → set up inputs and mock behavior
 * Act     → call the method being tested
 * Assert  → verify the output is correct
 *
 * @Nested: groups related tests together for better readability
 * @DisplayName: human-readable test name shown in test reports
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatasetService Unit Tests")
class DatasetServiceImplTest {

    @Mock
    private DatasetRecordRepository repository;

    // We use a REAL ObjectMapper (not a mock) because JSON parsing is core logic
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DatasetServiceImpl service;

    // @BeforeEach: runs before EVERY test method
    @BeforeEach
    void setUp() {
        // Re-inject objectMapper manually since @InjectMocks doesn't handle
        // the second constructor arg automatically in all cases
        service = new DatasetServiceImpl(repository, objectMapper);
    }

    // ─────────────────────────────────────────────────────────────
    // INSERT RECORD TESTS
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("insertRecord()")
    class InsertRecordTests {

        @Test
        @DisplayName("Should successfully insert a valid record")
        void insertRecord_validRequest_returnsSuccessResponse() {
            // ARRANGE
            String datasetName = "employee_dataset";
            InsertRecordRequest request = new InsertRecordRequest();
            request.addField("id", 1);
            request.addField("name", "John Doe");
            request.addField("department", "Engineering");

            // Mock: when repository.save() is called, return a fake saved entity
            DatasetRecord savedRecord = DatasetRecord.builder()
                    .id(1L)
                    .datasetName(datasetName)
                    .recordData("{\"id\":1,\"name\":\"John Doe\"}")
                    .build();
            when(repository.save(any(DatasetRecord.class))).thenReturn(savedRecord);

            // ACT
            InsertRecordResponse response = service.insertRecord(datasetName, request);

            // ASSERT
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Record added successfully");
            assertThat(response.getDataset()).isEqualTo(datasetName);
            assertThat(response.getRecordId()).isEqualTo(1);

            // Verify the repository was called exactly once
            verify(repository, times(1)).save(any(DatasetRecord.class));
        }

        @Test
        @DisplayName("Should throw InvalidRecordException for empty request body")
        void insertRecord_emptyBody_throwsInvalidRecordException() {
            // ARRANGE
            InsertRecordRequest emptyRequest = new InsertRecordRequest();

            // ACT + ASSERT: assertThatThrownBy checks that an exception is thrown
            assertThatThrownBy(() -> service.insertRecord("my_dataset", emptyRequest))
                    .isInstanceOf(InvalidRecordException.class)
                    .hasMessageContaining("cannot be empty");

            // Verify repository was NEVER called (we fail fast before reaching DB)
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidRecordException for blank dataset name")
        void insertRecord_blankDatasetName_throwsInvalidRecordException() {
            InsertRecordRequest request = new InsertRecordRequest();
            request.addField("name", "test");

            assertThatThrownBy(() -> service.insertRecord("  ", request))
                    .isInstanceOf(InvalidRecordException.class)
                    .hasMessageContaining("Dataset name cannot be empty");
        }

        @Test
        @DisplayName("Should use DB-generated ID when record has no 'id' field")
        void insertRecord_noIdField_usesDatabaseId() {
            InsertRecordRequest request = new InsertRecordRequest();
            request.addField("name", "No ID Record");

            DatasetRecord savedRecord = DatasetRecord.builder()
                    .id(42L)
                    .datasetName("test_dataset")
                    .recordData("{\"name\":\"No ID Record\"}")
                    .build();
            when(repository.save(any())).thenReturn(savedRecord);

            InsertRecordResponse response = service.insertRecord("test_dataset", request);

            // Should fall back to the DB auto-generated ID
            assertThat(response.getRecordId()).isEqualTo(42L);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GROUP-BY TESTS
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("queryRecords() - Group By")
    class GroupByTests {

        private List<DatasetRecord> mockRecords;

        @BeforeEach
        void setUp() {
            mockRecords = List.of(
                buildRecord(1L, "employee_dataset", "{\"id\":1,\"name\":\"John\",\"department\":\"Engineering\"}"),
                buildRecord(2L, "employee_dataset", "{\"id\":2,\"name\":\"Jane\",\"department\":\"Engineering\"}"),
                buildRecord(3L, "employee_dataset", "{\"id\":3,\"name\":\"Alice\",\"department\":\"Marketing\"}")
            );
        }

        @Test
        @DisplayName("Should group records by the specified field")
        void queryRecords_groupBy_returnsGroupedRecords() {
            // ARRANGE
            when(repository.findByDatasetName("employee_dataset")).thenReturn(mockRecords);
            QueryParams params = new QueryParams();
            params.setGroupBy("department");

            // ACT
            QueryResponse response = service.queryRecords("employee_dataset", params);

            // ASSERT
            assertThat(response.getGroupedRecords()).isNotNull();
            assertThat(response.getSortedRecords()).isNull(); // sorted should not be present
            assertThat(response.getGroupedRecords()).containsKeys("Engineering", "Marketing");
            assertThat(response.getGroupedRecords().get("Engineering")).hasSize(2);
            assertThat(response.getGroupedRecords().get("Marketing")).hasSize(1);
        }

        @Test
        @DisplayName("Should throw DatasetNotFoundException when dataset has no records")
        void queryRecords_emptyDataset_throwsNotFoundException() {
            when(repository.findByDatasetName("unknown")).thenReturn(List.of());
            QueryParams params = new QueryParams();
            params.setGroupBy("department");

            assertThatThrownBy(() -> service.queryRecords("unknown", params))
                    .isInstanceOf(DatasetNotFoundException.class)
                    .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("Should throw InvalidQueryException when groupBy field doesn't exist")
        void queryRecords_nonExistentField_throwsInvalidQueryException() {
            when(repository.findByDatasetName("employee_dataset")).thenReturn(mockRecords);
            QueryParams params = new QueryParams();
            params.setGroupBy("nonExistentField");

            assertThatThrownBy(() -> service.queryRecords("employee_dataset", params))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("nonExistentField")
                    .hasMessageContaining("Available fields");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SORT-BY TESTS
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("queryRecords() - Sort By")
    class SortByTests {

        private List<DatasetRecord> mockRecords;

        @BeforeEach
        void setUp() {
            mockRecords = List.of(
                buildRecord(1L, "emp", "{\"id\":1,\"name\":\"John\",\"age\":30}"),
                buildRecord(2L, "emp", "{\"id\":2,\"name\":\"Jane\",\"age\":25}"),
                buildRecord(3L, "emp", "{\"id\":3,\"name\":\"Alice\",\"age\":28}")
            );
        }

        @Test
        @DisplayName("Should sort records by numeric field ascending")
        void queryRecords_sortByAgeAsc_returnsSortedAscending() {
            when(repository.findByDatasetName("emp")).thenReturn(mockRecords);
            QueryParams params = new QueryParams();
            params.setSortBy("age");
            params.setOrder("asc");

            QueryResponse response = service.queryRecords("emp", params);

            assertThat(response.getSortedRecords()).isNotNull();
            assertThat(response.getGroupedRecords()).isNull();
            List<Map<String, Object>> sorted = response.getSortedRecords();
            // ages should be: 25, 28, 30
            assertThat(((Number) sorted.get(0).get("age")).intValue()).isEqualTo(25);
            assertThat(((Number) sorted.get(1).get("age")).intValue()).isEqualTo(28);
            assertThat(((Number) sorted.get(2).get("age")).intValue()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should sort records by numeric field descending")
        void queryRecords_sortByAgeDesc_returnsSortedDescending() {
            when(repository.findByDatasetName("emp")).thenReturn(mockRecords);
            QueryParams params = new QueryParams();
            params.setSortBy("age");
            params.setOrder("desc");

            QueryResponse response = service.queryRecords("emp", params);

            List<Map<String, Object>> sorted = response.getSortedRecords();
            // ages should be: 30, 28, 25
            assertThat(((Number) sorted.get(0).get("age")).intValue()).isEqualTo(30);
            assertThat(((Number) sorted.get(2).get("age")).intValue()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should sort records by string field")
        void queryRecords_sortByNameAsc_returnsSortedAlphabetically() {
            when(repository.findByDatasetName("emp")).thenReturn(mockRecords);
            QueryParams params = new QueryParams();
            params.setSortBy("name");
            params.setOrder("asc");

            QueryResponse response = service.queryRecords("emp", params);
            List<Map<String, Object>> sorted = response.getSortedRecords();

            // alphabetically: Alice, Jane, John
            assertThat(sorted.get(0).get("name")).isEqualTo("Alice");
            assertThat(sorted.get(1).get("name")).isEqualTo("Jane");
            assertThat(sorted.get(2).get("name")).isEqualTo("John");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATION TESTS
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("queryRecords() - Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should throw when neither groupBy nor sortBy is provided")
        void queryRecords_noParams_throwsInvalidQueryException() {
            QueryParams params = new QueryParams();
            // both groupBy and sortBy are null

            assertThatThrownBy(() -> service.queryRecords("dataset", params))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("At least one query parameter is required");
        }

        @Test
        @DisplayName("Should throw when both groupBy and sortBy are provided")
        void queryRecords_bothParams_throwsInvalidQueryException() {
            QueryParams params = new QueryParams();
            params.setGroupBy("department");
            params.setSortBy("age");

            assertThatThrownBy(() -> service.queryRecords("dataset", params))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("Only one operation is allowed");
        }

        @Test
        @DisplayName("Should throw when order value is invalid")
        void queryRecords_invalidOrder_throwsInvalidQueryException() {
            QueryParams params = new QueryParams();
            params.setSortBy("age");
            params.setOrder("random");

            assertThatThrownBy(() -> service.queryRecords("dataset", params))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("Invalid order value");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────

    private DatasetRecord buildRecord(Long id, String datasetName, String json) {
        return DatasetRecord.builder()
                .id(id)
                .datasetName(datasetName)
                .recordData(json)
                .build();
    }
}
