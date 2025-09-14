# Employee API Service

## Overview
This project is a Spring Boot-based REST API for employee management, acting as a proxy to a third-party Mock Employee API.

## Technology Stack
- **Framework:** Spring Boot 3.x
- **Language:** Java 17+
- **Build Tool:** Gradle
- **Caching:** Caffeine Cache with Spring Cache Abstraction
- **HTTP Client:** RestTemplate with Apache HttpClient5
- **Validation:** Bean Validation (Jakarta)
- **Object Mapping:** ModelMapper
- **Retry Logic:** Spring Retry with exponential backoff
- **Logging:** SLF4J with Logback

## Architecture & Design Patterns

### System Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client App    │───▶│  Employee API   │───▶│  Mock API       │
│                 │    │  (Port 8111)    │    │  (Port 8112)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │  Caffeine Cache │
                       │  (1 min TTL)    │
                       └─────────────────┘
```

### Design Patterns Implemented
- **Proxy Pattern:** Acts as a proxy to the Mock Employee API
- **Repository Pattern:** Service layer abstracts data access
- **DTO Pattern:** Data Transfer Objects for API contracts
- **Strategy Pattern:** Conditional caching based on request headers
- **Circuit Breaker Pattern:** Retry mechanism with exponential backoff
- **Exception Translation:** Custom exceptions with proper HTTP status codes

## Design Considerations & Challenges

### 1. **Conditional Caching Based on Request Header**
- **Challenge:** Allow clients to bypass cache and always fetch fresh data from the Mock API server.
- **Solution:** Used Spring's `@Cacheable` annotation with a `condition` that checks for a custom request header (`X-Cache-Bypass`). If the header is present and set to `true`, caching is skipped for that request.
- **Implementation:** Dynamic cache bypass using `RequestContextHolder` to access HTTP headers in service layer.

### 2. **Preventing Caching of Invalid/Failed Responses**
- **Challenge:** Avoid caching null, empty, or error responses (e.g., when the Mock API is down or returns an error).
- **Solution:** Used the `unless` attribute in `@Cacheable` to prevent caching when the result is null, empty, or otherwise invalid.
- **Benefit:** Ensures cache integrity and prevents serving stale error responses.

### 3. **Scalability & Performance**
- **Challenge:** Handle large datasets and high concurrency without performance degradation.
- **Solution:**
  - Used Caffeine cache with a 1-minute TTL and size limits (1000 entries)
  - Designed all endpoints to be stateless and thread-safe
  - Used parallel streams for data processing to minimize memory footprint
  - Implemented connection pooling with configurable timeouts

### 4. **Comprehensive Error Handling**
- **Challenge:** Ensure that exceptions (network errors, API failures) do not pollute the cache or break the API contract.
- **Solution:**
  - Centralized exception handling using `@RestControllerAdvice`
  - Custom exception hierarchy with proper HTTP status codes
  - Only successful responses are cached; exceptions are propagated and not cached
  - Retry mechanism with exponential backoff for transient failures

### 5. **API Extensibility & Maintainability**
- **Challenge:** Make it easy to add new endpoints or change caching logic.
- **Solution:**
  - Used Spring's annotation-based caching and dependency injection
  - Kept controller and service layers clean and decoupled
  - ModelMapper for flexible object mapping
  - Configuration externalization for easy environment-specific changes

### 6. **Production Readiness**
- **Features Implemented:**
  - Input validation with comprehensive error messages
  - Structured logging with appropriate log levels
  - Configurable timeouts and connection settings
  - Rate limiting detection and handling
  - Health check endpoints (via Spring Boot Actuator)

## API Endpoints & Documentation

### Base URL
```
http://localhost:8111/api/v1/employee
```

### Authentication
Currently, no authentication is required. All endpoints are publicly accessible.

### Rate Limiting
The API respects rate limiting from the upstream Mock API server. When rate limits are exceeded, the API returns HTTP 429 with retry information.

### Caching Strategy
- **Default Behavior:** All GET endpoints are cached for 1 minute
- **Cache Bypass:** Add header `X-Cache-Bypass: true` to fetch fresh data
- **Cache Invalidation:** POST and DELETE operations automatically invalidate related caches

### API Endpoints

#### 1. Get All Employees
Retrieves a list of all employees from the system.

**Endpoint:** `GET /api/v1/employee`

**Response:** Array of Employee objects

**Example:**
```bash
curl -X GET "http://localhost:8111/api/v1/employee"
```

**Bypass cache:**
```bash
curl -X GET "http://localhost:8111/api/v1/employee" -H "X-Cache-Bypass: true"
```

#### 2. Search Employees by Name
Searches for employees whose names contain the specified search string (case-insensitive).

**Endpoint:** `GET /api/v1/employee/search/{searchString}`

**Parameters:**
- `searchString` (path): The name or part of name to search for

**Example:**
```bash
curl -X GET "http://localhost:8111/api/v1/employee/search/John"
```

#### 3. Get Employee by ID
Retrieves a specific employee by their unique identifier.

**Endpoint:** `GET /api/v1/employee/{id}`

**Parameters:**
- `id` (path): UUID of the employee

**Example:**
```bash
curl -X GET "http://localhost:8111/api/v1/employee/123e4567-e89b-12d3-a456-426614174000"
```

#### 4. Get Highest Salary
Returns the highest salary among all employees.

**Endpoint:** `GET /api/v1/employee/highestSalary`

**Response:** Integer representing the highest salary

**Example:**
```bash
curl -X GET "http://localhost:8111/api/v1/employee/highestSalary"
```

#### 5. Get Top 10 Highest Earning Employee Names
Returns the names of the top 10 highest earning employees, sorted by salary (descending).

**Endpoint:** `GET /api/v1/employee/topTenHighestEarningEmployeeNames`

**Response:** Array of employee names (strings)

**Example:**
```bash
curl -X GET "http://localhost:8111/api/v1/employee/topTenHighestEarningEmployeeNames"
```

#### 6. Create Employee
Creates a new employee in the system.

**Endpoint:** `POST /api/v1/employee`

**Request Body:** Employee object (JSON)

**Example:**
```bash
curl -X POST "http://localhost:8111/api/v1/employee" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "John Doe",
       "salary": 50000,
       "age": 30,
       "title": "Software Developer",
       "email": "john.doe@company.com"
     }'
```

#### 7. Delete Employee by ID
Deletes an employee from the system by their ID.

**Endpoint:** `DELETE /api/v1/employee/{id}`

**Parameters:**
- `id` (path): UUID of the employee to delete

**Response:** Name of the deleted employee

**Example:**
```bash
curl -X DELETE "http://localhost:8111/api/v1/employee/123e4567-e89b-12d3-a456-426614174000"
```

### Data Models

#### Employee Object
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "John Doe",
  "salary": 50000,
  "age": 30,
  "title": "Software Developer",
  "email": "john.doe@company.com"
}
```

#### Validation Rules
- **name:** Required, cannot be blank
- **salary:** Required, must be positive integer
- **age:** Required, must be between 16 and 75
- **title:** Required, cannot be blank
- **email:** Optional, must be valid email format if provided

## Error Handling

### HTTP Status Codes
- **200 OK:** Successful request
- **201 Created:** Employee created successfully
- **400 Bad Request:** Invalid input data or validation errors
- **404 Not Found:** Employee not found
- **429 Too Many Requests:** Rate limit exceeded
- **503 Service Unavailable:** Mock API server unavailable
- **500 Internal Server Error:** Unexpected server error

### Error Response Format
```json
{
  "error": "Error message description",
  "message": "Detailed error information",
  "retryAfterSeconds": 60
}
```

### Custom Exceptions
- **EmployeeNotFoundException:** When employee is not found (404)
- **RateLimitExceededException:** When rate limits are exceeded (429)
- **MockServerUnavailableException:** When upstream service is down (503)

## Response Headers for Caching
- `X-Cache-Status`: HIT, MISS, BYPASS
- `X-Cache-Message`: Describes cache action
- `X-Cache-TTL`: Cache expiry in seconds
- `X-Cache-Time`: Timestamp when data was cached (on MISS)

## Performance & Monitoring

### Caching Performance
- **Cache Hit Ratio:** Monitored via Spring Boot Actuator metrics
- **TTL:** 1 minute for all cached data
- **Cache Size:** Maximum 1000 entries per cache region
- **Eviction Policy:** LRU (Least Recently Used)

### Retry Configuration
- **Max Attempts:** 3 retries for failed requests
- **Backoff Strategy:** Exponential backoff (1s, 2s, 4s)
- **Max Delay:** 5 seconds between retries

### Connection Settings
- **Connect Timeout:** 3 seconds
- **Read Timeout:** 7 seconds
- **Connection Pool:** Apache HttpClient5 with connection pooling

## Testing Strategy

### Test Coverage Areas
- **Unit Tests:** Service layer business logic
- **Integration Tests:** API endpoint testing
- **Cache Tests:** Caching behavior verification
- **Error Handling Tests:** Exception scenarios
- **Performance Tests:** Load and stress testing

### Test Execution
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests EmployeeServiceTest

# Run with coverage report
./gradlew test jacocoTestReport
```

## Configuration

### Application Properties
```yaml
spring.application.name: employee-api
server.port: 8111

# Mock API Configuration
mock:
  api:
    base-url: http://localhost:8112

# Logging Configuration
logging:
  level:
    com.reliaquest: DEBUG
```

### Environment-Specific Configuration
- **Development:** Default configuration with debug logging
- **Production:** Optimized for performance with info-level logging
- **Testing:** In-memory configurations for fast test execution

## Deployment

### Prerequisites
- Java 17 or higher
- Gradle 7.0 or higher
- Access to Mock API server (port 8112)

### Build and Run
1. **Build the project:**
   ```bash
   ./gradlew build
   ```

2. **Run the API server:**
   ```bash
   ./gradlew :api:bootRun
   ```

3. **Access endpoints at:** `http://localhost:8111/api/v1/employee`

## Authors
- **Naveen Kumar** - Initial implementation and architecture design



