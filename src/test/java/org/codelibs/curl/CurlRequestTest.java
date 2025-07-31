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
}