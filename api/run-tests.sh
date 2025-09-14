#!/bin/bash

# Employee API Test Execution Script
# This script runs all tests with different configurations

echo "=========================================="
echo "Employee API Test Suite Execution"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "build.gradle" ]; then
    print_error "Please run this script from the api directory"
    exit 1
fi

print_status "Starting test execution..."

# Clean previous build
print_status "Cleaning previous build..."
./gradlew clean

# Run unit tests
print_status "Running unit tests..."
./gradlew test --tests "*Test" --info

# Check if unit tests passed
if [ $? -eq 0 ]; then
    print_status "Unit tests passed successfully!"
else
    print_error "Unit tests failed!"
    exit 1
fi

# Run integration tests
print_status "Running integration tests..."
./gradlew test --tests "*IntegrationTest" --info

# Check if integration tests passed
if [ $? -eq 0 ]; then
    print_status "Integration tests passed successfully!"
else
    print_error "Integration tests failed!"
    exit 1
fi

# Run cache tests
print_status "Running cache tests..."
./gradlew test --tests "*Cache*Test" --info

# Check if cache tests passed
if [ $? -eq 0 ]; then
    print_status "Cache tests passed successfully!"
else
    print_error "Cache tests failed!"
    exit 1
fi

# Run exception handling tests
print_status "Running exception handling tests..."
./gradlew test --tests "*Exception*Test" --info

# Check if exception tests passed
if [ $? -eq 0 ]; then
    print_status "Exception handling tests passed successfully!"
else
    print_error "Exception handling tests failed!"
    exit 1
fi

# Run performance tests
print_status "Running performance tests..."
./gradlew test --tests "*Performance*Test" --info

# Check if performance tests passed
if [ $? -eq 0 ]; then
    print_status "Performance tests passed successfully!"
else
    print_warning "Performance tests failed (this is expected in test environment without mock server)"
fi

# Generate test report
print_status "Generating test report..."
./gradlew test jacocoTestReport

# Check if report generation was successful
if [ $? -eq 0 ]; then
    print_status "Test report generated successfully!"
    print_status "Test report location: build/reports/tests/test/index.html"
    print_status "Coverage report location: build/reports/jacoco/test/html/index.html"
else
    print_warning "Test report generation failed"
fi

# Run all tests together
print_status "Running complete test suite..."
./gradlew test --info

# Final status
if [ $? -eq 0 ]; then
    print_status "=========================================="
    print_status "All tests completed successfully!"
    print_status "=========================================="
else
    print_error "=========================================="
    print_error "Some tests failed. Please check the output above."
    print_error "=========================================="
    exit 1
fi

print_status "Test execution completed!"
