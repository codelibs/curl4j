# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

curl4j is a simple cURL-like Java HTTP client library providing a fluent API for building HTTP requests. It's designed to be minimal with only Apache Commons IO as a dependency.

## Development Commands

### Build and Test
```bash
# Clean build and run tests
mvn clean test

# Full build with packaging
mvn clean package

# Run tests only
mvn test

# Run specific tests
mvn test -Dtest=CurlTest
mvn test -Dtest=CurlRequestTest
mvn test -Dtest=CurlResponseTest
mvn test -Dtest=CurlExceptionTest
```

### Code Quality
```bash
# Format code (using external formatter config)
mvn formatter:format

# Check license headers
mvn license:check

# Add/update license headers
mvn license:format

# Generate Javadoc
mvn javadoc:javadoc

# Generate test coverage report
mvn jacoco:report
```

### Release Process
```bash
# Sign artifacts (for releases)
mvn verify

# Deploy to Maven Central via Sonatype Central Portal
mvn deploy
```

## Architecture

### Core Components

- **`org.codelibs.curl.Curl`**: Static entry point providing factory methods for all HTTP methods (GET, POST, PUT, DELETE, HEAD, OPTIONS, CONNECT, TRACE)
- **`org.codelibs.curl.CurlRequest`**: Fluent request builder supporting parameters, headers, body content, SSL config, proxies, timeouts, and caching
- **`org.codelibs.curl.CurlResponse`**: Response wrapper implementing `Closeable` for proper resource management
- **`org.codelibs.curl.CurlException`**: Unchecked exception for HTTP errors

### I/O Layer

- **`org.codelibs.curl.io.ContentCache`**: Handles automatic in-memory or on-disk caching of request/response bodies
- **`org.codelibs.curl.io.ContentOutputStream`**: Streaming utilities for efficient content handling

### Key Design Patterns

1. **Fluent Builder Pattern**: `CurlRequest` uses method chaining for configuration
2. **Factory Pattern**: `Curl` class provides static factory methods for each HTTP method
3. **Resource Management**: `CurlResponse` implements `Closeable` for try-with-resources
4. **Async Support**: Both synchronous (`executeSync()`) and asynchronous (`execute()`) execution modes

## Technical Details

- **Java Version**: 17+
- **Dependencies**:
  - Apache Commons IO 2.19.0 (runtime)
  - JUnit 4.13.2 (test)
- **Module Name**: `org.codelibs.curl4j`
- **Package Structure**: `org.codelibs.curl.*`
- **Build Tool**: Maven 3.x
- **Test Coverage**: JaCoCo plugin enabled

## Code Style

- Uses external Eclipse formatter configuration from CodeLibs
- Apache License 2.0 headers on all source files (updated to 2025)
- Comprehensive Javadoc documentation required
- Code coverage reporting with JaCoCo

## Test Coverage

The project includes comprehensive unit tests for all core components:
- `CurlTest`: Tests for static factory methods and basic HTTP operations
- `CurlRequestTest`: Tests for request builder, headers, parameters, body content, SSL, proxies, timeouts
- `CurlResponseTest`: Tests for response handling, status codes, content retrieval, resource management
- `CurlExceptionTest`: Tests for exception handling and error scenarios
- I/O layer tests: `ContentCacheTest`, `ContentOutputStreamTest`

