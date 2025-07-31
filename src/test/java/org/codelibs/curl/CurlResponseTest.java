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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.curl.io.ContentCache;
import org.junit.Test;

/**
 * Test class for CurlResponse.
 * Tests response handling, header operations, and content access.
 */
public class CurlResponseTest {

    @Test
    public void testConstructor() {
        CurlResponse response = new CurlResponse();
        assertNotNull(response);
    }

    @Test
    public void testHttpStatusCode() {
        CurlResponse response = new CurlResponse();
        int statusCode = 200;

        response.setHttpStatusCode(statusCode);

        assertEquals(statusCode, response.getHttpStatusCode());
    }

    @Test
    public void testEncoding() {
        CurlResponse response = new CurlResponse();
        String encoding = "UTF-8";

        response.setEncoding(encoding);

        assertEquals(encoding, response.getEncoding());
    }

    @Test
    public void testContentException() {
        CurlResponse response = new CurlResponse();
        Exception exception = new IOException("Test exception");

        response.setContentException(exception);

        assertSame(exception, response.getContentException());
    }

    @Test
    public void testHeaders() {
        CurlResponse response = new CurlResponse();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Content-Length", Arrays.asList("100"));

        response.setHeaders(headers);

        Map<String, List<String>> result = response.getHeaders();
        assertNotNull(result);
        // Headers should be stored in lowercase
        assertTrue(result.containsKey("content-type"));
        assertTrue(result.containsKey("content-length"));
    }

    @Test
    public void testHeadersWithNullKey() {
        CurlResponse response = new CurlResponse();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(null, Arrays.asList("value"));
        headers.put("Content-Type", Arrays.asList("application/json"));

        response.setHeaders(headers);

        Map<String, List<String>> result = response.getHeaders();
        assertNotNull(result);
        assertEquals(1, result.size()); // null key should be filtered out
        assertTrue(result.containsKey("content-type"));
    }

    @Test
    public void testHeadersWithNullMap() {
        CurlResponse response = new CurlResponse();

        response.setHeaders(null);

        assertNull(response.getHeaders());
    }

    @Test
    public void testGetHeaderValue() {
        CurlResponse response = new CurlResponse();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Content-Length", Arrays.asList("100"));
        response.setHeaders(headers);

        String contentType = response.getHeaderValue("Content-Type");
        String contentLength = response.getHeaderValue("content-length"); // Test case insensitive
        String missing = response.getHeaderValue("Missing-Header");

        assertEquals("application/json", contentType);
        assertEquals("100", contentLength);
        assertNull(missing);
    }

    @Test
    public void testGetHeaderValues() {
        CurlResponse response = new CurlResponse();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Accept", Arrays.asList("application/json", "text/html"));
        response.setHeaders(headers);

        String[] acceptValues = response.getHeaderValues("Accept");
        String[] acceptValuesLowercase = response.getHeaderValues("accept"); // Test case insensitive
        String[] missingValues = response.getHeaderValues("Missing-Header");

        assertArrayEquals(new String[] { "application/json", "text/html" }, acceptValues);
        assertArrayEquals(new String[] { "application/json", "text/html" }, acceptValuesLowercase);
        assertArrayEquals(new String[0], missingValues);
    }

    @Test
    public void testGetHeaderValueMultipleValues() {
        CurlResponse response = new CurlResponse();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Accept", Arrays.asList("application/json", "text/html"));
        response.setHeaders(headers);

        String firstValue = response.getHeaderValue("Accept");

        assertEquals("application/json", firstValue); // Should return first value
    }

    @Test
    public void testContentCache() {
        CurlResponse response = new CurlResponse();
        byte[] data = "test content".getBytes();
        ContentCache cache = new ContentCache(data);

        response.setContentCache(cache);

        // ContentCache is not directly accessible, but we can test through content methods
        assertNotNull(response);
    }

    @Test
    public void testGetContentAsStringWithCache() throws IOException {
        CurlResponse response = new CurlResponse();
        response.setEncoding("UTF-8");
        String testContent = "Hello, World!";
        byte[] data = testContent.getBytes("UTF-8");
        ContentCache cache = new ContentCache(data);
        response.setContentCache(cache);

        String content = response.getContentAsString();

        assertEquals(testContent, content);
    }

    @Test
    public void testGetContentAsStringWithDifferentEncoding() throws IOException {
        CurlResponse response = new CurlResponse();
        response.setEncoding("ISO-8859-1");
        String testContent = "Hello, World!";
        byte[] data = testContent.getBytes("ISO-8859-1");
        ContentCache cache = new ContentCache(data);
        response.setContentCache(cache);

        String content = response.getContentAsString();

        assertEquals(testContent, content);
    }

    @Test
    public void testGetContentAsStringWithoutCache() {
        CurlResponse response = new CurlResponse();

        try {
            response.getContentAsString();
            fail("Expected CurlException");
        } catch (CurlException e) {
            assertTrue(e.getMessage().contains("Failed to access the content"));
        }
    }

    @Test
    public void testGetContentAsStringWithContentException() {
        CurlResponse response = new CurlResponse();
        IOException exception = new IOException("Test IO exception");
        response.setContentException(exception);

        try {
            response.getContentAsString();
            fail("Expected CurlException");
        } catch (CurlException e) {
            assertTrue(e.getMessage().contains("Failed to access the content"));
            // The cause should be another CurlException from getContentAsStream()
            assertTrue(e.getCause() instanceof CurlException);
            CurlException innerException = (CurlException) e.getCause();
            assertTrue(innerException.getMessage().contains("The content does not exist"));
            assertSame(exception, innerException.getCause());
        }
    }

    @Test
    public void testGetContentAsStreamWithCache() throws IOException {
        CurlResponse response = new CurlResponse();
        byte[] data = "test content".getBytes();
        ContentCache cache = new ContentCache(data);
        response.setContentCache(cache);

        try (InputStream stream = response.getContentAsStream()) {
            assertNotNull(stream);
            byte[] buffer = new byte[1024];
            int bytesRead = stream.read(buffer);
            assertEquals(data.length, bytesRead);
            assertArrayEquals(data, Arrays.copyOf(buffer, bytesRead));
        }
    }

    @Test
    public void testGetContentAsStreamWithoutCache() {
        CurlResponse response = new CurlResponse();

        try {
            response.getContentAsStream();
            fail("Expected CurlException");
        } catch (Exception e) {
            assertTrue(e instanceof CurlException);
            assertTrue(e.getMessage().contains("The content does not exist"));
        }
    }

    @Test
    public void testGetContentAsStreamWithContentException() {
        CurlResponse response = new CurlResponse();
        IOException exception = new IOException("Test IO exception");
        response.setContentException(exception);

        try {
            response.getContentAsStream();
            fail("Expected CurlException");
        } catch (CurlException e) {
            assertTrue(e.getMessage().contains("The content does not exist"));
            assertSame(exception, e.getCause());
        } catch (IOException e) {
            fail("Should throw CurlException, not IOException");
        }
    }

    @Test
    public void testGetContentWithParser() throws IOException {
        CurlResponse response = new CurlResponse();
        response.setEncoding("UTF-8");
        String testContent = "42";
        byte[] data = testContent.getBytes("UTF-8");
        ContentCache cache = new ContentCache(data);
        response.setContentCache(cache);

        Integer result = response.getContent(r -> Integer.parseInt(r.getContentAsString()));

        assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testCloseWithContentCache() throws IOException {
        CurlResponse response = new CurlResponse();
        byte[] data = "test content".getBytes();
        ContentCache cache = new ContentCache(data);
        response.setContentCache(cache);

        // Should not throw exception
        response.close();
    }

    @Test
    public void testCloseWithoutContentCache() throws IOException {
        CurlResponse response = new CurlResponse();

        // Should not throw exception
        response.close();
    }

    @Test
    public void testHeadersCaseInsensitive() {
        CurlResponse response = new CurlResponse();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("CONTENT-LENGTH", Arrays.asList("100"));
        headers.put("accept-encoding", Arrays.asList("gzip"));
        response.setHeaders(headers);

        // All header access should be case-insensitive
        assertEquals("application/json", response.getHeaderValue("content-type"));
        assertEquals("application/json", response.getHeaderValue("CONTENT-TYPE"));
        assertEquals("application/json", response.getHeaderValue("Content-Type"));

        assertEquals("100", response.getHeaderValue("content-length"));
        assertEquals("100", response.getHeaderValue("CONTENT-LENGTH"));

        assertEquals("gzip", response.getHeaderValue("Accept-Encoding"));
        assertEquals("gzip", response.getHeaderValue("ACCEPT-ENCODING"));
    }
}