package com.assignment.datasetapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * A Spring Configuration class — it defines Beans that Spring manages.
 *
 * WHAT IS A BEAN?
 * A Bean is just an object managed by the Spring IoC (Inversion of Control) container.
 * When you annotate a method with @Bean, Spring calls it once on startup and stores
 * the returned object. Any class that needs it gets it injected automatically.
 *
 * WHY CONFIGURE ObjectMapper HERE?
 * The default ObjectMapper works fine, but we want to:
 * 1. Handle Java 8 date/time types (LocalDateTime, etc.)
 * 2. Make JSON output pretty-printed (optional, can disable in prod)
 *
 * @Configuration tells Spring: "This class contains @Bean definitions"
 */
@Configuration
public class AppConfig {

    /**
     * Configures Jackson's ObjectMapper for JSON serialization.
     *
     * ObjectMapper is used throughout our service to:
     * - Convert record Map → JSON string (for DB storage)
     * - Convert JSON string → record Map (for queries)
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Support for Java 8 date/time types (LocalDateTime, LocalDate, etc.)
        mapper.registerModule(new JavaTimeModule());

        // Don't serialize dates as timestamps (use ISO format instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }

    /**
     * Configures Swagger/OpenAPI documentation metadata.
     * Visible at http://localhost:8080/swagger-ui.html
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dataset API")
                        .version("1.0.0")
                        .description("""
                                REST API for inserting and querying JSON dataset records.
                                
                                **Features:**
                                - Insert any JSON record into a named dataset
                                - Query records with GROUP BY operation
                                - Query records with SORT BY operation (asc/desc)
                                
                                **How to use:**
                                1. Insert records via POST /api/dataset/{datasetName}/record
                                2. Query via GET /api/dataset/{datasetName}/query?groupBy=field
                                3. Or sort via GET /api/dataset/{datasetName}/query?sortBy=field&order=asc
                                """)
                        .contact(new Contact()
                                .name("Dataset API")
                                .email("dev@example.com"))
                );
    }
}
