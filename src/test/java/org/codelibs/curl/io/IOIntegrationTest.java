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
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlRequest;
import org.codelibs.curl.CurlResponse;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import static org.codelibs.curl.io.ContentOutputStream.PREFIX;
import static org.codelibs.curl.io.ContentOutputStream.SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

        java.util.concurrent.ForkJoinPool currentThreadPool() {
            return threadPool; // inherited protected field
        }
    }

    // A CurlRequest built via the 1-arg constructor (url stays null) that records open() calls.
    class NullUrlRecordingCurlRequest extends CurlRequest {
        boolean openCalled = false;

        NullUrlRecordingCurlRequest(Curl.Method method) {
            super(method);
        }

        @Override
        protected HttpURLConnection open(final URL u) throws IOException {
            openCalled = true;
            return new OutputCapturingMockHttpURLConnection(u, "ok");
        }
    }

    @Test
    public void test_AssemblyFailure_RoutedToExceptionListener() throws Exception {
        // ## Arrange ##
        // With a null url and params present, query-string assembly throws (NPE). Because assembly
        // now runs inside the try block, the failure must be wrapped and routed to the exception
        // listener instead of escaping raw, and open() must never be reached.
        NullUrlRecordingCurlRequest req = new NullUrlRecordingCurlRequest(Curl.Method.GET);
        req.param("k", "v");
        final AtomicReference<Exception> captured = new AtomicReference<>();

        // ## Act ##
        req.execute(res -> fail("action listener must not be invoked"), captured::set);

        // ## Assert ##
        assertNotNull(captured.get());
        assertTrue("expected CurlException, got " + captured.get(), captured.get() instanceof CurlException);
        assertFalse("open() must not be called when assembly fails", req.openCalled);
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
        // Uses the open(URL) override so that execute() exercises the real connect() path.
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new MockHttpURLConnection(u));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            assertNotNull(response.getContentAsString());
        }
    }

    // --- GZIP response tests ---

    /**
     * Helper to GZIP-compress a string.
     */
    private byte[] gzipCompress(String text) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    /**
     * Mock HttpURLConnection that returns GZIP-compressed content for successful responses.
     */
    class GzipSuccessMockHttpURLConnection extends HttpURLConnection {
        private final byte[] gzippedData;

        GzipSuccessMockHttpURLConnection(URL u, byte[] gzippedData) {
            super(u);
            this.gzippedData = gzippedData;
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
        public String getContentEncoding() {
            return "gzip";
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(gzippedData);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }
    }

    @Test
    public void test_GzipSuccessResponse_DecompressedCorrectly() throws Exception {
        // ## Arrange ##
        String originalBody = "Hello, GZIP world!";
        byte[] gzipped = gzipCompress(originalBody);
        CurlRequest req =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new GzipSuccessMockHttpURLConnection(u, gzipped));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            assertEquals(originalBody, response.getContentAsString());
        }
    }

    /**
     * Mock HttpURLConnection that returns GZIP-compressed error body.
     */
    class GzipErrorMockHttpURLConnection extends HttpURLConnection {
        private final byte[] gzippedData;
        private final int statusCode;

        GzipErrorMockHttpURLConnection(URL u, int statusCode, byte[] gzippedData) {
            super(u);
            this.statusCode = statusCode;
            this.gzippedData = gzippedData;
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
        public String getContentEncoding() {
            return "gzip";
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new IOException("Error stream should be used for status " + statusCode);
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(gzippedData);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }
    }

    @Test
    public void test_GzipErrorResponse_DecompressedCorrectly() throws Exception {
        // ## Arrange ##
        String errorBody = "{\"error\":\"Server Error\"}";
        byte[] gzipped = gzipCompress(errorBody);
        CurlRequest req =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new GzipErrorMockHttpURLConnection(u, 500, gzipped));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(500, response.getHttpStatusCode());
            assertEquals(errorBody, response.getContentAsString());
        }
    }

    @Test
    public void test_GzipErrorResponse404_DecompressedCorrectly() throws Exception {
        // ## Arrange ##
        String errorBody = "Not Found";
        byte[] gzipped = gzipCompress(errorBody);
        CurlRequest req =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new GzipErrorMockHttpURLConnection(u, 404, gzipped));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(404, response.getHttpStatusCode());
            assertEquals(errorBody, response.getContentAsString());
        }
    }

    /**
     * Mock HttpURLConnection that returns GZIP-compressed content with a configurable
     * Content-Encoding header value (e.g. "GZIP", "x-gzip").
     */
    class ConfigurableGzipMockHttpURLConnection extends HttpURLConnection {
        private final byte[] gzippedData;
        private final String contentEncoding;

        ConfigurableGzipMockHttpURLConnection(URL u, String contentEncoding, byte[] gzippedData) {
            super(u);
            this.contentEncoding = contentEncoding;
            this.gzippedData = gzippedData;
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
        public String getContentEncoding() {
            return contentEncoding;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(gzippedData);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }
    }

    @Test
    public void test_GzipResponse_UppercaseEncoding_DecompressedCorrectly() throws Exception {
        // ## Arrange ##
        // "GZIP" (uppercase) must still be recognized as gzip.
        String originalBody = "Hello, uppercase GZIP!";
        byte[] gzipped = gzipCompress(originalBody);
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ConfigurableGzipMockHttpURLConnection(u, "GZIP", gzipped));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            assertEquals(originalBody, response.getContentAsString());
        }
    }

    @Test
    public void test_GzipResponse_XGzipEncoding_DecompressedCorrectly() throws Exception {
        // ## Arrange ##
        // "x-gzip" must also be recognized as gzip.
        String originalBody = "Hello, x-gzip!";
        byte[] gzipped = gzipCompress(originalBody);
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ConfigurableGzipMockHttpURLConnection(u, "x-gzip", gzipped));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            assertEquals(originalBody, response.getContentAsString());
        }
    }

    @Test
    public void test_GzipResponse_MixedCaseXGzipEncoding() throws Exception {
        // ## Arrange ##
        // "X-GZIP" (uppercase) must still be recognized as gzip.
        String originalBody = "hello X-GZIP";
        byte[] gzipped = gzipCompress(originalBody);
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ConfigurableGzipMockHttpURLConnection(u, "X-GZIP", gzipped));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            assertEquals(originalBody, response.getContentAsString());
        }
    }

    // --- Response Content-Type charset tests ---

    /**
     * Mock HttpURLConnection that declares a charset in its Content-Type and returns a body
     * encoded with that charset.
     */
    class ContentTypeCharsetMockHttpURLConnection extends HttpURLConnection {
        private final String contentType;
        private final byte[] body;

        ContentTypeCharsetMockHttpURLConnection(URL u, String contentType, byte[] body) {
            super(u);
            this.contentType = contentType;
            this.body = body;
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
        public String getContentType() {
            return contentType;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }
    }

    @Test
    public void test_ResponseContentTypeCharset_UsedForDecoding() throws Exception {
        // ## Arrange ##
        // The request-side encoding stays at the default UTF-8; only the response Content-Type
        // declares Shift_JIS. Decoding must honor the response charset without the caller
        // calling encoding().
        String text = "こんにちは世界";
        byte[] shiftJisBytes = text.getBytes("Shift_JIS");
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ContentTypeCharsetMockHttpURLConnection(u, "text/html; charset=Shift_JIS", shiftJisBytes));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals("Shift_JIS", response.getEncoding());
            assertEquals(text, response.getContentAsString());
        }
    }

    @Test
    public void test_ResponseWithoutContentTypeCharset_FallsBackToRequestEncoding() throws Exception {
        // ## Arrange ##
        // No charset in Content-Type: decoding falls back to the request-side encoding (UTF-8).
        String text = "plain ascii body";
        byte[] utf8Bytes = text.getBytes(StandardCharsets.UTF_8);
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ContentTypeCharsetMockHttpURLConnection(u, "text/plain", utf8Bytes));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals("UTF-8", response.getEncoding());
            assertEquals(text, response.getContentAsString());
        }
    }

    @Test
    public void test_ResponseContentTypeCharset_DoubleQuoted_UsedForDecoding() throws Exception {
        // ## Arrange ##
        // A charset wrapped in double quotes must be unquoted and used for decoding.
        String text = "こんにちは世界";
        byte[] shiftJisBytes = text.getBytes("Shift_JIS");
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ContentTypeCharsetMockHttpURLConnection(u, "text/html; charset=\"Shift_JIS\"", shiftJisBytes));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals("Shift_JIS", response.getEncoding());
            assertEquals(text, response.getContentAsString());
        }
    }

    @Test
    public void test_ResponseContentTypeCharset_SingleQuoted_UsedForDecoding() throws Exception {
        // ## Arrange ##
        // A charset wrapped in single quotes must be unquoted and used for decoding.
        String text = "こんにちは世界";
        byte[] shiftJisBytes = text.getBytes("Shift_JIS");
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ContentTypeCharsetMockHttpURLConnection(u, "text/html; charset='Shift_JIS'", shiftJisBytes));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals("Shift_JIS", response.getEncoding());
            assertEquals(text, response.getContentAsString());
        }
    }

    @Test
    public void test_ResponseContentTypeCharset_UnsupportedCharset_FallsBackToRequestEncoding() throws Exception {
        // ## Arrange ##
        // An unsupported (but syntactically legal) charset must fall back to the request encoding
        // without throwing.
        String text = "hello";
        byte[] utf8Bytes = text.getBytes(StandardCharsets.UTF_8);
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ContentTypeCharsetMockHttpURLConnection(u, "text/plain; charset=NO-SUCH-CHARSET", utf8Bytes));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals("UTF-8", response.getEncoding());
            assertEquals(text, response.getContentAsString());
        }
    }

    @Test
    public void test_ResponseContentTypeCharset_IllegalCharsetName_FallsBackToRequestEncoding() throws Exception {
        // ## Arrange ##
        // An illegal charset name must be caught and fall back to the request encoding without
        // throwing.
        String text = "hello";
        byte[] utf8Bytes = text.getBytes(StandardCharsets.UTF_8);
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ContentTypeCharsetMockHttpURLConnection(u, "text/plain; charset=@illegal@", utf8Bytes));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals("UTF-8", response.getEncoding());
            assertEquals(text, response.getContentAsString());
        }
    }

    @Test
    public void test_ExplicitRequestEncoding_TakesPrecedenceOverResponseCharset() throws Exception {
        // ## Arrange ##
        // The caller explicitly set the encoding via encoding("Shift_JIS"). Even though the server
        // declares a different charset (UTF-8), the caller's explicit choice must win so a
        // mis-declaring server can still be overridden. The body is Shift_JIS-encoded.
        String text = "こんにちは世界";
        byte[] shiftJisBytes = text.getBytes("Shift_JIS");
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ContentTypeCharsetMockHttpURLConnection(u, "text/html; charset=UTF-8", shiftJisBytes));
        req.encoding("Shift_JIS");

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals("Shift_JIS", response.getEncoding());
            assertEquals(text, response.getContentAsString());
        }
    }

    @Test
    public void test_ResponseContentTypeCharset_WithTrailingParameter_UsedForDecoding() throws Exception {
        // ## Arrange ##
        // charset is not the last parameter; the trailing "; boundary=xyz" must not be glued onto
        // the charset value (guards against a naive indexOf-based parser).
        String text = "こんにちは世界";
        byte[] shiftJisBytes = text.getBytes("Shift_JIS");
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy",
                u -> new ContentTypeCharsetMockHttpURLConnection(u, "text/html; charset=Shift_JIS; boundary=xyz", shiftJisBytes));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals("Shift_JIS", response.getEncoding());
            assertEquals(text, response.getContentAsString());
        }
    }

    // --- InputStream body writing test ---

    /**
     * Mock HttpURLConnection that captures output stream writes.
     */
    class OutputCapturingMockHttpURLConnection extends HttpURLConnection {
        private final ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        private final String responseBody;

        OutputCapturingMockHttpURLConnection(URL u, String responseBody) {
            super(u);
            this.responseBody = responseBody;
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
        public OutputStream getOutputStream() {
            return capturedOutput;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }

        public byte[] getCapturedOutput() {
            return capturedOutput.toByteArray();
        }
    }

    @Test
    public void test_InputStreamBody_WrittenToConnection() throws Exception {
        // ## Arrange ##
        String bodyContent = "stream body content";
        byte[] bodyBytes = bodyContent.getBytes(StandardCharsets.UTF_8);
        final OutputCapturingMockHttpURLConnection[] mockHolder = new OutputCapturingMockHttpURLConnection[1];
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.POST, "http://dummy", u -> {
            OutputCapturingMockHttpURLConnection mock = new OutputCapturingMockHttpURLConnection(u, "ok");
            mockHolder[0] = mock;
            return mock;
        });
        req.body(new ByteArrayInputStream(bodyBytes));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            assertNotNull(mockHolder[0]);
            assertTrue(Arrays.equals(bodyBytes, mockHolder[0].getCapturedOutput()));
        }
    }

    @Test
    public void test_StringBody_WrittenToConnection() throws Exception {
        // ## Arrange ##
        String bodyContent = "string body content";
        final OutputCapturingMockHttpURLConnection[] mockHolder = new OutputCapturingMockHttpURLConnection[1];
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.POST, "http://dummy", u -> {
            OutputCapturingMockHttpURLConnection mock = new OutputCapturingMockHttpURLConnection(u, "ok");
            mockHolder[0] = mock;
            return mock;
        });
        req.body(bodyContent);

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            assertNotNull(mockHolder[0]);
            assertEquals(bodyContent, new String(mockHolder[0].getCapturedOutput(), StandardCharsets.UTF_8));
        }
    }

    // --- Async thread pool execution test ---

    @Test
    public void test_AsyncThreadPoolExecution() throws Exception {
        // ## Arrange ##
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            TimeoutRecordingMockHttpURLConnection mock = new TimeoutRecordingMockHttpURLConnection(u);
            return mock;
        });
        ForkJoinPool pool = new ForkJoinPool(1);
        req.threadPool(pool);

        // ## Act ##
        req.execute(response -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        }, e -> {
            latch.countDown();
        });

        // ## Assert ##
        assertTrue("Async execution should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS));
        assertNotNull(threadName.get());
        // The async execution should have run on a ForkJoinPool thread, not the main thread
        assertTrue("Should run on ForkJoinPool thread: " + threadName.get(),
                threadName.get().contains("ForkJoinPool") || !threadName.get().equals(Thread.currentThread().getName()));
        pool.shutdown();
    }

    // --- Synchronous execute() test ---

    @Test
    public void test_SynchronousExecute_ReturnsResponse() throws Exception {
        // ## Arrange ##
        String body = "sync response body";
        CurlRequest req =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new OutputCapturingMockHttpURLConnection(u, body));

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            assertEquals(body, response.getContentAsString());
        }
    }

    @Test
    public void test_SynchronousExecute_DoesNotMutateThreadPoolField() throws Exception {
        // ## Arrange ##
        // execute() must run synchronously on the calling thread WITHOUT transiently nulling the
        // shared threadPool field. (The old save/restore approach nulled it and was not thread-safe.)
        ForkJoinPool pool = new ForkJoinPool(1);
        try {
            final OpenOverrideCurlRequest[] holder = new OpenOverrideCurlRequest[1];
            final AtomicReference<ForkJoinPool> fieldDuringExec = new AtomicReference<>();
            final AtomicReference<String> execThread = new AtomicReference<>();
            holder[0] = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
                fieldDuringExec.set(holder[0].currentThreadPool());
                execThread.set(Thread.currentThread().getName());
                return new OutputCapturingMockHttpURLConnection(u, "ok");
            });
            holder[0].threadPool(pool);

            // ## Act ##
            try (CurlResponse response = holder[0].execute()) {
                assertEquals(200, response.getHttpStatusCode());
            }

            // ## Assert ##
            // The field must NOT have been nulled during synchronous execution...
            assertSame(pool, fieldDuringExec.get());
            // ...and execution ran on the calling thread, not a pool thread.
            assertEquals(Thread.currentThread().getName(), execThread.get());
        } finally {
            pool.shutdown();
        }
    }

    // --- execute(Consumer, Consumer) tests ---

    @Test
    public void test_ExecuteWithListeners_Success() throws Exception {
        // ## Arrange ##
        String body = "listener response";
        CurlRequest req =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new OutputCapturingMockHttpURLConnection(u, body));
        AtomicReference<String> receivedBody = new AtomicReference<>();
        AtomicInteger statusCode = new AtomicInteger();

        // ## Act ##
        req.execute(response -> {
            statusCode.set(response.getHttpStatusCode());
            receivedBody.set(response.getContentAsString());
        }, e -> fail("Should not throw exception: " + e.getMessage()));

        // ## Assert ##
        assertEquals(200, statusCode.get());
        assertEquals(body, receivedBody.get());
    }

    @Test
    public void test_ExecuteWithListeners_ExceptionHandling() throws Exception {
        // ## Arrange ##
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            throw new IOException("Connection failed");
        });
        AtomicReference<Exception> receivedException = new AtomicReference<>();

        // ## Act ##
        req.execute(response -> fail("Should not succeed"), e -> receivedException.set(e));

        // ## Assert ##
        assertNotNull(receivedException.get());
        assertTrue(receivedException.get() instanceof CurlException);
        assertTrue(receivedException.get().getMessage().contains("Failed to access"));
    }

    // --- URL parameter concatenation tests ---

    /**
     * Mock that records the URL it receives.
     */
    class UrlRecordingMockHttpURLConnection extends HttpURLConnection {
        UrlRecordingMockHttpURLConnection(URL u) {
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
    }

    @Test
    public void test_ParamsAppendedToUrlWithoutQueryString() throws Exception {
        // ## Arrange ##
        final AtomicReference<URL> capturedUrl = new AtomicReference<>();
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://example.com/path", u -> {
            capturedUrl.set(u);
            return new UrlRecordingMockHttpURLConnection(u);
        });
        req.param("key1", "value1").param("key2", "value2");

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            String urlStr = capturedUrl.get().toString();
            assertTrue("URL should contain ?key1=value1: " + urlStr, urlStr.contains("?key1=value1"));
            assertTrue("URL should contain &key2=value2: " + urlStr, urlStr.contains("&key2=value2"));
        }
    }

    @Test
    public void test_ParamsAppendedToUrlWithExistingQueryString() throws Exception {
        // ## Arrange ##
        final AtomicReference<URL> capturedUrl = new AtomicReference<>();
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://example.com/path?existing=true", u -> {
            capturedUrl.set(u);
            return new UrlRecordingMockHttpURLConnection(u);
        });
        req.param("key1", "value1");

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            String urlStr = capturedUrl.get().toString();
            assertTrue("URL should start with existing params: " + urlStr, urlStr.contains("?existing=true"));
            assertTrue("Additional param should use &: " + urlStr, urlStr.contains("&key1=value1"));
        }
    }

    @Test
    public void test_ParamsAppendedToUrlWithTrailingQuestionMark() throws Exception {
        // ## Arrange ##
        // A URL that already ends with '?' must not gain an extra '&' before the first param.
        final AtomicReference<URL> capturedUrl = new AtomicReference<>();
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://example.com/path?", u -> {
            capturedUrl.set(u);
            return new UrlRecordingMockHttpURLConnection(u);
        });
        req.param("key", "value").param("key2", "value2");

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            String urlStr = capturedUrl.get().toString();
            assertTrue("First param should follow '?' directly: " + urlStr, urlStr.contains("path?key=value"));
            assertTrue("Second param should use '&': " + urlStr, urlStr.contains("&key2=value2"));
            assertTrue("URL should not contain '?&': " + urlStr, !urlStr.contains("?&"));
        }
    }

    @Test
    public void test_ParamsAppendedToUrlWithTrailingAmpersand() throws Exception {
        // ## Arrange ##
        // A URL that already ends with '&' must not gain an extra '&' before the first param.
        final AtomicReference<URL> capturedUrl = new AtomicReference<>();
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://example.com/path?a=b&", u -> {
            capturedUrl.set(u);
            return new UrlRecordingMockHttpURLConnection(u);
        });
        req.param("key", "value");

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            String urlStr = capturedUrl.get().toString();
            assertTrue("First param should follow trailing '&' directly: " + urlStr, urlStr.contains("a=b&key=value"));
            assertTrue("URL should not contain '&&': " + urlStr, !urlStr.contains("&&"));
        }
    }

    // --- File-based content cache test (large response) ---

    /**
     * Mock HttpURLConnection that returns a large response body.
     */
    class LargeResponseMockHttpURLConnection extends HttpURLConnection {
        private final int responseSize;

        LargeResponseMockHttpURLConnection(URL u, int responseSize) {
            super(u);
            this.responseSize = responseSize;
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
            byte[] data = new byte[responseSize];
            Arrays.fill(data, (byte) 'A');
            return new ByteArrayInputStream(data);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }
    }

    @Test
    public void test_LargeResponse_UsesFileBasedCache() throws Exception {
        // ## Arrange ##
        int responseSize = 2 * 1024 * 1024; // 2MB, exceeds default 1MB threshold
        CurlRequest req =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new LargeResponseMockHttpURLConnection(u, responseSize));

        long tmpFilesBefore = countTmpFiles();

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            String content = response.getContentAsString();
            assertEquals(responseSize, content.length());
            // All characters should be 'A'
            for (int i = 0; i < 100; i++) {
                assertEquals('A', content.charAt(i));
            }
        }

        // After close, temp files should be cleaned up
        long tmpFilesAfter = countTmpFiles();
        assertEquals(tmpFilesBefore, tmpFilesAfter);
    }

    @Test
    public void test_SmallThresholdResponse_UsesFileBasedCache() throws Exception {
        // ## Arrange ##
        String body = "small body but threshold is 0";
        CurlRequest req =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new OutputCapturingMockHttpURLConnection(u, body));
        req.threshold(0); // Force file-based caching

        long tmpFilesBefore = countTmpFiles();

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(200, response.getHttpStatusCode());
            assertEquals(body, response.getContentAsString());
        }

        // After close, temp files should be cleaned up
        long tmpFilesAfter = countTmpFiles();
        assertEquals(tmpFilesBefore, tmpFilesAfter);
    }

    // --- Connect exception handling test ---

    @Test
    public void test_ConnectException_WrappedInCurlException() throws Exception {
        // ## Arrange ##
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            throw new IOException("Simulated connection failure");
        });

        // ## Act & Assert ##
        try {
            req.execute();
            fail("Should throw CurlException");
        } catch (CurlException e) {
            assertTrue(e.getMessage().contains("Failed to"));
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void test_ConnectException_ViaExceptionListener() throws Exception {
        // ## Arrange ##
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            throw new IOException("Simulated connection failure");
        });
        AtomicReference<Exception> receivedException = new AtomicReference<>();

        // ## Act ##
        req.connect(con -> fail("Should not reach action listener"), e -> receivedException.set(e));

        // ## Assert ##
        assertNotNull(receivedException.get());
        assertTrue(receivedException.get() instanceof CurlException);
        assertTrue(receivedException.get().getMessage().contains("Failed to access"));
    }

    // --- Compression and request headers verification tests ---

    /**
     * Mock HttpURLConnection that records headers set on the connection.
     */
    class HeaderRecordingMockHttpURLConnection extends HttpURLConnection {
        private final Map<String, List<String>> requestProperties = new HashMap<>();

        HeaderRecordingMockHttpURLConnection(URL u) {
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
        public void addRequestProperty(String key, String value) {
            requestProperties.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        @Override
        public void setRequestProperty(String key, String value) {
            List<String> list = new ArrayList<>();
            list.add(value);
            requestProperties.put(key, list);
        }

        public Map<String, List<String>> getRecordedRequestProperties() {
            return requestProperties;
        }
    }

    @Test
    public void test_GzipCompression_SetsAcceptEncodingHeader() throws Exception {
        // ## Arrange ##
        final HeaderRecordingMockHttpURLConnection[] mockHolder = new HeaderRecordingMockHttpURLConnection[1];
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            HeaderRecordingMockHttpURLConnection mock = new HeaderRecordingMockHttpURLConnection(u);
            mockHolder[0] = mock;
            return mock;
        });
        req.gzip();

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            Map<String, List<String>> props = mockHolder[0].getRecordedRequestProperties();
            assertTrue("Accept-Encoding header should be set", props.containsKey("Accept-Encoding"));
            assertEquals("gzip", props.get("Accept-Encoding").get(0));
        }
    }

    @Test
    public void test_CustomHeaders_AppliedToConnection() throws Exception {
        // ## Arrange ##
        final HeaderRecordingMockHttpURLConnection[] mockHolder = new HeaderRecordingMockHttpURLConnection[1];
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            HeaderRecordingMockHttpURLConnection mock = new HeaderRecordingMockHttpURLConnection(u);
            mockHolder[0] = mock;
            return mock;
        });
        req.header("Content-Type", "application/json");
        req.header("Authorization", "Bearer token123");

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            Map<String, List<String>> props = mockHolder[0].getRecordedRequestProperties();
            assertTrue(props.containsKey("Content-Type"));
            assertEquals("application/json", props.get("Content-Type").get(0));
            assertTrue(props.containsKey("Authorization"));
            assertEquals("Bearer token123", props.get("Authorization").get(0));
        }
    }

    @Test
    public void test_CustomCompression_SetsAcceptEncodingHeader() throws Exception {
        // ## Arrange ##
        final HeaderRecordingMockHttpURLConnection[] mockHolder = new HeaderRecordingMockHttpURLConnection[1];
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
            HeaderRecordingMockHttpURLConnection mock = new HeaderRecordingMockHttpURLConnection(u);
            mockHolder[0] = mock;
            return mock;
        });
        req.compression("deflate");

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            Map<String, List<String>> props = mockHolder[0].getRecordedRequestProperties();
            assertTrue("Accept-Encoding header should be set", props.containsKey("Accept-Encoding"));
            assertEquals("deflate", props.get("Accept-Encoding").get(0));
        }
    }

    // --- Sensitive-header masking in the FINE log path ---

    // Records request properties set on the connection so the sent (unmasked) value can be asserted.
    class RequestPropertyRecordingMockHttpURLConnection extends HttpURLConnection {
        final Map<String, String> props = new HashMap<>();

        RequestPropertyRecordingMockHttpURLConnection(URL u) {
            super(u);
        }

        @Override
        public void addRequestProperty(String key, String value) {
            props.put(key, value);
        }

        @Override
        public void setRequestProperty(String key, String value) {
            props.put(key, value);
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
            return new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }
    }

    @Test
    public void test_SensitiveHeaderMaskedInFineLog_ButSentUnmasked() throws Exception {
        // ## Arrange ##
        final String token = "Bearer SECRET-TOKEN-VALUE";
        final Logger curlLogger = Logger.getLogger(CurlRequest.class.getName());
        final Level originalLevel = curlLogger.getLevel();
        final boolean originalUseParent = curlLogger.getUseParentHandlers();
        final List<String> records = Collections.synchronizedList(new ArrayList<>());
        final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord r) {
                records.add(r.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.FINE);
        curlLogger.setLevel(Level.FINE);
        curlLogger.setUseParentHandlers(false);
        curlLogger.addHandler(handler);
        final RequestPropertyRecordingMockHttpURLConnection[] mock = new RequestPropertyRecordingMockHttpURLConnection[1];
        try {
            CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> {
                mock[0] = new RequestPropertyRecordingMockHttpURLConnection(u);
                return mock[0];
            });
            req.header("Authorization", token);

            // ## Act ##
            try (CurlResponse response = req.execute()) {
                assertEquals(200, response.getHttpStatusCode());
            }

            // ## Assert ##
            // The FINE log masks the credential...
            boolean sawMaskedAuth = records.stream().anyMatch(m -> m != null && m.contains("Authorization=***"));
            boolean leakedToken = records.stream().anyMatch(m -> m != null && m.contains("SECRET-TOKEN-VALUE"));
            assertTrue("expected a masked Authorization line in FINE log; got: " + records, sawMaskedAuth);
            assertFalse("credential must not appear in any log record; got: " + records, leakedToken);
            // ...but the real value is still sent on the wire.
            assertEquals(token, mock[0].props.get("Authorization"));
        } finally {
            curlLogger.removeHandler(handler);
            curlLogger.setLevel(originalLevel);
            curlLogger.setUseParentHandlers(originalUseParent);
        }
    }

    // --- connectionBuilder callback verification ---

    @Test
    public void test_OnConnect_ReceivesBothRequestAndConnection() throws Exception {
        // ## Arrange ##
        AtomicReference<CurlRequest> receivedRequest = new AtomicReference<>();
        AtomicReference<HttpURLConnection> receivedConnection = new AtomicReference<>();
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new TimeoutRecordingMockHttpURLConnection(u));
        req.onConnect((r, conn) -> {
            receivedRequest.set(r);
            receivedConnection.set(conn);
        });

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertNotNull("CurlRequest should be passed to onConnect", receivedRequest.get());
            assertNotNull("HttpURLConnection should be passed to onConnect", receivedConnection.get());
            assertEquals(req, receivedRequest.get());
        }
    }

    // --- No params: URL unchanged test ---

    @Test
    public void test_NoParams_UrlUnchanged() throws Exception {
        // ## Arrange ##
        String originalUrl = "http://example.com/api/v1/resource";
        final AtomicReference<URL> capturedUrl = new AtomicReference<>();
        CurlRequest req = new OpenOverrideCurlRequest(Curl.Method.GET, originalUrl, u -> {
            capturedUrl.set(u);
            return new UrlRecordingMockHttpURLConnection(u);
        });

        // ## Act ##
        try (CurlResponse response = req.execute()) {
            // ## Assert ##
            assertEquals(originalUrl, capturedUrl.get().toString());
        }
    }

    // --- Helper: MockConnectionFactory ---

    interface MockConnectionFactory {
        HttpURLConnection create(URL url) throws IOException;
    }
}
