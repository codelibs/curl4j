# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

curl4j is a simple cURL-like Java HTTP client library providing a fluent API for building HTTP requests. It's designed to be minimal with only Apache Commons IO as a dependency.

- **Current Version**: 1.3.2-SNAPSHOT
- **Repository**: https://github.com/codelibs/curl4j
- **License**: Apache License 2.0

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

# Run tests only
mvn test

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
# Format code (using external Eclipse formatter config from CodeLibs)
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

- **`org.codelibs.curl.Curl`**: Static entry point providing factory methods for all HTTP methods (GET, POST, PUT, DELETE, HEAD, OPTIONS, CONNECT, TRACE). Also defines the `Method` enum and the `tmpDir` constant for temporary file storage.

- **`org.codelibs.curl.CurlRequest`**: Fluent request builder supporting:
  - Query parameters (`param()`)
  - Request headers (`header()`)
  - Body content (`body()` - String or InputStream)
  - GZIP compression (`gzip()`, `compression()`)
  - SSL configuration (`sslSocketFactory()`)
  - Proxy settings (`proxy()`)
  - Character encoding (`encoding()`)
  - Threshold for memory/file caching (`threshold()`)
  - Custom connection configuration (`onConnect()`)
  - Async execution with ForkJoinPool (`threadPool()`)

- **`org.codelibs.curl.CurlResponse`**: Response wrapper implementing `Closeable` for proper resource management. Provides:
  - HTTP status code (`getHttpStatusCode()`)
  - Response headers (`getHeaders()`, `getHeaderValue()`, `getHeaderValues()`)
  - Content as String or InputStream (`getContentAsString()`, `getContentAsStream()`)
  - Custom content parsing (`getContent(Function)`)

- **`org.codelibs.curl.CurlException`**: Unchecked RuntimeException for HTTP errors.

### I/O Layer

- **`org.codelibs.curl.io.ContentCache`**: Handles automatic in-memory or file-based caching of request/response bodies. Implements `Closeable` and automatically deletes file cache on close.

- **`org.codelibs.curl.io.ContentOutputStream`**: Extends Apache Commons IO's `DeferredFileOutputStream`. Writes to memory until threshold (default 1MB) is exceeded, then switches to temporary file storage.

### Key Design Patterns

1. **Fluent Builder Pattern**: `CurlRequest` uses method chaining for configuration
2. **Factory Pattern**: `Curl` class provides static factory methods for each HTTP method
3. **Resource Management**: `CurlResponse` implements `Closeable` for try-with-resources
4. **Async Support**: Both synchronous (`execute()`) and asynchronous (`execute(Consumer, Consumer)`) execution modes

## Usage Examples

### Synchronous Request
```java
try (CurlResponse response = Curl.get("https://example.com")
                                .param("q", "curl4j")
                                .header("Accept", "application/json")
                                .execute()) {
    System.out.println("Status: " + response.getHttpStatusCode());
    System.out.println(response.getContentAsString());
}
```

### Asynchronous Request
```java
Curl.post("https://api.example.com/items")
    .body("{\"name\":\"item1\"}")
    .header("Content-Type", "application/json")
    .execute(
        response -> System.out.println("Status: " + response.getHttpStatusCode()),
        error -> error.printStackTrace());
```

### With Custom SSL Configuration
```java
SSLContext sslContext = // ... configure SSL context
Curl.get("https://secure.example.com")
    .sslSocketFactory(sslContext.getSocketFactory())
    .execute();
```

## Technical Details

- **Java Version**: 17+ (uses `release` compiler option)
- **Dependencies**:
  - Apache Commons IO 2.19.0 (runtime)
  - JUnit 4.13.2 (test)
- **Module Name**: `org.codelibs.curl4j` (JPMS compatible via Automatic-Module-Name)
- **Package Structure**: `org.codelibs.curl.*`
- **Build Tool**: Maven 3.x
- **Test Coverage**: JaCoCo plugin enabled

## CI/CD

The project uses GitHub Actions for continuous integration:
- **Workflow**: `.github/workflows/maven.yml`
- **Triggers**: Push/PR to `master` and `*.x` branches
- **Matrix Build**: Ubuntu and Windows with JDK 17 (Temurin distribution)
- **Security Analysis**: CodeQL scanning via `.github/workflows/codeql-analysis.yml`

## Code Style

- Uses external Eclipse formatter configuration from CodeLibs: `https://www.codelibs.org/assets/formatter/eclipse-formatter-1.0.xml`
- Apache License 2.0 headers on all source files (year: 2025)
- Comprehensive Javadoc documentation on all public APIs
- Code coverage reporting with JaCoCo
- License header definition: `https://www.codelibs.org/assets/license/header-definition-2.xml`

## Test Conventions

The project uses JUnit 4 with the following patterns:
- Test class names end with `Test` (e.g., `CurlTest`, `CurlRequestTest`)
- Test methods start with `test_` prefix
- Tests use `## Arrange ##`, `## Act ##`, `## Assert ##` comments for clarity
- Integration tests may require network access (e.g., `test_Get()` hits `https://www.codelibs.org/`)

## Important Notes for AI Assistants

1. **Resource Management**: Always use try-with-resources when working with `CurlResponse` to ensure proper cleanup of cached content.

2. **Null Safety**: The codebase includes null validation in constructors (e.g., `CurlRequest`, `ContentCache`). Follow this pattern when making changes.

3. **Thread Safety**: `CurlRequest` supports async execution via `threadPool()`. The `RequestProcessor` is designed for single-use per request.

4. **Encoding**: Default encoding is UTF-8. The `encoding()` method must be called before `param()` methods.

5. **Threshold Behavior**: Default threshold is 1MB. Content smaller than threshold stays in memory; larger content spills to temp files.
