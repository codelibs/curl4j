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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;
import org.codelibs.curl.Curl.Method;
import org.codelibs.curl.io.ContentCache;
import org.codelibs.curl.io.ContentOutputStream;

/**
 * The CurlRequest class represents an HTTP request that can be configured and executed.
 * It supports various HTTP methods, request parameters, headers, body content, and more.
 */
public class CurlRequest {

    /**
     * The GZIP compression type.
     */
    protected static final String GZIP = "gzip";

    /**
     * Logger for logging request details.
     */
    protected static final Logger logger = Logger.getLogger(CurlRequest.class.getName());

    /**
     * The URL for the HTTP request.
     */
    protected String url;

    /**
     * The proxy to be used for the HTTP request.
     */
    protected Proxy proxy;

    /**
     * The character encoding for the request.
     */
    protected String encoding = "UTF-8";

    /**
     * The threshold size for the request body.
     */
    protected int threshold = 1024 * 1024; // 1MB

    /**
     * The HTTP method for the request.
     */
    protected final Method method;

    /**
     * The list of request parameters.
     */
    protected List<String> paramList;

    /**
     * The list of request headers.
     */
    protected List<String[]> headerList;

    /**
     * The body content of the request.
     */
    protected String body;

    /**
     * The input stream for the request body.
     */
    protected InputStream bodyStream;

    /**
     * The compression type for the request.
     */
    protected String compression = null;

    /**
     * The SSL socket factory for secure connections.
     */
    protected SSLSocketFactory sslSocketFactory = null;

    /**
     * The thread pool for executing the request.
     */
    protected ForkJoinPool threadPool;

    /**
     * The connection builder for customizing the connection.
     */
    private BiConsumer<CurlRequest, HttpURLConnection> connectionBuilder;

    /**
     * The connection timeout in milliseconds. A value of -1 means not set.
     */
    protected int connectTimeout = -1;

    /**
     * The read timeout in milliseconds. A value of -1 means not set.
     */
    protected int readTimeout = -1;

    /**
     * Constructs a new CurlRequest with the specified HTTP method.
     *
     * @param method the HTTP method
     * @throws IllegalArgumentException if method is null
     */
    public CurlRequest(final Method method) {
        if (method == null) {
            throw new IllegalArgumentException("method must not be null");
        }
        this.method = method;
    }

    /**
     * Constructs a new CurlRequest with the specified HTTP method and URL.
     *
     * @param method the HTTP method
     * @param url the URL
     * @throws IllegalArgumentException if method or url is null
     */
    public CurlRequest(final Method method, final String url) {
        this(method);
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        this.url = url;
    }

    /**
     * Returns the proxy for the request.
     *
     * @return the proxy
     */
    public Proxy proxy() {
        return proxy;
    }

    /**
     * Returns the character encoding for the request.
     *
     * @return the encoding
     */
    public String encoding() {
        return encoding;
    }

    /**
     * Returns the threshold size for the request body.
     *
     * @return the threshold
     */
    public int threshold() {
        return threshold;
    }

    /**
     * Returns the HTTP method for the request.
     *
     * @return the method
     */
    public Method method() {
        return method;
    }

    /**
     * Returns the body content of the request.
     *
     * @return the body
     */
    public String body() {
        return body;
    }

    /**
     * Sets the proxy for the request.
     *
     * @param proxy the proxy
     * @return this CurlRequest instance
     */
    public CurlRequest proxy(final Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * Sets the character encoding for the request.
     *
     * @param encoding the encoding
     * @return this CurlRequest instance
     * @throws CurlException if the method is called after the param method
     */
    public CurlRequest encoding(final String encoding) {
        if (paramList != null) {
            throw new CurlException("This method must be called before param method.");
        }
        this.encoding = encoding;
        return this;
    }

    /**
     * Sets the threshold size for the request body.
     *
     * @param threshold the threshold
     * @return this CurlRequest instance
     */
    public CurlRequest threshold(final int threshold) {
        this.threshold = threshold;
        return this;
    }

    /**
     * Enables GZIP compression for the request.
     *
     * @return this CurlRequest instance
     */
    public CurlRequest gzip() {
        return compression(GZIP);
    }

    /**
     * Sets the compression type for the request.
     *
     * @param compression the compression type
     * @return this CurlRequest instance
     */
    public CurlRequest compression(final String compression) {
        this.compression = compression;
        return this;
    }

    /**
     * Sets the SSL socket factory for secure connections.
     *
     * @param sslSocketFactory the SSL socket factory
     * @return this CurlRequest instance
     */
    public CurlRequest sslSocketFactory(final SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        return this;
    }

    /**
     * Sets the body content for the request.
     *
     * @param body the body content
     * @return this CurlRequest instance
     * @throws CurlException if the body method is already called
     */
    public CurlRequest body(final String body) {
        if (this.body != null || bodyStream != null) {
            throw new CurlException("body method is already called.");
        }
        this.body = body;
        return this;
    }

    /**
     * Sets the input stream for the request body.
     *
     * @param stream the input stream
     * @return this CurlRequest instance
     * @throws CurlException if the body method is already called
     */
    public CurlRequest body(final InputStream stream) {
        if (body != null || bodyStream != null) {
            throw new CurlException("body method is already called.");
        }
        this.bodyStream = stream;
        return this;
    }

    /**
     * Sets the connection builder for customizing the connection.
     *
     * @param connectionBuilder the connection builder
     * @return this CurlRequest instance
     */
    public CurlRequest onConnect(final BiConsumer<CurlRequest, HttpURLConnection> connectionBuilder) {
        this.connectionBuilder = connectionBuilder;
        return this;
    }

    /**
     * Adds a request parameter.
     *
     * @param key the parameter key
     * @param value the parameter value
     * @return this CurlRequest instance
     */
    public CurlRequest param(final String key, final String value) {
        if (value == null) {
            return this;
        }
        if (paramList == null) {
            paramList = new ArrayList<>();
        }
        paramList.add(encode(key) + "=" + encode(value));
        return this;
    }

    /**
     * Adds a request header.
     *
     * @param key the header key
     * @param value the header value
     * @return this CurlRequest instance
     */
    public CurlRequest header(final String key, final String value) {
        if (headerList == null) {
            headerList = new ArrayList<>();
        }
        headerList.add(new String[] { key, value });
        return this;
    }

    /**
     * Connects to the URL and executes the request.
     *
     * <p>If a thread pool has been configured via {@link #threadPool(ForkJoinPool)}, the request
     * runs asynchronously on that pool; otherwise it runs synchronously on the calling thread.</p>
     *
     * @param actionListener the action listener for handling the response
     * @param exceptionListener the exception listener for handling exceptions
     */
    public void connect(final Consumer<HttpURLConnection> actionListener, final Consumer<Exception> exceptionListener) {
        connect(actionListener, exceptionListener, false);
    }

    /**
     * Connects to the URL and executes the request, optionally forcing synchronous execution.
     *
     * @param actionListener the action listener for handling the response
     * @param exceptionListener the exception listener for handling exceptions
     * @param forceSync if {@code true}, the request always runs synchronously on the calling
     *            thread even when a thread pool has been configured; if {@code false}, the
     *            configured thread pool is used when present
     */
    private void connect(final Consumer<HttpURLConnection> actionListener, final Consumer<Exception> exceptionListener,
            final boolean forceSync) {
        final Runnable task = () -> {
            String targetUrl = url;
            HttpURLConnection connection = null;
            try {
                final String finalUrl;
                if (paramList != null) {
                    final StringBuilder urlBuf = new StringBuilder(100);
                    char sp;
                    final char lastChar = url.isEmpty() ? '\0' : url.charAt(url.length() - 1);
                    if (lastChar == '?' || lastChar == '&') {
                        // The URL already ends with a separator, so the first parameter needs none.
                        sp = '\0';
                    } else if (url.indexOf('?') == -1) {
                        sp = '?';
                    } else {
                        sp = '&';
                    }
                    for (final String param : paramList) {
                        if (sp != '\0') {
                            urlBuf.append(sp);
                        }
                        urlBuf.append(param);
                        sp = '&';
                    }
                    finalUrl = url + urlBuf.toString();
                } else {
                    finalUrl = url;
                }
                targetUrl = finalUrl;

                logger.fine(() -> ">>> " + method + " " + finalUrl);
                @SuppressWarnings("deprecation")
                final URL u = new URL(finalUrl);
                connection = open(u);
                connection.setRequestMethod(method.toString());
                if (connectTimeout >= 0) {
                    connection.setConnectTimeout(connectTimeout);
                }
                if (readTimeout >= 0) {
                    connection.setReadTimeout(readTimeout);
                }
                if (headerList != null) {
                    for (final String[] values : headerList) {
                        logger.fine(() -> ">>> " + values[0] + "=" + maskSensitiveHeader(values[0], values[1]));
                        connection.addRequestProperty(values[0], values[1]);
                    }
                }
                if (compression != null) {
                    connection.setRequestProperty("Accept-Encoding", compression);
                }

                if (connectionBuilder != null) {
                    connectionBuilder.accept(this, connection);
                }

                if (body != null) {
                    logger.fine(() -> ">>> " + body);
                    connection.setDoOutput(true);
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), encoding))) {
                        writer.write(body);
                        writer.flush();
                    }
                } else if (bodyStream != null) {
                    logger.fine(() -> ">>> <binary>");
                    connection.setDoOutput(true);
                    try (final OutputStream out = connection.getOutputStream()) {
                        IOUtils.copy(bodyStream, out);
                        out.flush();
                    }
                }

                actionListener.accept(connection);
            } catch (final Exception e) {
                exceptionListener.accept(new CurlException("Failed to access to " + targetUrl, e));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        };
        if (!forceSync && threadPool != null) {
            threadPool.execute(task);
        } else {
            task.run();
        }
    }

    /**
     * Masks the value of sensitive request headers so that credentials are not written to logs.
     *
     * <p>The value is replaced with {@code ***} for the following header names (case-insensitive):
     * {@code Authorization}, {@code Proxy-Authorization}, {@code Cookie} and {@code Set-Cookie}.
     * All other header values are returned unchanged.</p>
     *
     * <p>Only header <em>values</em> are masked. Credentials carried elsewhere in the request are
     * not masked and may still appear in {@code FINE} logs and in exception messages: query-string
     * parameters (which are part of the request URL) and the request body are logged verbatim.
     * Avoid placing secrets in the URL or body if log confidentiality is a concern.</p>
     *
     * @param key the header name
     * @param value the header value
     * @return the masked value for sensitive headers, otherwise the original value
     */
    protected static String maskSensitiveHeader(final String key, final String value) {
        if (key != null) {
            switch (key.toLowerCase(Locale.ROOT)) {
            case "authorization":
            case "proxy-authorization":
            case "cookie":
            case "set-cookie":
                return "***";
            default:
                break;
            }
        }
        return value;
    }

    /**
     * Determines whether the given content encoding denotes GZIP compression.
     *
     * <p>Both {@code gzip} and {@code x-gzip} are recognized, case-insensitively and ignoring
     * surrounding whitespace.</p>
     *
     * @param encoding the content encoding value, may be {@code null}
     * @return {@code true} if the encoding denotes GZIP, otherwise {@code false}
     */
    protected static boolean isGzipEncoding(final String encoding) {
        if (encoding == null) {
            return false;
        }
        final String trimmed = encoding.trim();
        return GZIP.equalsIgnoreCase(trimmed) || "x-gzip".equalsIgnoreCase(trimmed);
    }

    /**
     * Opens a connection to the specified URL.
     *
     * @param u the URL
     * @return the HttpURLConnection
     * @throws IOException if an I/O exception occurs
     */
    protected HttpURLConnection open(final URL u) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) (proxy != null ? u.openConnection(proxy) : u.openConnection());
        if (sslSocketFactory != null && connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        }
        return connection;
    }

    /**
     * Executes the request and processes the response.
     *
     * @param actionListener the action listener for handling the response
     * @param exceptionListener the exception listener for handling exceptions
     */
    public void execute(final Consumer<CurlResponse> actionListener, final Consumer<Exception> exceptionListener) {
        connect(con -> {
            final RequestProcessor processor = new RequestProcessor(encoding, threshold);
            processor.accept(con);
            try (final CurlResponse res = processor.getResponse()) {
                actionListener.accept(res);
            } catch (final IOException e) {
                exceptionListener.accept(e);
            }
        }, exceptionListener);
    }

    /**
     * Executes the request and returns the response.
     *
     * @return the CurlResponse
     */
    public CurlResponse execute() {
        final RequestProcessor processor = new RequestProcessor(encoding, threshold);
        connect(processor, e -> {
            throw new CurlException("Failed to process a request.", e);
        }, true);
        return processor.getResponse();
    }

    /**
     * Encodes the specified value using the character encoding.
     *
     * @param value the value to encode
     * @return the encoded value
     * @throws CurlException if the encoding is unsupported
     */
    protected String encode(final String value) {
        try {
            return URLEncoder.encode(value, encoding);
        } catch (final UnsupportedEncodingException e) {
            throw new CurlException("Invalid encoding: " + encoding, e);
        }
    }

    /**
     * Sets the thread pool for executing the request.
     *
     * @param threadPool the thread pool
     * @return this CurlRequest instance
     */
    public CurlRequest threadPool(final ForkJoinPool threadPool) {
        this.threadPool = threadPool;
        return this;
    }

    /**
     * Sets the connection and read timeouts for the request.
     *
     * <p>When {@code timeout()} is not called, the JDK defaults apply ({@code 0}, meaning no
     * timeout), so a request may block indefinitely while connecting or reading.</p>
     *
     * @param connectTimeout the connection timeout in milliseconds
     * @param readTimeout the read timeout in milliseconds
     * @return this CurlRequest instance
     */
    public CurlRequest timeout(final int connectTimeout, final int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * The RequestProcessor class processes the HTTP request and handles the response.
     */
    public static class RequestProcessor implements Consumer<HttpURLConnection> {
        /**
         * The CurlResponse object to store the response.
         */
        protected CurlResponse response = new CurlResponse();

        private String encoding;

        private int threshold;

        /**
         * Constructs a new RequestProcessor with the specified encoding and threshold.
         *
         * @param encoding the character encoding
         * @param threshold the threshold size
         */
        public RequestProcessor(final String encoding, final int threshold) {
            this.encoding = encoding;
            this.threshold = threshold;
        }

        /**
         * Returns the CurlResponse.
         *
         * @return the response
         */
        public CurlResponse getResponse() {
            return response;
        }

        /**
         * Processes the HTTP connection and handles the response.
         *
         * @param con the HttpURLConnection
         */
        @Override
        public void accept(final HttpURLConnection con) {
            try {
                // Prefer the charset declared by the response Content-Type; fall back to the
                // request-side encoding when it is absent or unsupported.
                final String responseCharset = parseCharset(con.getContentType());
                if (responseCharset != null) {
                    encoding = responseCharset;
                }
                response.setEncoding(encoding);
                response.setHttpStatusCode(con.getResponseCode());
                response.setHeaders(con.getHeaderFields());
            } catch (final Exception e) {
                throw new CurlException("Failed to access the response.", e);
            }
            writeContent(() -> {
                try {
                    if (Method.HEAD.toString().equalsIgnoreCase(con.getRequestMethod())) {
                        return new ByteArrayInputStream(new byte[0]);
                    } else if (con.getResponseCode() < 400) {
                        if (isGzipEncoding(con.getContentEncoding())) {
                            return new GZIPInputStream(con.getInputStream());
                        } else {
                            return con.getInputStream();
                        }
                    } else {
                        final InputStream errorStream = con.getErrorStream();
                        if (errorStream == null) {
                            return new ByteArrayInputStream(new byte[0]);
                        }
                        if (isGzipEncoding(con.getContentEncoding())) {
                            return new GZIPInputStream(errorStream);
                        } else {
                            return errorStream;
                        }
                    }
                } catch (IOException e) {
                    throw new CurlException("Failed to process a request.", e);
                }
            });
        }

        /**
         * Parses the {@code charset} parameter from a response {@code Content-Type} header value.
         *
         * <p>A charset value wrapped in a matching pair of double ({@code "}) or single ({@code '})
         * quotes is unquoted before it is validated.</p>
         *
         * @param contentType the {@code Content-Type} header value, may be {@code null}
         * @return the charset name if present and supported, otherwise {@code null}
         */
        private static String parseCharset(final String contentType) {
            if (contentType == null) {
                return null;
            }
            for (final String part : contentType.split(";")) {
                final String token = part.trim();
                if (token.regionMatches(true, 0, "charset=", 0, 8)) {
                    String charset = token.substring(8).trim();
                    if (charset.length() >= 2) {
                        final char first = charset.charAt(0);
                        final char last = charset.charAt(charset.length() - 1);
                        if (first == last && (first == '"' || first == '\'')) {
                            charset = charset.substring(1, charset.length() - 1);
                        }
                    }
                    if (charset.isEmpty()) {
                        return null;
                    }
                    try {
                        return Charset.isSupported(charset) ? charset : null;
                    } catch (final IllegalCharsetNameException e) {
                        return null;
                    }
                }
            }
            return null;
        }

        /**
         * Writes the content of the response to the CurlResponse.
         *
         * @param handler the input stream supplier
         */
        private void writeContent(final Supplier<InputStream> handler) {
            try (BufferedInputStream bis = new BufferedInputStream(handler.get());
                    ContentOutputStream dfos = new ContentOutputStream(threshold, Curl.tmpDir)) {
                final byte[] bytes = new byte[4096];
                int length = bis.read(bytes);
                while (length != -1) {
                    if (length != 0) {
                        final int len = length;
                        logger.fine(() -> {
                            try {
                                return "<<< " + new String(bytes, 0, len, encoding);
                            } catch (final Exception e) {
                                return "<<< <" + e.getMessage() + ">";
                            }
                        });
                        dfos.write(bytes, 0, length);
                    }
                    length = bis.read(bytes);
                }
                dfos.flush();
                final ContentCache contentCache;
                logger.fine(() -> "Response in " + (dfos.isInMemory() ? "Memory" : "File"));
                if (dfos.isInMemory()) {
                    contentCache = new ContentCache(dfos.getData());
                } else {
                    contentCache = new ContentCache(dfos.getFile());
                }
                response.setContentCache(contentCache);
            } catch (final Exception e) {
                response.setContentException(e);
                throw new CurlException("Failed to write a response.", e);
            }
        }
    }
}
