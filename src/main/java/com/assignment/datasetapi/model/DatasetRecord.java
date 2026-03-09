package com.assignment.datasetapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 
 * This is a JPA Entity — it maps directly to a database TABLE called "dataset_records".
 * Every field here becomes a COLUMN in that table.
 *
 * DESIGN PATTERN: Repository Pattern
 * This is our domain model. It's the "M" in MVC.
 *
 * LOMBOK ANNOTATIONS (saves us writing boilerplate):
 * @Data        = generates getters, setters, equals, hashCode, toString
 * @Builder     = lets us do DatasetRecord.builder().datasetName("x").build()
 * @NoArgsConstructor = generates empty constructor (required by JPA)
 * @AllArgsConstructor = generates constructor with all fields
 */
@Entity
@Table(
    name = "dataset_records",
    // Index on datasetName for faster GROUP BY / SORT BY queries
    indexes = {
        @Index(name = "idx_dataset_name", columnList = "dataset_name")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetRecord {

    /**
     * Primary Key — auto-incremented by the database.
     * This is the internal DB ID, different from the user-supplied "id" in JSON.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Which dataset does this record belong to?
     * e.g., "employee_dataset", "sales_dataset"
     * This acts like a logical "table name" within our single DB table.
     */
    @Column(name = "dataset_name", nullable = false, length = 255)
    private String datasetName;

    /**
     * The actual JSON payload stored as a string.
     * We use @Lob (Large Object) because JSON can be very large.
     * Example: {"id":1,"name":"John","age":30,"department":"Engineering"}
     *
     * WHY store as string? Because JSON is schema-less — each record in a
     * dataset can have different fields. Storing as TEXT gives us flexibility.
     */
    @Lob
    @Column(name = "record_data", nullable = false, columnDefinition = "TEXT")
    private String recordData;

    /**
     * Audit field: automatically set when record is created.
     * @CreationTimestamp is a Hibernate annotation that fills this automatically.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
