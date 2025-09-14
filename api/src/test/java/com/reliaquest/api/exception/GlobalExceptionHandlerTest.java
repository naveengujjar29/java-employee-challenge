package com.reliaquest.api.exception;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.controller.EmployeeController;
import com.reliaquest.api.dto.EmployeeDto;
import com.reliaquest.api.service.EmployeeService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for GlobalExceptionHandler
 *
 * @author Naveen Kumar
 */
@WebMvcTest(EmployeeController.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testEmployeeId;
    private EmployeeDto testEmployee;

    @BeforeEach
    void setUp() {
        testEmployeeId = UUID.randomUUID();
        testEmployee = createTestEmployee(testEmployeeId, "John Doe", 50000);
    }

    @Test
    @DisplayName("Should handle EmployeeNotFoundException with 404 status")
    void testHandleEmployeeNotFoundException() throws Exception {
        // Given
        when(employeeService.getEmployeeById(testEmployeeId))
                .thenThrow(new EmployeeNotFoundException("Employee not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/employee/{id}", testEmployeeId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Employee not found"));
    }

    @Test
    @DisplayName("Should handle RateLimitExceededException with 429 status")
    void testHandleRateLimitExceededException() throws Exception {
        // Given
        RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded");
        when(employeeService.getAllEmployees()).thenThrow(exception);

        // When & Then
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Rate limit exceeded. Please try again later."))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded"));
    }

    @Test
    @DisplayName("Should handle RateLimitExceededException with retry after seconds")
    void testHandleRateLimitExceededExceptionWithRetryAfter() throws Exception {
        // Given
        RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded", 60);
        when(employeeService.getAllEmployees()).thenThrow(exception);

        // When & Then
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Rate limit exceeded. Please try again later."))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded"))
                .andExpect(jsonPath("$.retryAfterSeconds").value("60"));
    }

    @Test
    @DisplayName("Should handle MockServerUnavailableException with 503 status")
    void testHandleMockServerUnavailableException() throws Exception {
        // Given
        when(employeeService.getAllEmployees())
                .thenThrow(new MockServerUnavailableException("Mock server is unavailable"));

        // When & Then
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Mock server is unavailable. Please try again later."))
                .andExpect(jsonPath("$.message").value("Mock server is unavailable"));
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400 status")
    void testHandleIllegalArgumentException() throws Exception {
        // Given
        when(employeeService.getEmployeeById(testEmployeeId))
                .thenThrow(new IllegalArgumentException("Invalid employee ID format"));

        // When & Then
        mockMvc.perform(get("/api/v1/employee/{id}", testEmployeeId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid employee ID format"));
    }

    @Test
    @DisplayName("Should handle RuntimeException with 500 status")
    void testHandleRuntimeException() throws Exception {
        // Given
        when(employeeService.getAllEmployees()).thenThrow(new RuntimeException("Unexpected server error"));

        // When & Then
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unexpected server error"));
    }

    @Test
    @DisplayName("Should handle validation errors for invalid employee data")
    void testHandleValidationErrors() throws Exception {
        // Given
        EmployeeDto invalidEmployee = new EmployeeDto();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.salary").exists())
                .andExpect(jsonPath("$.age").exists())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @DisplayName("Should handle validation errors for negative salary")
    void testHandleValidationErrorsNegativeSalary() throws Exception {
        // Given
        EmployeeDto invalidEmployee = createTestEmployee(null, "Test Employee", -1000);

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.salary").value("Employee salary must be greater than zero"));
    }

    @Test
    @DisplayName("Should handle validation errors for invalid age")
    void testHandleValidationErrorsInvalidAge() throws Exception {
        // Given
        EmployeeDto invalidEmployee = createTestEmployee(null, "Test Employee", 50000);
        invalidEmployee.setAge(15); // Below minimum age

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.age").value("Employee age must be at least 16"));
    }

    @Test
    @DisplayName("Should handle validation errors for blank name")
    void testHandleValidationErrorsBlankName() throws Exception {
        // Given
        EmployeeDto invalidEmployee = createTestEmployee(null, "", 50000);

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Employee name cannot be blank"));
    }

    @Test
    @DisplayName("Should handle validation errors for blank title")
    void testHandleValidationErrorsBlankTitle() throws Exception {
        // Given
        EmployeeDto invalidEmployee = createTestEmployee(null, "Test Employee", 50000);
        invalidEmployee.setTitle("");

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Employee title cannot be blank"));
    }

    @Test
    @DisplayName("Should handle validation errors for null salary")
    void testHandleValidationErrorsNullSalary() throws Exception {
        // Given
        EmployeeDto invalidEmployee = createTestEmployee(null, "Test Employee", null);

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.salary").value("Employee salary cannot be null"));
    }

    @Test
    @DisplayName("Should handle validation errors for null age")
    void testHandleValidationErrorsNullAge() throws Exception {
        // Given
        EmployeeDto invalidEmployee = createTestEmployee(null, "Test Employee", 50000);
        invalidEmployee.setAge(null);

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.age").value("Employee age cannot be null"));
    }

    @Test
    @DisplayName("Should handle validation errors for age above maximum")
    void testHandleValidationErrorsAgeAboveMaximum() throws Exception {
        // Given
        EmployeeDto invalidEmployee = createTestEmployee(null, "Test Employee", 50000);
        invalidEmployee.setAge(80); // Above maximum age

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.age").value("Employee age must be at most 75"));
    }

    @Test
    @DisplayName("Should handle malformed JSON request")
    void testHandleMalformedJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Malformed JSON request"));
    }

    @Test
    @DisplayName("Should handle unsupported media type")
    void testHandleUnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unsupported media type"));
    }

    // Helper method
    private EmployeeDto createTestEmployee(UUID id, String name, Integer salary) {
        EmployeeDto employee = new EmployeeDto();
        employee.setId(id);
        employee.setName(name);
        employee.setSalary(salary);
        employee.setAge(30);
        employee.setTitle("Developer");
        employee.setEmail("test@example.com");
        return employee;
    }
}
