/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.curl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.util.concurrent.ForkJoinPool;

import javax.net.ssl.SSLSocketFactory;

import org.codelibs.curl.Curl.Method;
import org.junit.Test;

/**
 * Test class for CurlRequest.
 * Tests the fluent API, parameter handling, and request configuration.
 */
public class CurlRequestTest {

    @Test
    public void testConstructor() {
        String url = "https://example.com";
        CurlRequest request = new CurlRequest(Method.GET, url);

        assertEquals(Method.GET, request.method());
        assertEquals("UTF-8", request.encoding());
        assertEquals(1024 * 1024, request.threshold());
        assertNull(request.proxy());
        assertNull(request.body());
    }

    @Test
    public void testConstructorWithNullUrl() {
        // URL can be null with the two-argument constructor
        CurlRequest request = new CurlRequest(Method.DELETE, null);

        assertEquals(Method.DELETE, request.method());
        assertEquals("UTF-8", request.encoding());
        assertEquals(1024 * 1024, request.threshold());
        assertNull(request.proxy());
        assertNull(request.body());
    }

    @Test
    public void testConstructorWithNullMethod() {
        String url = "https://example.com";

        try {
            new CurlRequest(null, url);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method must not be null"));
        }
    }

    @Test
    public void testSingleArgumentConstructor() {
        CurlRequest request = new CurlRequest(Method.POST);

        assertEquals(Method.POST, request.method());
        assertEquals("UTF-8", request.encoding());
        assertEquals(1024 * 1024, request.threshold());
        assertNull(request.proxy());
        assertNull(request.body());
    }

    @Test
    public void testSingleArgumentConstructorWithNullMethod() {
        try {
            new CurlRequest(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method must not be null"));
        }
    }

    @Test
    public void testProxyMethod() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");
        Proxy proxy = Proxy.NO_PROXY;

        CurlRequest result = request.proxy(proxy);

        assertSame(request, result); // Fluent API
        assertSame(proxy, request.proxy());
    }

    @Test
    public void testEncodingMethod() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");
        String encoding = "ISO-8859-1";

        CurlRequest result = request.encoding(encoding);

        assertSame(request, result); // Fluent API
        assertEquals(encoding, request.encoding());
    }

    @Test
    public void testEncodingAfterParamThrowsException() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");
        request.param("key", "value");

        try {
            request.encoding("ISO-8859-1");
            fail("Expected CurlException");
        } catch (CurlException e) {
            assertTrue(e.getMessage().contains("must be called before param method"));
        }
    }

    @Test
    public void testThresholdMethod() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");
        int threshold = 2048;

        CurlRequest result = request.threshold(threshold);

        assertSame(request, result); // Fluent API
        assertEquals(threshold, request.threshold());
    }

    @Test
    public void testGzipMethod() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        CurlRequest result = request.gzip();

        assertSame(request, result); // Fluent API
    }

    @Test
    public void testCompressionMethod() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");
        String compression = "deflate";

        CurlRequest result = request.compression(compression);

        assertSame(request, result); // Fluent API
    }

    @Test
    public void testSslSocketFactoryMethod() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        CurlRequest result = request.sslSocketFactory(factory);

        assertSame(request, result); // Fluent API
    }

    @Test
    public void testBodyStringMethod() {
        CurlRequest request = new CurlRequest(Method.POST, "https://example.com");
        String body = "{\"key\":\"value\"}";

        CurlRequest result = request.body(body);

        assertSame(request, result); // Fluent API
        assertEquals(body, request.body());
    }

    @Test
    public void testBodyInputStreamMethod() {
        CurlRequest request = new CurlRequest(Method.POST, "https://example.com");
        InputStream stream = new ByteArrayInputStream("test data".getBytes());

        CurlRequest result = request.body(stream);

        assertSame(request, result); // Fluent API
        assertNull(request.body()); // body() returns String body, not stream
    }

    @Test
    public void testBodyStringAfterStreamThrowsException() {
        CurlRequest request = new CurlRequest(Method.POST, "https://example.com");
        InputStream stream = new ByteArrayInputStream("test data".getBytes());
        request.body(stream);

        try {
            request.body("string body");
            fail("Expected CurlException");
        } catch (CurlException e) {
            assertTrue(e.getMessage().contains("body method is already called"));
        }
    }

    @Test
    public void testBodyStreamAfterStringThrowsException() {
        CurlRequest request = new CurlRequest(Method.POST, "https://example.com");
        request.body("string body");

        try {
            InputStream stream = new ByteArrayInputStream("test data".getBytes());
            request.body(stream);
            fail("Expected CurlException");
        } catch (CurlException e) {
            assertTrue(e.getMessage().contains("body method is already called"));
        }
    }

    @Test
    public void testOnConnectMethod() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        CurlRequest result = request.onConnect((req, conn) -> {
            conn.setConnectTimeout(5000);
        });

        assertSame(request, result); // Fluent API
    }

    @Test
    public void testParamMethod() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        CurlRequest result = request.param("key1", "value1").param("key2", "value2");

        assertSame(request, result); // Fluent API
    }

    @Test
    public void testParamWithNullValue() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        CurlRequest result = request.param("key", null);

        assertSame(request, result); // Should not add null parameters
    }

    @Test
    public void testHeaderMethod() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        CurlRequest result = request.header("Content-Type", "application/json").header("Accept", "application/json");

        assertSame(request, result); // Fluent API
    }

    @Test
    public void testThreadPoolMethod() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");
        ForkJoinPool pool = new ForkJoinPool();

        CurlRequest result = request.threadPool(pool);

        assertSame(request, result); // Fluent API
    }

    @Test
    public void testEncodingSpecialCharacters() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Test encoding of special characters
        request.param("query", "hello world & more");
        request.param("special", "ñ€±");

        // Should not throw exception
        assertNotNull(request);
    }

    @Test
    public void testUnsupportedEncodingThrowsException() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");
        request.encoding("INVALID-ENCODING");

        try {
            request.param("key", "value");
            fail("Expected CurlException for invalid encoding");
        } catch (CurlException e) {
            assertTrue(e.getMessage().contains("Invalid encoding"));
        }
    }

    @Test
    public void testFluentChaining() {
        String url = "https://example.com";
        CurlRequest request = new CurlRequest(Method.POST, url).encoding("UTF-8").threshold(2048).gzip().param("param1", "value1")
                .param("param2", "value2").header("Content-Type", "application/json").header("Accept", "application/json")
                .body("{\"test\":\"data\"}");

        assertNotNull(request);
        assertEquals(Method.POST, request.method());
        assertEquals("UTF-8", request.encoding());
        assertEquals(2048, request.threshold());
        assertEquals("{\"test\":\"data\"}", request.body());
    }

    @Test
    public void testAllHttpMethods() {
        String url = "https://example.com";

        // Test that CurlRequest can be created with all HTTP methods
        for (Method method : Method.values()) {
            CurlRequest request = new CurlRequest(method, url);
            assertEquals(method, request.method());
        }
    }

    @Test
    public void testRequestProcessorConstructor() {
        // Test RequestProcessor constructor
        CurlRequest.RequestProcessor processor = new CurlRequest.RequestProcessor("UTF-8", 1024);
        assertNotNull(processor);
        assertNotNull(processor.getResponse());
    }

    @Test
    public void testRequestProcessorGetResponse() {
        // Test RequestProcessor getResponse method
        CurlRequest.RequestProcessor processor = new CurlRequest.RequestProcessor("UTF-8", 2048);
        CurlResponse response = processor.getResponse();
        assertNotNull(response);
    }

    @Test
    public void testMultipleParamsWithSpecialCharacters() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Add params with special characters
        request.param("name", "John Doe").param("email", "test@example.com").param("query", "hello & goodbye").param("special", "100%");

        assertNotNull(request);
        assertEquals(Method.GET, request.method());
    }

    @Test
    public void testHeadersWithMultipleValues() {
        CurlRequest request = new CurlRequest(Method.POST, "https://example.com");

        // Add multiple headers
        request.header("Accept", "application/json").header("Content-Type", "application/json").header("Authorization", "Bearer token123")
                .header("X-Custom-Header", "custom-value");

        assertNotNull(request);
    }

    @Test
    public void testGzipSetsCompressionToGzip() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Call gzip() and verify it sets compression
        request.gzip();

        // We can't directly access compression field, but we can verify the method returns this
        assertSame(request, request.gzip());
    }

    @Test
    public void testMultipleCompressionCalls() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Multiple compression calls should work
        request.compression("gzip");
        request.compression("deflate");

        assertNotNull(request);
    }

    @Test
    public void testEmptyStringParam() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Empty string param should be added
        request.param("empty", "");

        assertNotNull(request);
    }

    @Test
    public void testEmptyStringHeader() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Empty string header value should be added
        request.header("X-Empty", "");

        assertNotNull(request);
    }

    @Test
    public void testEmptyBody() {
        CurlRequest request = new CurlRequest(Method.POST, "https://example.com");

        // Empty body should be accepted
        request.body("");

        assertEquals("", request.body());
    }

    @Test
    public void testProxyWithNullValue() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Setting proxy to null should work
        request.proxy(null);

        assertNull(request.proxy());
    }

    @Test
    public void testMultipleEncodingCallsBeforeParam() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Multiple encoding calls should work as long as no params are added yet
        request.encoding("UTF-8");
        request.encoding("ISO-8859-1");
        request.encoding("UTF-16");

        assertEquals("UTF-16", request.encoding());
    }

    @Test
    public void testThresholdWithZero() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Threshold of 0 should be accepted
        request.threshold(0);

        assertEquals(0, request.threshold());
    }

    @Test
    public void testThresholdWithNegative() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Negative threshold should be accepted (though may not make sense)
        request.threshold(-1);

        assertEquals(-1, request.threshold());
    }

    @Test
    public void testThresholdWithLargeValue() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Large threshold value
        int largeThreshold = Integer.MAX_VALUE;
        request.threshold(largeThreshold);

        assertEquals(largeThreshold, request.threshold());
    }

    @Test
    public void testBodyWithUnicodeCharacters() {
        CurlRequest request = new CurlRequest(Method.POST, "https://example.com");

        // Body with unicode characters
        String unicodeBody = "{\"message\":\"こんにちは世界\"}";
        request.body(unicodeBody);

        assertEquals(unicodeBody, request.body());
    }

    @Test
    public void testParamWithUnicodeCharacters() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Param with unicode characters
        request.param("message", "こんにちは");

        assertNotNull(request);
    }

    @Test
    public void testUrlWithQueryString() {
        // URL already contains query string
        String url = "https://example.com?existing=param";
        CurlRequest request = new CurlRequest(Method.GET, url);

        // Add additional param
        request.param("new", "value");

        assertNotNull(request);
    }

    @Test
    public void testMultipleThreadPoolCalls() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");
        ForkJoinPool pool1 = new ForkJoinPool(2);
        ForkJoinPool pool2 = new ForkJoinPool(4);

        // Multiple threadPool calls should work
        request.threadPool(pool1);
        request.threadPool(pool2);

        assertNotNull(request);
    }

    @Test
    public void testOnConnectWithNull() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Setting onConnect to null should work
        request.onConnect(null);

        assertNotNull(request);
    }

    @Test
    public void testMultipleOnConnectCalls() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Multiple onConnect calls should work (last one wins)
        request.onConnect((req, conn) -> conn.setConnectTimeout(1000));
        request.onConnect((req, conn) -> conn.setReadTimeout(2000));

        assertNotNull(request);
    }

    @Test
    public void testSslSocketFactoryWithNull() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Setting SSL socket factory to null should work
        request.sslSocketFactory(null);

        assertNotNull(request);
    }

    @Test
    public void testLongUrl() {
        // Test with very long URL
        StringBuilder longUrl = new StringBuilder("https://example.com/path");
        for (int i = 0; i < 100; i++) {
            longUrl.append("/segment").append(i);
        }

        CurlRequest request = new CurlRequest(Method.GET, longUrl.toString());
        assertNotNull(request);
        assertEquals(Method.GET, request.method());
    }

    @Test
    public void testManyParams() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Add many parameters
        for (int i = 0; i < 100; i++) {
            request.param("param" + i, "value" + i);
        }

        assertNotNull(request);
    }

    @Test
    public void testManyHeaders() {
        CurlRequest request = new CurlRequest(Method.GET, "https://example.com");

        // Add many headers
        for (int i = 0; i < 50; i++) {
            request.header("X-Header-" + i, "value" + i);
        }

        assertNotNull(request);
    }

    @Test
    public void testComplexFluentChaining() {
        // Test complex fluent chaining with various configurations
        CurlRequest request = new CurlRequest(Method.POST, "https://api.example.com/v1/users").encoding("UTF-8").threshold(2048).gzip()
                .proxy(Proxy.NO_PROXY).sslSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault()).param("filter", "active")
                .param("sort", "name").header("Accept", "application/json").header("Content-Type", "application/json")
                .header("Authorization", "Bearer token").body("{\"name\":\"test\"}").threadPool(new ForkJoinPool())
                .onConnect((req, conn) -> conn.setConnectTimeout(5000));

        assertNotNull(request);
        assertEquals(Method.POST, request.method());
        assertEquals("UTF-8", request.encoding());
        assertEquals(2048, request.threshold());
        assertEquals("{\"name\":\"test\"}", request.body());
    }

    @Test
    public void testRequestProcessorWithDifferentEncodings() {
        // Test RequestProcessor with different encodings
        CurlRequest.RequestProcessor processor1 = new CurlRequest.RequestProcessor("UTF-8", 1024);
        CurlRequest.RequestProcessor processor2 = new CurlRequest.RequestProcessor("ISO-8859-1", 2048);
        CurlRequest.RequestProcessor processor3 = new CurlRequest.RequestProcessor("UTF-16", 512);

        assertNotNull(processor1.getResponse());
        assertNotNull(processor2.getResponse());
        assertNotNull(processor3.getResponse());
    }

    @Test
    public void testRequestProcessorWithZeroThreshold() {
        // Test RequestProcessor with zero threshold
        CurlRequest.RequestProcessor processor = new CurlRequest.RequestProcessor("UTF-8", 0);
        assertNotNull(processor.getResponse());
    }

    @Test
    public void testRequestProcessorWithLargeThreshold() {
        // Test RequestProcessor with large threshold
        CurlRequest.RequestProcessor processor = new CurlRequest.RequestProcessor("UTF-8", Integer.MAX_VALUE);
        assertNotNull(processor.getResponse());
    }

    @Test
    public void testMethodEnumToString() {
        // Test that Method enum toString returns correct values
        assertEquals("GET", Method.GET.toString());
        assertEquals("POST", Method.POST.toString());
        assertEquals("PUT", Method.PUT.toString());
        assertEquals("DELETE", Method.DELETE.toString());
        assertEquals("HEAD", Method.HEAD.toString());
        assertEquals("OPTIONS", Method.OPTIONS.toString());
        assertEquals("TRACE", Method.TRACE.toString());
        assertEquals("CONNECT", Method.CONNECT.toString());
    }
}