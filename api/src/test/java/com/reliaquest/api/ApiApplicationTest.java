package com.reliaquest.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test to verify Spring Boot application context loads successfully
 *
 * @author Naveen Kumar
 */
@SpringBootTest
@ActiveProfiles("test")
class ApiApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring Boot application context loads
        // successfully
        // If there are any configuration issues, this test will fail
        assertTrue(true, "Application context should load successfully");
    }

    @Test
    void applicationStarts() {
        // This test verifies that the application can start without errors
        // It's a basic smoke test to ensure all beans are properly configured
        assertTrue(true, "Application should start successfully");
    }
}
