package com.reliaquest.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.reliaquest.api.dto.ApiResponse;
import com.reliaquest.api.dto.EmployeeDto;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.MockServerUnavailableException;
import com.reliaquest.api.exception.RateLimitExceededException;
import com.reliaquest.api.model.ServerCreateEmployeeDto;
import com.reliaquest.api.model.ServerEmployeeDto;
import com.reliaquest.api.util.RestTemplateUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for EmployeeService
 *
 * @author Naveen Kumar
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService Unit Tests")
class EmployeeServiceTest {

    @Mock
    private RestTemplateUtil restTemplateUtil;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private EmployeeService employeeService;

    private final String mockApiBaseUrl = "http://localhost:8112";
    private final UUID testEmployeeId = UUID.randomUUID();
    private final String testEmployeeName = "John Doe";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(employeeService, "mockApiBaseUrl", mockApiBaseUrl);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }

    @Test
    @DisplayName("Should successfully get all employees from cache")
    void testGetAllEmployees_Success() {
        // Given
        ServerEmployeeDto serverEmployee1 = createServerEmployee(testEmployeeId, testEmployeeName, 50000);
        ServerEmployeeDto serverEmployee2 = createServerEmployee(UUID.randomUUID(), "Jane Smith", 60000);
        List<ServerEmployeeDto> serverEmployees = Arrays.asList(serverEmployee1, serverEmployee2);

        EmployeeDto employeeDto1 = createEmployeeDto(testEmployeeId, testEmployeeName, 50000);
        EmployeeDto employeeDto2 = createEmployeeDto(UUID.randomUUID(), "Jane Smith", 60000);

        ApiResponse<List<ServerEmployeeDto>> apiResponse = new ApiResponse<>();
        apiResponse.setData(serverEmployees);

        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));
        when(modelMapper.map(serverEmployee1, EmployeeDto.class)).thenReturn(employeeDto1);
        when(modelMapper.map(serverEmployee2, EmployeeDto.class)).thenReturn(employeeDto2);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When
        List<EmployeeDto> result = employeeService.getAllEmployees();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testEmployeeName, result.get(0).getName());
        assertEquals(50000, result.get(0).getSalary());
        verify(restTemplateUtil, times(1)).get(anyString(), any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should bypass cache when X-Cache-Bypass header is true")
    void testGetAllEmployees_WithCacheBypass() {
        // Given
        ServerEmployeeDto serverEmployee = createServerEmployee(testEmployeeId, testEmployeeName, 50000);
        EmployeeDto employeeDto = createEmployeeDto(testEmployeeId, testEmployeeName, 50000);

        ApiResponse<List<ServerEmployeeDto>> apiResponse = new ApiResponse<>();
        apiResponse.setData(Arrays.asList(serverEmployee));

        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));
        when(modelMapper.map(serverEmployee, EmployeeDto.class)).thenReturn(employeeDto);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn("true");

        // When
        List<EmployeeDto> result = employeeService.getAllEmployees();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(employeeService.shouldBypassCache());
        verify(restTemplateUtil, times(1)).get(anyString(), any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should return empty list when API returns null data")
    void testGetAllEmployees_EmptyResponse() {
        // Given
        ApiResponse<List<ServerEmployeeDto>> apiResponse = new ApiResponse<>();
        apiResponse.setData(null);

        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When
        List<EmployeeDto> result = employeeService.getAllEmployees();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw MockServerUnavailableException when server is unreachable")
    void testGetAllEmployees_ServerUnavailable() {
        // Given
        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When & Then
        MockServerUnavailableException exception =
                assertThrows(MockServerUnavailableException.class, () -> employeeService.getAllEmployees());

        assertEquals("Mock server is unavailable. Please try again later.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw RateLimitExceededException when rate limit is exceeded")
    void testGetAllEmployees_RateLimitExceeded() {
        // Given
        HttpClientErrorException rateLimitException = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);
        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenThrow(rateLimitException);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When & Then
        RateLimitExceededException exception =
                assertThrows(RateLimitExceededException.class, () -> employeeService.getAllEmployees());

        assertEquals("Server is currently rate limiting requests. Please try again later.", exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully search employees by name")
    void testSearchEmployeesByName_Success() {
        // Given
        String searchString = "John";
        ServerEmployeeDto serverEmployee1 = createServerEmployee(testEmployeeId, "John Doe", 50000);
        ServerEmployeeDto serverEmployee2 = createServerEmployee(UUID.randomUUID(), "Jane Smith", 60000);
        List<ServerEmployeeDto> serverEmployees = Arrays.asList(serverEmployee1, serverEmployee2);

        EmployeeDto employeeDto1 = createEmployeeDto(testEmployeeId, "John Doe", 50000);
        EmployeeDto employeeDto2 = createEmployeeDto(UUID.randomUUID(), "Jane Smith", 60000);

        ApiResponse<List<ServerEmployeeDto>> apiResponse = new ApiResponse<>();
        apiResponse.setData(serverEmployees);

        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));
        when(modelMapper.map(serverEmployee1, EmployeeDto.class)).thenReturn(employeeDto1);
        when(modelMapper.map(serverEmployee2, EmployeeDto.class)).thenReturn(employeeDto2);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When
        List<EmployeeDto> result = employeeService.searchEmployeesByName(searchString);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
    }

    @Test
    @DisplayName("Should perform case-insensitive search")
    void testSearchEmployeesByName_CaseInsensitive() {
        // Given
        String searchString = "john";
        ServerEmployeeDto serverEmployee = createServerEmployee(testEmployeeId, "John Doe", 50000);
        EmployeeDto employeeDto = createEmployeeDto(testEmployeeId, "John Doe", 50000);

        ApiResponse<List<ServerEmployeeDto>> apiResponse = new ApiResponse<>();
        apiResponse.setData(Arrays.asList(serverEmployee));

        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));
        when(modelMapper.map(serverEmployee, EmployeeDto.class)).thenReturn(employeeDto);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When
        List<EmployeeDto> result = employeeService.searchEmployeesByName(searchString);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
    }

    @Test
    @DisplayName("Should successfully get employee by ID")
    void testGetEmployeeById_Success() {
        // Given
        ServerEmployeeDto serverEmployee = createServerEmployee(testEmployeeId, testEmployeeName, 50000);
        EmployeeDto employeeDto = createEmployeeDto(testEmployeeId, testEmployeeName, 50000);

        ApiResponse<ServerEmployeeDto> apiResponse = new ApiResponse<>();
        apiResponse.setData(serverEmployee);

        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));
        when(modelMapper.map(serverEmployee, EmployeeDto.class)).thenReturn(employeeDto);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When
        EmployeeDto result = employeeService.getEmployeeById(testEmployeeId);

        // Then
        assertNotNull(result);
        assertEquals(testEmployeeId, result.getId());
        assertEquals(testEmployeeName, result.getName());
        assertEquals(50000, result.getSalary());
    }

    @Test
    @DisplayName("Should throw EmployeeNotFoundException when employee not found")
    void testGetEmployeeById_NotFound() {
        // Given
        HttpClientErrorException notFoundException =
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Employee not found", null, null, null);
        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenThrow(notFoundException);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When & Then
        EmployeeNotFoundException exception =
                assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(testEmployeeId));

        assertEquals("Employee not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully get highest salary")
    void testGetHighestSalary_Success() {
        // Given
        ServerEmployeeDto serverEmployee1 = createServerEmployee(testEmployeeId, "John Doe", 50000);
        ServerEmployeeDto serverEmployee2 = createServerEmployee(UUID.randomUUID(), "Jane Smith", 75000);
        List<ServerEmployeeDto> serverEmployees = Arrays.asList(serverEmployee1, serverEmployee2);

        EmployeeDto employeeDto1 = createEmployeeDto(testEmployeeId, "John Doe", 50000);
        EmployeeDto employeeDto2 = createEmployeeDto(UUID.randomUUID(), "Jane Smith", 75000);

        ApiResponse<List<ServerEmployeeDto>> apiResponse = new ApiResponse<>();
        apiResponse.setData(serverEmployees);

        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));
        when(modelMapper.map(serverEmployee1, EmployeeDto.class)).thenReturn(employeeDto1);
        when(modelMapper.map(serverEmployee2, EmployeeDto.class)).thenReturn(employeeDto2);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When
        Integer result = employeeService.getHighestSalary();

        // Then
        assertNotNull(result);
        assertEquals(75000, result);
    }

    @Test
    @DisplayName("Should return 0 when no employees have salary")
    void testGetHighestSalary_NoSalaries() {
        // Given
        ServerEmployeeDto serverEmployee = createServerEmployee(testEmployeeId, testEmployeeName, null);
        EmployeeDto employeeDto = createEmployeeDto(testEmployeeId, testEmployeeName, null);

        ApiResponse<List<ServerEmployeeDto>> apiResponse = new ApiResponse<>();
        apiResponse.setData(Arrays.asList(serverEmployee));

        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));
        when(modelMapper.map(serverEmployee, EmployeeDto.class)).thenReturn(employeeDto);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When
        Integer result = employeeService.getHighestSalary();

        // Then
        assertNotNull(result);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should successfully get top 10 highest earning employee names")
    void testGetTop10HighestEarningEmployeeNames_Success() {
        // Given
        ServerEmployeeDto serverEmployee1 = createServerEmployee(testEmployeeId, "John Doe", 50000);
        ServerEmployeeDto serverEmployee2 = createServerEmployee(UUID.randomUUID(), "Jane Smith", 75000);
        ServerEmployeeDto serverEmployee3 = createServerEmployee(UUID.randomUUID(), "Bob Johnson", 60000);
        List<ServerEmployeeDto> serverEmployees = Arrays.asList(serverEmployee1, serverEmployee2, serverEmployee3);

        EmployeeDto employeeDto1 = createEmployeeDto(testEmployeeId, "John Doe", 50000);
        EmployeeDto employeeDto2 = createEmployeeDto(UUID.randomUUID(), "Jane Smith", 75000);
        EmployeeDto employeeDto3 = createEmployeeDto(UUID.randomUUID(), "Bob Johnson", 60000);

        ApiResponse<List<ServerEmployeeDto>> apiResponse = new ApiResponse<>();
        apiResponse.setData(serverEmployees);

        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));
        when(modelMapper.map(serverEmployee1, EmployeeDto.class)).thenReturn(employeeDto1);
        when(modelMapper.map(serverEmployee2, EmployeeDto.class)).thenReturn(employeeDto2);
        when(modelMapper.map(serverEmployee3, EmployeeDto.class)).thenReturn(employeeDto3);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When
        List<String> result = employeeService.getTop10HighestEarningEmployeeNames();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Jane Smith", result.get(0)); // Highest salary
        assertEquals("Bob Johnson", result.get(1)); // Second highest
        assertEquals("John Doe", result.get(2)); // Lowest salary
    }

    @Test
    @DisplayName("Should successfully create employee")
    void testCreateEmployee_Success() {
        // Given
        EmployeeDto inputEmployee = createEmployeeDto(null, testEmployeeName, 50000);
        ServerCreateEmployeeDto serverInput = new ServerCreateEmployeeDto();
        serverInput.setName(testEmployeeName);
        serverInput.setSalary(50000);

        ServerEmployeeDto serverResponse = createServerEmployee(testEmployeeId, testEmployeeName, 50000);
        EmployeeDto expectedResponse = createEmployeeDto(testEmployeeId, testEmployeeName, 50000);

        ApiResponse<ServerEmployeeDto> apiResponse = new ApiResponse<>();
        apiResponse.setData(serverResponse);

        when(modelMapper.map(inputEmployee, ServerCreateEmployeeDto.class)).thenReturn(serverInput);
        when(restTemplateUtil.post(
                        anyString(), any(ServerCreateEmployeeDto.class), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.CREATED));
        when(modelMapper.map(serverResponse, EmployeeDto.class)).thenReturn(expectedResponse);

        // When
        EmployeeDto result = employeeService.createEmployee(inputEmployee);

        // Then
        assertNotNull(result);
        assertEquals(testEmployeeId, result.getId());
        assertEquals(testEmployeeName, result.getName());
        assertEquals(50000, result.getSalary());
    }

    @Test
    @DisplayName("Should successfully delete employee by ID")
    void testDeleteEmployeeById_Success() {
        // Given
        ServerEmployeeDto serverEmployee = createServerEmployee(testEmployeeId, testEmployeeName, 50000);
        EmployeeDto employeeDto = createEmployeeDto(testEmployeeId, testEmployeeName, 50000);

        ApiResponse<ServerEmployeeDto> getResponse = new ApiResponse<>();
        getResponse.setData(serverEmployee);

        ApiResponse<Boolean> deleteResponse = new ApiResponse<>();
        deleteResponse.setData(true);

        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(getResponse, HttpStatus.OK));
        when(modelMapper.map(serverEmployee, EmployeeDto.class)).thenReturn(employeeDto);
        when(restTemplateUtil.delete(anyString(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(deleteResponse, HttpStatus.OK));
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When
        String result = employeeService.deleteEmployeeById(testEmployeeId);

        // Then
        assertNotNull(result);
        assertEquals(testEmployeeName, result);
    }

    @Test
    @DisplayName("Should throw EmployeeNotFoundException when deleting non-existent employee")
    void testDeleteEmployeeById_NotFound() {
        // Given
        HttpClientErrorException notFoundException =
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Employee not found", null, null, null);
        when(restTemplateUtil.get(anyString(), any(ParameterizedTypeReference.class)))
                .thenThrow(notFoundException);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When & Then
        EmployeeNotFoundException exception =
                assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployeeById(testEmployeeId));

        assertEquals("Employee not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should return false when cache bypass header is not present")
    void testShouldBypassCache_NoHeader() {
        // Given
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn(null);

        // When
        boolean result = employeeService.shouldBypassCache();

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true when cache bypass header is 'true'")
    void testShouldBypassCache_HeaderTrue() {
        // Given
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn("true");

        // When
        boolean result = employeeService.shouldBypassCache();

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when cache bypass header is 'false'")
    void testShouldBypassCache_HeaderFalse() {
        // Given
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Cache-Bypass")).thenReturn("false");

        // When
        boolean result = employeeService.shouldBypassCache();

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle exception when request context is not available")
    void testShouldBypassCache_NoRequestContext() {
        // Given
        RequestContextHolder.resetRequestAttributes();

        // When
        boolean result = employeeService.shouldBypassCache();

        // Then
        assertFalse(result);
    }

    // Helper methods
    private ServerEmployeeDto createServerEmployee(UUID id, String name, Integer salary) {
        ServerEmployeeDto employee = new ServerEmployeeDto();
        employee.setId(id);
        employee.setName(name);
        employee.setSalary(salary);
        employee.setAge(30);
        employee.setTitle("Developer");
        return employee;
    }

    private EmployeeDto createEmployeeDto(UUID id, String name, Integer salary) {
        EmployeeDto employee = new EmployeeDto();
        employee.setId(id);
        employee.setName(name);
        employee.setSalary(salary);
        employee.setAge(30);
        employee.setTitle("Developer");
        return employee;
    }
}
