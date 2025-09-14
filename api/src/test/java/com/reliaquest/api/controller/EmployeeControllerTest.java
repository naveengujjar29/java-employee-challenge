package com.reliaquest.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.dto.EmployeeDto;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.MockServerUnavailableException;
import com.reliaquest.api.exception.RateLimitExceededException;
import com.reliaquest.api.service.EmployeeService;
import java.util.Arrays;
import java.util.List;
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
 * Integration tests for EmployeeController
 *
 * @author Naveen Kumar
 */
@WebMvcTest(EmployeeController.class)
@DisplayName("EmployeeController Integration Tests")
class EmployeeControllerTest {

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
    @DisplayName("Should successfully get all employees")
    void testGetAllEmployees_Success() throws Exception {
        // Given
        List<EmployeeDto> employees =
                Arrays.asList(testEmployee, createTestEmployee(UUID.randomUUID(), "Jane Smith", 60000));
        when(employeeService.getAllEmployees()).thenReturn(employees);

        // When & Then
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].salary").value(50000))
                .andExpect(jsonPath("$[1].name").value("Jane Smith"))
                .andExpect(jsonPath("$[1].salary").value(60000));
    }

    @Test
    @DisplayName("Should successfully search employees by name")
    void testSearchEmployeesByName_Success() throws Exception {
        // Given
        String searchString = "John";
        List<EmployeeDto> searchResults = Arrays.asList(testEmployee);
        when(employeeService.searchEmployeesByName(searchString)).thenReturn(searchResults);

        // When & Then
        mockMvc.perform(get("/api/v1/employee/search/{searchString}", searchString))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].salary").value(50000));
    }

    @Test
    @DisplayName("Should successfully get employee by ID")
    void testGetEmployeeById_Success() throws Exception {
        // Given
        when(employeeService.getEmployeeById(testEmployeeId)).thenReturn(testEmployee);

        // When & Then
        mockMvc.perform(get("/api/v1/employee/{id}", testEmployeeId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testEmployeeId.toString()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.salary").value(50000))
                .andExpect(jsonPath("$.age").value(30))
                .andExpect(jsonPath("$.title").value("Developer"));
    }

    @Test
    @DisplayName("Should return 404 when employee not found")
    void testGetEmployeeById_NotFound() throws Exception {
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
    @DisplayName("Should return 400 for invalid UUID format")
    void testGetEmployeeById_InvalidUUID() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/employee/{id}", "invalid-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should successfully get highest salary")
    void testGetHighestSalary_Success() throws Exception {
        // Given
        Integer highestSalary = 75000;
        when(employeeService.getHighestSalary()).thenReturn(highestSalary);

        // When & Then
        mockMvc.perform(get("/api/v1/employee/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(75000));
    }

    @Test
    @DisplayName("Should successfully get top 10 highest earning employee names")
    void testGetTopTenHighestEarningEmployeeNames_Success() throws Exception {
        // Given
        List<String> topEarners = Arrays.asList("Jane Smith", "John Doe", "Bob Johnson");
        when(employeeService.getTop10HighestEarningEmployeeNames()).thenReturn(topEarners);

        // When & Then
        mockMvc.perform(get("/api/v1/employee/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Jane Smith"))
                .andExpect(jsonPath("$[1]").value("John Doe"))
                .andExpect(jsonPath("$[2]").value("Bob Johnson"));
    }

    @Test
    @DisplayName("Should successfully create employee")
    void testCreateEmployee_Success() throws Exception {
        // Given
        EmployeeDto inputEmployee = createTestEmployee(null, "New Employee", 45000);
        EmployeeDto createdEmployee = createTestEmployee(testEmployeeId, "New Employee", 45000);

        when(employeeService.createEmployee(any(EmployeeDto.class))).thenReturn(createdEmployee);

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputEmployee)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testEmployeeId.toString()))
                .andExpect(jsonPath("$.name").value("New Employee"))
                .andExpect(jsonPath("$.salary").value(45000));
    }

    @Test
    @DisplayName("Should return 400 for invalid employee data")
    void testCreateEmployee_InvalidData() throws Exception {
        // Given
        EmployeeDto invalidEmployee = new EmployeeDto();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 400 for negative salary")
    void testCreateEmployee_NegativeSalary() throws Exception {
        // Given
        EmployeeDto invalidEmployee = createTestEmployee(null, "Test Employee", -1000);

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 400 for invalid age")
    void testCreateEmployee_InvalidAge() throws Exception {
        // Given
        EmployeeDto invalidEmployee = createTestEmployee(null, "Test Employee", 50000);
        invalidEmployee.setAge(15); // Below minimum age

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 400 for blank name")
    void testCreateEmployee_BlankName() throws Exception {
        // Given
        EmployeeDto invalidEmployee = createTestEmployee(null, "", 50000);

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should successfully delete employee by ID")
    void testDeleteEmployeeById_Success() throws Exception {
        // Given
        String employeeName = "John Doe";
        when(employeeService.deleteEmployeeById(testEmployeeId)).thenReturn(employeeName);

        // When & Then
        mockMvc.perform(delete("/api/v1/employee/{id}", testEmployeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("John Doe"));
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent employee")
    void testDeleteEmployeeById_NotFound() throws Exception {
        // Given
        when(employeeService.deleteEmployeeById(testEmployeeId))
                .thenThrow(new EmployeeNotFoundException("Employee not found"));

        // When & Then
        mockMvc.perform(delete("/api/v1/employee/{id}", testEmployeeId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Employee not found"));
    }

    @Test
    @DisplayName("Should return 400 for invalid UUID in delete request")
    void testDeleteEmployeeById_InvalidUUID() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/employee/{id}", "invalid-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 503 when mock server is unavailable")
    void testGetAllEmployees_ServiceUnavailable() throws Exception {
        // Given
        when(employeeService.getAllEmployees())
                .thenThrow(new MockServerUnavailableException("Mock server is unavailable"));

        // When & Then
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Mock server is unavailable. Please try again later."));
    }

    @Test
    @DisplayName("Should return 429 when rate limit is exceeded")
    void testGetAllEmployees_RateLimitExceeded() throws Exception {
        // Given
        when(employeeService.getAllEmployees()).thenThrow(new RateLimitExceededException("Rate limit exceeded"));

        // When & Then
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Rate limit exceeded. Please try again later."));
    }

    @Test
    @DisplayName("Should handle cache bypass header")
    void testGetAllEmployees_WithCacheBypassHeader() throws Exception {
        // Given
        List<EmployeeDto> employees = Arrays.asList(testEmployee);
        when(employeeService.getAllEmployees()).thenReturn(employees);

        // When & Then
        mockMvc.perform(get("/api/v1/employee").header("X-Cache-Bypass", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
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
