package com.assignment.datasetapi.service;

import com.assignment.datasetapi.dto.InsertRecordRequest;
import com.assignment.datasetapi.dto.InsertRecordResponse;
import com.assignment.datasetapi.dto.QueryParams;
import com.assignment.datasetapi.dto.QueryResponse;

/**
 * 
 * The Service Interface defines the CONTRACT (what operations are available).
 * The implementation class provides HOW those operations work.
 *
 * WHY USE AN INTERFACE?
 * 1. Loose coupling: Controller depends on the interface, not the implementation.
 *    This means we can swap implementations without touching the Controller.
 * 2. Testability: In tests, we can mock this interface easily with Mockito.
 * 3. Design Pattern: Interface Segregation + Dependency Inversion (SOLID principles)
 *
 * HOW SPRING USES THIS:
 * @Autowired DatasetService service; → Spring auto-injects DatasetServiceImpl
 * because it's the only class that implements this interface.
 */
public interface DatasetService {

    /**
     * Inserts a JSON record into the specified dataset.
     *
     * @param datasetName  the logical dataset name (from URL path)
     * @param request      the JSON body as a dynamic key-value map
     * @return             confirmation response with the record identifier
     */
    InsertRecordResponse insertRecord(String datasetName, InsertRecordRequest request);

    /**
     * Queries records from a dataset with optional group-by or sort-by operations.
     *
     * @param datasetName  the logical dataset name (from URL path)
     * @param params       query parameters (groupBy, sortBy, order)
     * @return             grouped or sorted records
     */
    QueryResponse queryRecords(String datasetName, QueryParams params);
}
