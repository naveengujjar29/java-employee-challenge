@echo off
REM Employee API Test Execution Script for Windows
REM This script runs all tests with different configurations

echo ==========================================
echo Employee API Test Suite Execution
echo ==========================================

REM Check if we're in the right directory
if not exist "build.gradle" (
    echo [ERROR] Please run this script from the api directory
    exit /b 1
)

echo [INFO] Starting test execution...

REM Clean previous build
echo [INFO] Cleaning previous build...
gradlew.bat clean

REM Run unit tests
echo [INFO] Running unit tests...
gradlew.bat test --tests "*Test" --info

REM Check if unit tests passed
if %errorlevel% neq 0 (
    echo [ERROR] Unit tests failed!
    exit /b 1
)
echo [INFO] Unit tests passed successfully!

REM Run integration tests
echo [INFO] Running integration tests...
gradlew.bat test --tests "*IntegrationTest" --info

REM Check if integration tests passed
if %errorlevel% neq 0 (
    echo [ERROR] Integration tests failed!
    exit /b 1
)
echo [INFO] Integration tests passed successfully!

REM Run cache tests
echo [INFO] Running cache tests...
gradlew.bat test --tests "*Cache*Test" --info

REM Check if cache tests passed
if %errorlevel% neq 0 (
    echo [ERROR] Cache tests failed!
    exit /b 1
)
echo [INFO] Cache tests passed successfully!

REM Run exception handling tests
echo [INFO] Running exception handling tests...
gradlew.bat test --tests "*Exception*Test" --info

REM Check if exception tests passed
if %errorlevel% neq 0 (
    echo [ERROR] Exception handling tests failed!
    exit /b 1
)
echo [INFO] Exception handling tests passed successfully!

REM Run performance tests
echo [INFO] Running performance tests...
gradlew.bat test --tests "*Performance*Test" --info

REM Check if performance tests passed
if %errorlevel% neq 0 (
    echo [WARNING] Performance tests failed (this is expected in test environment without mock server)
)

REM Generate test report
echo [INFO] Generating test report...
gradlew.bat test jacocoTestReport

REM Check if report generation was successful
if %errorlevel% neq 0 (
    echo [WARNING] Test report generation failed
) else (
    echo [INFO] Test report generated successfully!
    echo [INFO] Test report location: build\reports\tests\test\index.html
    echo [INFO] Coverage report location: build\reports\jacoco\test\html\index.html
)

REM Run all tests together
echo [INFO] Running complete test suite...
gradlew.bat test --info

REM Final status
if %errorlevel% neq 0 (
    echo ==========================================
    echo [ERROR] Some tests failed. Please check the output above.
    echo ==========================================
    exit /b 1
) else (
    echo ==========================================
    echo [INFO] All tests completed successfully!
    echo ==========================================
)

echo [INFO] Test execution completed!
pause
