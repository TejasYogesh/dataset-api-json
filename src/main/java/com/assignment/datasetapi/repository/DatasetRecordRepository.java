package com.assignment.datasetapi.repository;

import com.assignment.datasetapi.model.DatasetRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 
 * A Spring Data JPA Repository. This interface gives us FREE database operations
 * without writing any SQL or implementation code!
 *
 * HOW IT WORKS:
 * By extending JpaRepository<DatasetRecord, Long>, Spring automatically provides:
 * - save(entity)       → INSERT or UPDATE
 * - findById(id)       → SELECT WHERE id = ?
 * - findAll()          → SELECT *
 * - delete(entity)     → DELETE
 * - count()            → SELECT COUNT(*)
 * ...and many more!
 *
 * DESIGN PATTERN: Repository Pattern
 * Abstracts the data access layer — the service layer doesn't need to know
 * whether data comes from H2, MySQL, PostgreSQL, etc.
 *
 * Generic types: JpaRepository<EntityClass, PrimaryKeyType>
 * - EntityClass = DatasetRecord (our @Entity class)
 * - PrimaryKeyType = Long (the type of our @Id field)
 */
@Repository
public interface DatasetRecordRepository extends JpaRepository<DatasetRecord, Long> {

    /**
     * Spring Data Magic: Method name tells Spring what SQL to generate!
     * "findBy" + "DatasetName" → WHERE dataset_name = ?
     *
     * Generated SQL: SELECT * FROM dataset_records WHERE dataset_name = ?
     */
    List<DatasetRecord> findByDatasetName(String datasetName);

    /**
     * Custom JPQL Query for existence check.
     * JPQL = Java Persistence Query Language (uses class/field names, not table/column names)
     *
     * This checks if any records exist for a dataset — used for 404 validation.
     */
    @Query("SELECT COUNT(r) > 0 FROM DatasetRecord r WHERE r.datasetName = :datasetName")
    boolean existsByDatasetName(@Param("datasetName") String datasetName);
}
