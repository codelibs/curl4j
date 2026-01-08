# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

curl4j is a simple cURL-like Java HTTP client library providing a fluent API for building HTTP requests. It's designed to be minimal with only Apache Commons IO as a dependency.

## Directory Structure

```
curl4j/
├── src/
│   ├── main/java/org/codelibs/curl/
│   │   ├── Curl.java              # Static entry point with factory methods
│   │   ├── CurlRequest.java       # Fluent request builder
│   │   ├── CurlResponse.java      # Response wrapper (implements Closeable)
│   │   ├── CurlException.java     # Unchecked exception for HTTP errors
│   │   └── io/
│   │       ├── ContentCache.java       # In-memory or file-based caching
│   │       └── ContentOutputStream.java # Threshold-based output stream
│   └── test/java/org/codelibs/curl/
│       ├── CurlTest.java          # Factory method tests
│       ├── CurlRequestTest.java   # Request builder tests
│       ├── CurlResponseTest.java  # Response handling tests
│       ├── CurlExceptionTest.java # Exception tests
│       └── io/
│           ├── ContentCacheTest.java
│           ├── ContentOutputStreamTest.java
│           └── IOIntegrationTest.java
├── .github/workflows/             # CI/CD configuration
│   ├── maven.yml                  # Build and test workflow
│   └── codeql-analysis.yml        # Security analysis
├── pom.xml                        # Maven build configuration
├── README.md                      # Project documentation
└── CLAUDE.md                      # This file
```

## Development Commands

### Build and Test
```bash
# Clean build and run tests
mvn clean test

# Full build with packaging
mvn clean package

# Run specific test class
mvn test -Dtest=CurlTest
mvn test -Dtest=CurlRequestTest
mvn test -Dtest=CurlResponseTest
mvn test -Dtest=CurlExceptionTest

# Run I/O layer tests
mvn test -Dtest=ContentCacheTest
mvn test -Dtest=ContentOutputStreamTest
mvn test -Dtest=IOIntegrationTest
```

### Code Quality
```bash
# Format code
mvn formatter:format

# Check/update license headers
mvn license:check
mvn license:format

# Generate Javadoc
mvn javadoc:javadoc

# Generate test coverage report
mvn jacoco:report
```

## Architecture

### Core Components

- **`Curl`**: Static entry point providing factory methods for all HTTP methods (GET, POST, PUT, DELETE, HEAD, OPTIONS, CONNECT, TRACE). Also defines the `Method` enum and `tmpDir` constant.

- **`CurlRequest`**: Fluent request builder supporting query parameters, headers, body content, GZIP compression, SSL configuration, proxy settings, encoding, threshold, custom connection configuration, and async execution.

- **`CurlResponse`**: Response wrapper implementing `Closeable`. Provides HTTP status code, headers, and content access methods.

- **`CurlException`**: Unchecked RuntimeException for HTTP errors.

### I/O Layer

- **`ContentCache`**: In-memory or file-based caching. Implements `Closeable` and auto-deletes file cache on close.

- **`ContentOutputStream`**: Extends `DeferredFileOutputStream`. Writes to memory until threshold (default 1MB) exceeded, then spills to temp file.

## Technical Details

- **Java Version**: 17+
- **Dependencies**: Apache Commons IO (runtime), JUnit 4 (test)
- **Module Name**: `org.codelibs.curl4j`
- **Package**: `org.codelibs.curl.*`

## Code Style

- Uses external Eclipse formatter from CodeLibs
- Apache License 2.0 headers on all source files
- Comprehensive Javadoc on public APIs

## Test Conventions

- Test class names end with `Test`
- Test methods start with `test_` prefix
- Tests use `## Arrange ##`, `## Act ##`, `## Assert ##` comments
- Some integration tests require network access

## Important Notes for AI Assistants

1. **Resource Management**: Always use try-with-resources with `CurlResponse`.

2. **Null Safety**: Constructors validate null arguments. Follow this pattern.

3. **Thread Safety**: `RequestProcessor` is single-use per request.

4. **Encoding**: Call `encoding()` before `param()` methods.

5. **Threshold**: Default 1MB. Smaller content stays in memory; larger spills to temp files.
