package com.reliaquest.api.controller;

import com.reliaquest.api.dto.EmployeeDto;
import com.reliaquest.api.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Employee API Controller
 * <p>
 * This controller provides REST endpoints for employee operations.
 * It acts as a proxy to the Mock Employee API running on localhost:8112.
 */
/**
 * @author Naveen Kumar
 */
@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping()
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        log.info("GET /api/v1/employee - Fetching all employees");
        List<EmployeeDto> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/search/{searchString}")
    public ResponseEntity<List<EmployeeDto>> getEmployeesByNameSearch(@PathVariable String searchString) {
        log.info("GET /api/v1/employee/search/{} - Searching employees by name", searchString);
        List<EmployeeDto> employees = employeeService.searchEmployeesByName(searchString);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable String id) {
        log.info("GET /api/v1/employee/{} - Fetching employee by ID", id);
        UUID employeeId = UUID.fromString(id);
        EmployeeDto employee = employeeService.getEmployeeById(employeeId);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/highestSalary")
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        log.info("GET /api/v1/employee/highestSalary - Fetching highest salary");
        Integer highestSalary = employeeService.getHighestSalary();
        return ResponseEntity.ok(highestSalary);
    }

    @GetMapping("/topTenHighestEarningEmployeeNames")
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        log.info("GET /api/v1/employee/topTenHighestEarningEmployeeNames - Fetching top 10 highest earning employees");
        List<String> employeeNames = employeeService.getTop10HighestEarningEmployeeNames();
        return ResponseEntity.ok(employeeNames);
    }

    @PostMapping()
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody EmployeeDto employeeInput) {
        log.info("POST /api/v1/employee - Creating employee with name: {}", employeeInput.getName());
        EmployeeDto createdEmployee = employeeService.createEmployee(employeeInput);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployeeById(@PathVariable String id) {
        log.info("DELETE /api/v1/employee/{} - Deleting employee by ID", id);
        UUID employeeId = UUID.fromString(id);
        String employeeName = employeeService.deleteEmployeeById(employeeId);
        return ResponseEntity.ok(employeeName);
    }
}
