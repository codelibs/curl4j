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
package org.codelibs.curl.io;

import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlRequest;
import org.codelibs.curl.CurlResponse;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.codelibs.curl.io.ContentOutputStream.PREFIX;
import static org.codelibs.curl.io.ContentOutputStream.SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IOIntegrationTest {

    private static final Logger logger = Logger.getLogger(IOIntegrationTest.class.getName());

    class MockCurlRequest extends CurlRequest {

        MockCurlRequest(Curl.Method method, String url) {
            super(method, url);
        }

        @Override
        public void connect(Consumer<HttpURLConnection> actionListener, Consumer<Exception> exceptionListener) {
            try {
                actionListener.accept(new MockHttpURLConnection(new URL(url)));
            } catch (MalformedURLException e) {
                exceptionListener.accept(e);
            }
        }
    }

    class MockHttpURLConnection extends HttpURLConnection {

        MockHttpURLConnection(URL u) {
            super(u);
        }

        @Override
        public void disconnect() {
            // Do Nothing
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {
            // Do Nothing
        }

        @Override
        public int getResponseCode() throws IOException {
            return 200;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(new byte[100]); // dummy payload
        }
    }

    @Test
    public void test_TmpFileHasBeenDeletedAfterResponseWasClosed() throws Exception {
        // ## Arrange ##
        CurlRequest req = new MockCurlRequest(Curl.Method.POST, "http://dummy");
        req.threshold(0); // always create tmp file

        // ## Act ##
        long before = countTmpFiles();
        logger.info("Before request. Number of temp files: " + before);
        req.execute(res -> {
            logger.info("Processing request. Number of temp files: " + countTmpFiles());
        }, e -> {});
        long after = countTmpFiles();
        logger.info("After close response. Number of temp files: " + after);

        // ## Assert ##
        assertEquals(before, after);
    }

    private long countTmpFiles() {
        return Arrays.stream(Objects.requireNonNull(Curl.tmpDir.listFiles())).map(File::getName)
                .filter(s -> s.startsWith(PREFIX) && s.endsWith(SUFFIX)).count();
    }

    // --- HEAD request logic tests ---

    /**
     * Mock HttpURLConnection that simulates a HEAD request with configurable status code.
     */
    class HeadMockHttpURLConnection extends HttpURLConnection {
        private final int statusCode;

        HeadMockHttpURLConnection(URL u, int statusCode) {
            super(u);
            this.statusCode = statusCode;
            this.method = "HEAD";
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }

        @Override
        public int getResponseCode() {
            return statusCode;
        }

        @Override
        public String getRequestMethod() {
            return "HEAD";
        }

        @Override
        public InputStream getInputStream() {
            throw new AssertionError("HEAD request should not read input stream");
        }

        @Override
        public InputStream getErrorStream() {
            throw new AssertionError("HEAD request should not read error stream");
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }
    }

    @Test
    public void test_HeadRequestWith200_ReturnsEmptyBody() throws Exception {
        // ## Arrange ##
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.HEAD, "http://dummy", u -> new HeadMockHttpURLConnection(u, 200));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            String content = response.getContentAsString();
            assertEquals("", content);
        }
    }

    @Test
    public void test_HeadRequestWith404_ReturnsEmptyBody() throws Exception {
        // ## Arrange ##
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.HEAD, "http://dummy", u -> new HeadMockHttpURLConnection(u, 404));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(404, response.getHttpStatusCode());
            String content = response.getContentAsString();
            assertEquals("", content);
        }
    }

    @Test
    public void test_HeadRequestWith500_ReturnsEmptyBody() throws Exception {
        // ## Arrange ##
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.HEAD, "http://dummy", u -> new HeadMockHttpURLConnection(u, 500));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(500, response.getHttpStatusCode());
            String content = response.getContentAsString();
            assertEquals("", content);
        }
    }

    // --- Null error stream tests ---

    /**
     * Mock HttpURLConnection that returns null from getErrorStream().
     */
    class NullErrorStreamMockHttpURLConnection extends HttpURLConnection {
        NullErrorStreamMockHttpURLConnection(URL u) {
            super(u);
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }

        @Override
        public int getResponseCode() {
            return 500;
        }

        @Override
        public InputStream getErrorStream() {
            return null; // Simulates server returning no error body
        }

        @Override
        public String getContentEncoding() {
            return null;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }
    }

    @Test
    public void test_NullErrorStream_DoesNotThrowNPE() throws Exception {
        // ## Arrange ##
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new NullErrorStreamMockHttpURLConnection(u));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(500, response.getHttpStatusCode());
            String content = response.getContentAsString();
            assertEquals("", content);
        }
    }

    // --- Error stream with content tests ---

    /**
     * Mock HttpURLConnection that returns error body from getErrorStream().
     */
    class ErrorBodyMockHttpURLConnection extends HttpURLConnection {
        private final int statusCode;
        private final String errorBody;

        ErrorBodyMockHttpURLConnection(URL u, int statusCode, String errorBody) {
            super(u);
            this.statusCode = statusCode;
            this.errorBody = errorBody;
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }

        @Override
        public int getResponseCode() {
            return statusCode;
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(errorBody.getBytes());
        }

        @Override
        public String getContentEncoding() {
            return null;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }
    }

    @Test
    public void test_ErrorResponseWithBody_ReturnsErrorBody() throws Exception {
        // ## Arrange ##
        String errorBody = "{\"error\":\"Internal Server Error\"}";
        CurlRequest req =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new ErrorBodyMockHttpURLConnection(u, 500, errorBody));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(500, response.getHttpStatusCode());
            assertEquals(errorBody, response.getContentAsString());
        }
    }

    @Test
    public void test_404ResponseWithBody_ReturnsErrorBody() throws Exception {
        // ## Arrange ##
        String errorBody = "Not Found";
        CurlRequest req =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new ErrorBodyMockHttpURLConnection(u, 404, errorBody));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(404, response.getHttpStatusCode());
            assertEquals(errorBody, response.getContentAsString());
        }
    }

    // --- Timeout application tests (using open(URL) override to test production connect() path) ---

    /**
     * Mock HttpURLConnection that records timeout values.
     */
    class TimeoutRecordingMockHttpURLConnection extends HttpURLConnection {
        int recordedConnectTimeout = -999;
        int recordedReadTimeout = -999;

        TimeoutRecordingMockHttpURLConnection(URL u) {
            super(u);
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }

        @Override
        public int getResponseCode() {
            return 200;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream("ok".getBytes());
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }

        @Override
        public void setConnectTimeout(int timeout) {
            this.recordedConnectTimeout = timeout;
            super.setConnectTimeout(timeout);
        }

        @Override
        public void setReadTimeout(int timeout) {
            this.recordedReadTimeout = timeout;
            super.setReadTimeout(timeout);
        }
    }

    /**
     * CurlRequest subclass that overrides only open(URL) to inject a mock connection.
     * This ensures the real connect() code path is exercised (timeout, headers, onConnect, etc.).
     */
    class OpenOverrideCurlRequest extends CurlRequest {
        private final MockConnectionFactory connectionFactory;

        OpenOverrideCurlRequest(Curl.Method method, String url, MockConnectionFactory factory) {
            super(method, url);
            this.connectionFactory = factory;
        }

        @Override
        protected HttpURLConnection open(final URL u) throws IOException {
            return connectionFactory.create(u);
        }
    }

    @Test
    public void test_TimeoutAppliedToConnection() throws Exception {
        // ## Arrange ##
        final TimeoutRecordingMockHttpURLConnection[] mockHolder = new TimeoutRecordingMockHttpURLConnection[1];
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            TimeoutRecordingMockHttpURLConnection mock = new TimeoutRecordingMockHttpURLConnection(u);
            mockHolder[0] = mock;
            return mock;
        });
        req.timeout(3000, 5000);

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertNotNull(mockHolder[0]);
            assertEquals(3000, mockHolder[0].recordedConnectTimeout);
            assertEquals(5000, mockHolder[0].recordedReadTimeout);
        }
    }

    @Test
    public void test_TimeoutNotSetWhenDefault() throws Exception {
        // ## Arrange ##
        final TimeoutRecordingMockHttpURLConnection[] mockHolder = new TimeoutRecordingMockHttpURLConnection[1];
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            TimeoutRecordingMockHttpURLConnection mock = new TimeoutRecordingMockHttpURLConnection(u);
            mockHolder[0] = mock;
            return mock;
        });
        // Do NOT set timeout - should remain at default

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertNotNull(mockHolder[0]);
            // Should not have been called (still at initial -999)
            assertEquals(-999, mockHolder[0].recordedConnectTimeout);
            assertEquals(-999, mockHolder[0].recordedReadTimeout);
        }
    }

    @Test
    public void test_TimeoutZeroAppliedToConnection() throws Exception {
        // ## Arrange ##
        final TimeoutRecordingMockHttpURLConnection[] mockHolder = new TimeoutRecordingMockHttpURLConnection[1];
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            TimeoutRecordingMockHttpURLConnection mock = new TimeoutRecordingMockHttpURLConnection(u);
            mockHolder[0] = mock;
            return mock;
        });
        req.timeout(0, 0); // 0 means infinite timeout in HttpURLConnection

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertNotNull(mockHolder[0]);
            assertEquals(0, mockHolder[0].recordedConnectTimeout);
            assertEquals(0, mockHolder[0].recordedReadTimeout);
        }
    }

    @Test
    public void test_TimeoutWithOnConnectOverride() throws Exception {
        // ## Arrange ##
        final AtomicInteger onConnectReadTimeout = new AtomicInteger(-1);
        final TimeoutRecordingMockHttpURLConnection[] mockHolder = new TimeoutRecordingMockHttpURLConnection[1];
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            TimeoutRecordingMockHttpURLConnection mock = new TimeoutRecordingMockHttpURLConnection(u);
            mockHolder[0] = mock;
            return mock;
        });
        req.timeout(3000, 5000);
        // onConnect runs AFTER timeout, so it can override
        req.onConnect((r, conn) -> {
            conn.setReadTimeout(9999);
            onConnectReadTimeout.set(conn.getReadTimeout());
        });

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            // timeout() was applied first (3000, 5000)
            // onConnect then overrode readTimeout to 9999
            assertEquals(3000, mockHolder[0].recordedConnectTimeout);
            assertEquals(9999, onConnectReadTimeout.get());
        }
    }

    @Test
    public void test_NegativeTimeoutTreatedAsUnset() throws Exception {
        // ## Arrange ##
        final TimeoutRecordingMockHttpURLConnection[] mockHolder = new TimeoutRecordingMockHttpURLConnection[1];
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            TimeoutRecordingMockHttpURLConnection mock = new TimeoutRecordingMockHttpURLConnection(u);
            mockHolder[0] = mock;
            return mock;
        });
        req.timeout(-2, -3); // Any negative value should be treated as "not set"

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertNotNull(mockHolder[0]);
            // Negative values should not trigger setConnectTimeout/setReadTimeout
            assertEquals(-999, mockHolder[0].recordedConnectTimeout);
            assertEquals(-999, mockHolder[0].recordedReadTimeout);
        }
    }

    // --- URL special characters through real connect() path ---

    @Test
    public void test_UrlWithSpecialCharacters_ThroughRealConnectPath() throws Exception {
        // ## Arrange ##
        // URLs with special characters (spaces, curly braces, pipes) must be accepted
        // by the real connect() path using new URL()
        String[] specialUrls = { "http://localhost:9200/{index}/_search", "http://example.com/path?q=a|b",
                "http://example.com/page#section", "http://user:pass@example.com/path", "http://localhost:9200/_cluster/health" };

        for (String specialUrl : specialUrls) {
            final AtomicInteger openCalled = new AtomicInteger(0);
            CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, specialUrl, u -> {
                openCalled.incrementAndGet();
                return new MockHttpURLConnection(u);
            });

            // ## Act ##
            try (CurlResponse response = req.execute()) {
                // ## Assert ##
                // The request should reach open() without URL parsing failure
                assertEquals("URL failed: " + specialUrl, 1, openCalled.get());
                assertEquals(200, response.getHttpStatusCode());
            }
        }
    }

    // --- Success response tests ---

    @Test
    public void test_SuccessResponseWithBody_ReturnsBody() throws Exception {
        // ## Arrange ##
        CurlRequest req = new MockCurlRequest(Curl.Method.GET, "http://dummy");

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            assertNotNull(response.getContentAsString());
        }
    }

    // --- Helper: MockConnectionFactory ---

    interface MockConnectionFactory {
        HttpURLConnection create(URL url) throws IOException;
    }
}
