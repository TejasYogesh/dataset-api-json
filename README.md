# Dataset API — Spring Boot Assignment

A RESTful API for inserting and querying JSON records with Group-By and Sort-By operations.

---

## 🚀 How to Run

### Prerequisites
- Java 17+
- Maven 3.6+ (or use the included `mvnw` wrapper)

### Steps

```bash
# 1. Clone / navigate to project
cd dataset-api

# 2. Build the project
mvn clean install

# 3. Run the application
mvn spring-boot:run
```

The app starts on **http://localhost:8080**

### Useful URLs after startup:
| URL | Purpose |
|-----|---------|
| http://localhost:8080/swagger-ui.html | Interactive API docs |
| http://localhost:8080/h2-console | Database viewer (JDBC URL: `jdbc:h2:mem:datasetdb`) |
| http://localhost:8080/api-docs | Raw OpenAPI spec (JSON) |

---

## 📦 Project Structure

```
src/
├── main/java/com/assignment/datasetapi/
│   ├── DatasetApiApplication.java     ← Main entry point
│   ├── config/
│   │   └── AppConfig.java             ← Bean config (ObjectMapper, Swagger)
│   ├── controller/
│   │   └── DatasetController.java     ← REST endpoints
│   ├── dto/
│   │   ├── InsertRecordRequest.java   ← Dynamic JSON request body
│   │   ├── InsertRecordResponse.java  ← Insert response shape
│   │   ├── QueryParams.java           ← Query parameter holder
│   │   └── QueryResponse.java        ← Group/sort response shape
│   ├── exception/
│   │   ├── DatasetNotFoundException.java
│   │   ├── InvalidQueryException.java
│   │   ├── InvalidRecordException.java
│   │   └── GlobalExceptionHandler.java ← Centralized error handling
│   ├── model/
│   │   └── DatasetRecord.java         ← JPA entity (DB table)
│   ├── repository/
│   │   └── DatasetRecordRepository.java ← Database operations
│   └── service/
│       ├── DatasetService.java        ← Service interface
│       └── DatasetServiceImpl.java    ← Business logic
└── test/java/com/assignment/datasetapi/
    ├── controller/
    │   └── DatasetControllerTest.java ← MockMvc HTTP tests
    └── service/
        └── DatasetServiceImplTest.java ← Unit tests with Mockito
```

---

## 🧪 Running Tests

```bash
# Run all tests
mvn test

# Run with test report
mvn test surefire-report:report
```

---

## 📋 API Reference

### 1. Insert Record

**`POST /api/dataset/{datasetName}/record`**

Inserts a JSON record into the named dataset. The JSON body is schema-free — any fields are accepted.

#### Request
```
POST /api/dataset/employee_dataset/record
Content-Type: application/json
```
```json
{
  "id": 1,
  "name": "John Doe",
  "age": 30,
  "department": "Engineering"
}
```

#### Response — `201 Created`
```json
{
  "message": "Record added successfully",
  "dataset": "employee_dataset",
  "recordId": 1
}
```

#### Error Responses
| Status | When |
|--------|------|
| 400 | Empty request body |
| 400 | Malformed JSON |
| 500 | Unexpected server error |

---

### 2. Query Records — Group By

**`GET /api/dataset/{datasetName}/query?groupBy={field}`**

Groups all records in the dataset by the value of the specified field.

#### Request
```
GET /api/dataset/employee_dataset/query?groupBy=department
```

#### Response — `200 OK`
```json
{
  "groupedRecords": {
    "Engineering": [
      { "id": 1, "name": "John Doe", "age": 30, "department": "Engineering" },
      { "id": 2, "name": "Jane Smith", "age": 25, "department": "Engineering" }
    ],
    "Marketing": [
      { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing" }
    ]
  }
}
```

---

### 3. Query Records — Sort By

**`GET /api/dataset/{datasetName}/query?sortBy={field}&order={asc|desc}`**

Sorts all records by the specified field. Handles both numeric and string fields correctly.

#### Request
```
GET /api/dataset/employee_dataset/query?sortBy=age&order=asc
```

#### Response — `200 OK`
```json
{
  "sortedRecords": [
    { "id": 2, "name": "Jane Smith", "age": 25, "department": "Engineering" },
    { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing" },
    { "id": 1, "name": "John Doe", "age": 30, "department": "Engineering" }
  ]
}
```

#### Error Responses
| Status | When |
|--------|------|
| 400 | Neither `groupBy` nor `sortBy` provided |
| 400 | Both `groupBy` AND `sortBy` provided |
| 400 | `order` is not `asc` or `desc` |
| 400 | Specified field doesn't exist in the dataset |
| 404 | Dataset has no records |

---

## 🔬 Postman Testing Guide

### Step 1: Insert sample records

Run these 3 POST requests first:

**Employee 1:**
```
POST http://localhost:8080/api/dataset/employee_dataset/record
Body: {"id":1,"name":"John Doe","age":30,"department":"Engineering"}
```

**Employee 2:**
```
POST http://localhost:8080/api/dataset/employee_dataset/record
Body: {"id":2,"name":"Jane Smith","age":25,"department":"Engineering"}
```

**Employee 3:**
```
POST http://localhost:8080/api/dataset/employee_dataset/record
Body: {"id":3,"name":"Alice Brown","age":28,"department":"Marketing"}
```

### Step 2: Test Group By
```
GET http://localhost:8080/api/dataset/employee_dataset/query?groupBy=department
```

### Step 3: Test Sort By (ascending)
```
GET http://localhost:8080/api/dataset/employee_dataset/query?sortBy=age&order=asc
```

### Step 4: Test Sort By (descending)
```
GET http://localhost:8080/api/dataset/employee_dataset/query?sortBy=name&order=desc
```

### Step 5: Test error cases
```
# 404 - Dataset not found
GET http://localhost:8080/api/dataset/nonexistent/query?groupBy=department

# 400 - Field doesn't exist
GET http://localhost:8080/api/dataset/employee_dataset/query?groupBy=salary

# 400 - No params
GET http://localhost:8080/api/dataset/employee_dataset/query
```

---

## 🏗️ Design Decisions

| Decision | Reason |
|----------|--------|
| Schema-free JSON stored as TEXT | Records can have any structure; storing as string gives flexibility |
| Service Interface + Impl | Loose coupling; easy to mock in tests; follows Dependency Inversion |
| `@JsonInclude(NON_NULL)` on QueryResponse | Cleaner API: `groupedRecords` absent from response when doing sortBy, and vice versa |
| Numeric-aware comparator | Prevents string-sort of numbers ("25" > "9" but numerically 9 < 25) |
| `LinkedHashMap` for group results | Preserves insertion order in grouped output |
| Global `@RestControllerAdvice` | Centralized error handling; no try-catch in controllers |
| Constructor injection | Preferred over `@Autowired` on fields; makes dependencies explicit and testable |

---

## 🗄️ Database Schema

```sql
CREATE TABLE dataset_records (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_name VARCHAR(255) NOT NULL,
    record_data  TEXT NOT NULL,
    created_at   TIMESTAMP
);

CREATE INDEX idx_dataset_name ON dataset_records(dataset_name);
```
