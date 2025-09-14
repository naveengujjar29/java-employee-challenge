package com.reliaquest.api.service;

import com.reliaquest.api.dto.ApiResponse;
import com.reliaquest.api.dto.EmployeeDto;
import com.reliaquest.api.exception.MockServerUnavailableException;
import com.reliaquest.api.exception.RateLimitExceededException;
import com.reliaquest.api.model.ServerCreateEmployeeDto;
import com.reliaquest.api.model.ServerEmployeeDto;
import com.reliaquest.api.util.RestTemplateUtil;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final RestTemplateUtil restTemplateUtil;
    private final String mockApiBaseUrl;
    private final ModelMapper modelMapper;

    public EmployeeService(RestTemplateUtil restTemplateUtil,
                           @Value("${mock.api.base-url:http://localhost:8112}") String mockApiBaseUrl,
                           ModelMapper modelMapper) {
        this.restTemplateUtil = restTemplateUtil;
        this.mockApiBaseUrl = mockApiBaseUrl;
        this.modelMapper = modelMapper;
    }

    /**
     * Get all employees from the mock API Server
     */
    @Retryable(value = {
            HttpClientErrorException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
    @Cacheable("employees")
    public List<EmployeeDto> getAllEmployees() {
        log.info("Fetching all employees from mock API Server");
        try {
            String url = mockApiBaseUrl + "/api/v1/employee";

            ResponseEntity<ApiResponse<List<ServerEmployeeDto>>> response = restTemplateUtil.get(
                    url,
                    new ParameterizedTypeReference<ApiResponse<List<ServerEmployeeDto>>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                log.info("Successfully fetched {} employees from mock API Server", response.getBody().getData().size());
                return response.getBody().getData().parallelStream()
                        .map(serverEmployee -> modelMapper.map(serverEmployee, EmployeeDto.class))
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (HttpClientErrorException e) {
            handleRateLimitException(e);
            throw new RuntimeException("Failed to fetch employees from mock API Server", e);
        } catch (ResourceAccessException e) {
            throw new MockServerUnavailableException("Mock server is unavailable. Please try again later.", e);
        } catch (MockServerUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching employees from mock API Server", e);
            throw new RuntimeException("Failed to fetch employees from mock API Server", e);
        }
    }

    /**
     * Search employees by name
     */
    @Retryable(value = {
            HttpClientErrorException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
    @Cacheable(value = "employeeSearch", key = "#searchString")
    public List<EmployeeDto> searchEmployeesByName(String searchString) {
        log.info("Searching employees by name: {}", searchString);
        List<EmployeeDto> allEmployees = getAllEmployees();

        List<EmployeeDto> filteredEmployees = allEmployees.parallelStream()
                .filter(employee -> employee.getName() != null &&
                        employee.getName().toLowerCase().contains(searchString.toLowerCase()))
                .collect(Collectors.toList());

        log.info("Found {} employees matching search string: {}", filteredEmployees.size(), searchString);
        return filteredEmployees;
    }

    /**
     * Get employee by ID from the mock API Server
     */
    @Retryable(value = {
            HttpClientErrorException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
    @Cacheable(value = "employeeById", key = "#id")
    public EmployeeDto getEmployeeById(UUID id) {
        log.info("Fetching employee with ID: {} from mock API Server", id);
        try {
            String url = mockApiBaseUrl + "/api/v1/employee/" + id;

            ResponseEntity<ApiResponse<ServerEmployeeDto>> response = restTemplateUtil.get(
                    url,
                    new ParameterizedTypeReference<ApiResponse<ServerEmployeeDto>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                log.info("Successfully fetched employee with ID: {} from mock API Server", id);
                return modelMapper.map(response.getBody().getData(), EmployeeDto.class);
            }
            throw new RuntimeException("Employee not found in mock API Server");
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Employee with ID: {} not found in mock API Server", id);
            throw new RuntimeException("Employee not found", e);
        } catch (HttpClientErrorException e) {
            handleRateLimitException(e);
            throw new RuntimeException("Failed to fetch employee from mock API Server", e);
        } catch (ResourceAccessException e) {
            throw new MockServerUnavailableException("Mock server is unavailable. Please try again later.", e);
        } catch (MockServerUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching employee with ID: {} from mock API Server", id, e);
            throw new RuntimeException("Failed to fetch employee from mock API Server", e);
        }
    }

    /**
     * Get highest salary among all employees
     */
    @Retryable(value = {
            HttpClientErrorException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
    @Cacheable("highestSalary")
    public Integer getHighestSalary() {
        log.info("Finding highest salary among all employees");
        List<EmployeeDto> allEmployees = getAllEmployees();

        Integer highestSalary = allEmployees.parallelStream()
                .filter(employee -> employee.getSalary() != null)
                .mapToInt(EmployeeDto::getSalary)
                .max()
                .orElse(0);

        log.info("Highest salary found: {}", highestSalary);
        return highestSalary;
    }

    /**
     * Get top 10 highest earning employee names
     */
    @Retryable(value = {
            HttpClientErrorException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
    @Cacheable("top10HighestEarningEmployeeNames")
    public List<String> getTop10HighestEarningEmployeeNames() {
        log.info("Finding top 10 highest earning employees");
        List<EmployeeDto> allEmployees = getAllEmployees();

        List<String> topEmployeeNames = allEmployees.parallelStream()
                .filter(employee -> employee.getSalary() != null && employee.getName() != null)
                .sorted((e1, e2) -> Integer.compare(e2.getSalary(), e1.getSalary()))
                .limit(10)
                .map(EmployeeDto::getName)
                .collect(Collectors.toList());

        log.info("Found {} top earning employees", topEmployeeNames.size());
        return topEmployeeNames;
    }

    /**
     * Create employee in the mock API Server
     */
    @Retryable(value = {
            HttpClientErrorException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
    @CacheEvict(value = {"employees", "employeeSearch", "employeeById", "highestSalary", "top10HighestEarningEmployeeNames"}, allEntries = true)
    public EmployeeDto createEmployee(EmployeeDto input) {
        try {
            log.info("Creating employee with name: {} in mock API Server", input.getName());
            String url = mockApiBaseUrl + "/api/v1/employee";

            // Convert API format to server format using ModelMapper
            ServerCreateEmployeeDto serverInput = modelMapper.map(input, ServerCreateEmployeeDto.class);

            ResponseEntity<ApiResponse<ServerEmployeeDto>> response = restTemplateUtil.post(
                    url,
                    serverInput,
                    new ParameterizedTypeReference<ApiResponse<ServerEmployeeDto>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                log.info("Successfully created employee with name: {} in mock API Server", input.getName());
                // Convert server response back to API format using ModelMapper
                return modelMapper.map(response.getBody().getData(), EmployeeDto.class);
            }
            throw new RuntimeException("Failed to create employee in mock API Server");
        } catch (HttpClientErrorException e) {
            handleRateLimitException(e);
            throw new RuntimeException("Failed to create employee in mock API Server", e);
        } catch (ResourceAccessException e) {
            throw new MockServerUnavailableException("Mock server is unavailable. Please try again later.", e);
        } catch (MockServerUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating employee in mock API Server", e);
            throw new RuntimeException("Failed to create employee in mock API Server", e);
        }
    }

    /**
     * Delete employee by ID
     */
    @Retryable(value = {
            HttpClientErrorException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
    @CacheEvict(value = {"employees", "employeeSearch", "employeeById", "highestSalary", "top10HighestEarningEmployeeNames"}, allEntries = true)
    public String deleteEmployeeById(UUID id) {

        log.info("Deleting employee with ID: {}", id);

        // First get the employee to find their name
        EmployeeDto employee = getEmployeeById(id);
        String employeeName = employee.getName();

        // Then delete by name
        boolean deleted = deleteEmployeeByName(employeeName);

        if (deleted) {
            log.info("Successfully deleted employee with ID: {} and name: {}", id, employeeName);
            return employeeName;
        } else {
            throw new RuntimeException("Failed to delete employee");
        }
    }

    /**
     * Delete employee by name from the mock API Server
     */
    @Retryable(value = {
            HttpClientErrorException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
    private boolean deleteEmployeeByName(String name) {
        try {
            log.info("Deleting employee with name: {} from mock API Server", name);
            String url = mockApiBaseUrl + "/api/v1/employee/" + name;

            ResponseEntity<ApiResponse<Boolean>> response = restTemplateUtil.delete(
                    url,
                    new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                boolean deleted = response.getBody().getData();
                log.info("Successfully deleted employee with name: {} from mock API Server", name);
                return deleted;
            }
            return false;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Employee with name: {} not found in mock API Server", name);
            throw new RuntimeException("Employee not found", e);
        } catch (HttpClientErrorException e) {
            handleRateLimitException(e);
            throw new RuntimeException("Failed to delete employee from mock API Server", e);
        } catch (ResourceAccessException e) {
            throw new MockServerUnavailableException("Mock server is unavailable. Please try again later.", e);
        } catch (MockServerUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting employee with name: {} from mock API Server", name, e);
            throw new RuntimeException("Failed to delete employee from mock API Server", e);
        }
    }

    /**
     * Handle rate limiting exceptions and convert them to our custom exception
     */
    private void handleRateLimitException(HttpClientErrorException ex) {
        if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            log.warn("Rate limit exceeded from server: {}", ex.getMessage());
            throw new RateLimitExceededException("Server is currently rate limiting requests. Please try again later.",
                    ex);
        }
        throw ex;
    }

}
