package com.assignment.datasetapi.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WHAT IS A DTO?
 * DTO = Data Transfer Object. It's the shape of data we RECEIVE or SEND over HTTP.
 * We keep DTOs separate from our DB Entity (DatasetRecord) because:
 * 1. API contract can differ from DB schema
 * 2. Protects DB internals from being exposed
 * 3. Easier to validate input without touching DB model
 *
 * DESIGN PATTERN: DTO Pattern
 *
 * CHALLENGE: The JSON body can have ANY fields (id, name, age, department, etc.)
 * We solve this with @JsonAnySetter / @JsonAnyGetter from Jackson.
 *
 * Example request body:
 * { "id": 1, "name": "John", "age": 30, "department": "Engineering" }
 *
 * This gets deserialized into a Map<String, Object> dynamically.
 */
@Data
public class InsertRecordRequest {

    /**
     * @JsonAnySetter tells Jackson:
     * "For every JSON field that doesn't match a Java field, put it in this map"
     *
     * So { "id": 1, "name": "John" } becomes:
     * fields = {"id": 1, "name": "John"}
     */
    private Map<String, Object> fields = new LinkedHashMap<>();

    @JsonAnySetter
    public void addField(String key, Object value) {
        this.fields.put(key, value);
    }

    /**
     * @JsonAnyGetter tells Jackson:
     * "When serializing this object back to JSON, inline the map entries"
     *
     * So the object serializes back to: {"id":1,"name":"John"} (not {"fields":{...}})
     */
    @JsonAnyGetter
    public Map<String, Object> getFields() {
        return fields;
    }

    /**
     * Validation: ensure the request body is not empty
     */
    @NotNull
    public Map<String, Object> getFieldsForValidation() {
        return fields;
    }

    public boolean isEmpty() {
        return fields == null || fields.isEmpty();
    }
}
