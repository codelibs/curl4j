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
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import static org.codelibs.curl.io.ContentOutputStream.PREFIX;
import static org.codelibs.curl.io.ContentOutputStream.SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    public void test_SynchronousExecute_RestoresThreadPool() throws Exception {
        // ## Arrange ##
        CurlRequest req =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new OutputCapturingMockHttpURLConnection(u, "ok"));
        ForkJoinPool pool = new ForkJoinPool(1);
        req.threadPool(pool);

        // ## Act ##
        // execute() should force synchronous execution but restore threadPool afterward
        try (CurlResponse response = req.execute()) {
            assertEquals(200, response.getHttpStatusCode());
        }

        // ## Assert ##
        // After execute(), the threadPool should be restored
        // We verify by running async execute which should use the pool
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();
        CurlRequest req2 =
                new OpenOverrideCurlRequest(Curl.Method.GET, "http://dummy", u -> new OutputCapturingMockHttpURLConnection(u, "ok"));
        req2.threadPool(pool);
        req2.execute(response -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        }, e -> latch.countDown());
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();
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
