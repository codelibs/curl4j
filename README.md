curl4j
[![Java CI with Maven](https://github.com/codelibs/curl4j/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/curl4j/actions/workflows/maven.yml)
=====

A simple cURL-like Java HTTP client.

## Features

- Fluent API for building HTTP requests (GET, POST, PUT, DELETE, HEAD, OPTIONS, CONNECT, TRACE)
- Support for query parameters, headers, body (String or stream), compression, SSL configuration, proxies, and timeouts
- Automatic in-memory or on-disk caching of request/response bodies
- Synchronous and asynchronous (callback) execution
- Minimal dependencies (only Apache Commons IO)

## Installation

### Maven

Add the dependency to your `pom.xml` (replace `x.y.z` with the latest version):

```xml
<dependency>
  <groupId>org.codelibs</groupId>
  <artifactId>curl4j</artifactId>
  <version>x.y.z</version>
</dependency>
```

See [Maven Central](https://repo1.maven.org/maven2/org/codelibs/curl4j/) for available versions.

### Gradle

```groovy
implementation 'org.codelibs:curl4j:x.y.z'
```

## Quick Start

### Synchronous request

```java
import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlResponse;

try (CurlResponse response = Curl.get("https://example.com")
                                .param("q", "curl4j")
                                .header("Accept", "application/json")
                                .execute()) {
    System.out.println("Status: " + response.getHttpStatusCode());
    System.out.println(response.getContentAsString());
}
```

### Asynchronous request

```java
import org.codelibs.curl.Curl;

Curl.post("https://api.example.com/items")
    .body("{\"name\":\"item1\"}")
    .header("Content-Type", "application/json")
    .execute(
        response -> System.out.println("Async status: " + response.getHttpStatusCode()),
        error -> error.printStackTrace());
```

## API Overview

- `org.codelibs.curl.Curl`: entry point for HTTP methods (GET, POST, PUT, DELETE, HEAD, OPTIONS, CONNECT, TRACE).
- `org.codelibs.curl.CurlRequest`: builder for HTTP requests.
- `org.codelibs.curl.CurlResponse`: wrapper for HTTP responses.
- `org.codelibs.curl.CurlException`: unchecked exception for errors.
- `org.codelibs.curl.io.ContentCache` and `ContentOutputStream`: internal utilities for streaming and caching.

Refer to the Javadoc for full API details.

## Building and Testing

```bash
git clone https://github.com/codelibs/curl4j.git
cd curl4j
mvn clean test
```

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
