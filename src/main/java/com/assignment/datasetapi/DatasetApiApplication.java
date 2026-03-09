package com.assignment.datasetapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ✅ WHAT IS THIS?
 * The entry point of the Spring Boot application.
 *
 * ✅ @SpringBootApplication is a shortcut for THREE annotations:
 *
 * 1. @Configuration
 *    → This class can define Spring @Bean methods
 *
 * 2. @EnableAutoConfiguration
 *    → Spring Boot auto-configures everything based on what's in your classpath.
 *    → Sees H2 on classpath? Auto-configures an in-memory datasource.
 *    → Sees spring-web? Auto-configures a Tomcat web server.
 *    → This is the "magic" of Spring Boot!
 *
 * 3. @ComponentScan
 *    → Scans this package and all sub-packages for:
 *       @Component, @Service, @Repository, @Controller, @RestController
 *    → Registers them all as Spring Beans automatically
 *
 * ✅ HOW TO RUN:
 * From terminal: mvn spring-boot:run
 * Or: java -jar target/dataset-api-1.0.0.jar
 * Then visit: http://localhost:8080/swagger-ui.html
 */
@SpringBootApplication
public class DatasetApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasetApiApplication.class, args);
        System.out.println("""
                
                ✅ Dataset API started successfully!
                📋 Swagger UI:   http://localhost:8080/swagger-ui.html
                🗄️  H2 Console:   http://localhost:8080/h2-console
                📦 API Base URL: http://localhost:8080/api/dataset
                """);
    }
}
