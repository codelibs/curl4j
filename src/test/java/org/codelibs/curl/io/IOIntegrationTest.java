package org.codelibs.curl.io;

import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlRequest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.codelibs.curl.io.ContentOutputStream.PREFIX;
import static org.codelibs.curl.io.ContentOutputStream.SUFFIX;
import static org.junit.Assert.assertEquals;

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
        return Arrays.stream(Objects.requireNonNull(Curl.tmpDir.listFiles()))
                .map(File::getName)
                .filter(s -> s.startsWith(PREFIX) && s.endsWith(SUFFIX))
                .count();
    }
}
