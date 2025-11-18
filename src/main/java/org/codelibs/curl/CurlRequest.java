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
import java.util.ArrayList;
import java.util.List;
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
    protected Method method;

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
     * Constructs a new CurlRequest with the specified HTTP method and URL.
     *
     * @param method the HTTP method
     * @param url the URL
     * @throws IllegalArgumentException if method or url is null
     */
    public CurlRequest(final Method method, final String url) {
        if (method == null) {
            throw new IllegalArgumentException("method must not be null");
        }
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        this.method = method;
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
        if (bodyStream != null) {
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
        if (body != null) {
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
     * @param actionListener the action listener for handling the response
     * @param exceptionListener the exception listener for handling exceptions
     */
    public void connect(final Consumer<HttpURLConnection> actionListener, final Consumer<Exception> exceptionListener) {
        final Runnable task = () -> {
            String finalUrl = url;
            if (paramList != null) {
                char sp;
                if (finalUrl.indexOf('?') == -1) {
                    sp = '?';
                } else {
                    sp = '&';
                }
                final StringBuilder urlBuf = new StringBuilder(100);
                for (final String param : paramList) {
                    urlBuf.append(sp).append(param);
                    if (sp == '?') {
                        sp = '&';
                    }
                }
                finalUrl = finalUrl + urlBuf.toString();
            }

            HttpURLConnection connection = null;
            try {
                logger.fine(() -> ">>> " + method + " " + finalUrl);
                final URL u = new URL(finalUrl);
                connection = open(u);
                connection.setRequestMethod(method.toString());
                if (headerList != null) {
                    for (final String[] values : headerList) {
                        logger.fine(() -> ">>> " + values[0] + "=" + values[1]);
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
                    try (final OutputStream out = connection.getOutputStream();
                            final InputStream in = bodyStream) {
                        IOUtils.copy(in, out);
                        out.flush();
                    }
                }

                actionListener.accept(connection);
            } catch (final Exception e) {
                exceptionListener.accept(new CurlException("Failed to access to " + finalUrl, e));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        };
        if (threadPool != null) {
            threadPool.execute(task);
        } else {
            task.run();
        }
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
        final ForkJoinPool originalThreadPool = this.threadPool;
        try {
            this.threadPool = null;
            final RequestProcessor processor = new RequestProcessor(encoding, threshold);
            connect(processor, e -> {
                throw new CurlException("Failed to process a request.", e);
            });
            return processor.getResponse();
        } finally {
            this.threadPool = originalThreadPool;
        }
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
     * The RequestProcessor class processes the HTTP request and handles the response.
     */
    public static class RequestProcessor implements Consumer<HttpURLConnection> {
        /**
         * The CurlResponse object to store the response.
         */
        protected CurlResponse response = new CurlResponse();

        private final String encoding;

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
                response.setEncoding(encoding);
                response.setHttpStatusCode(con.getResponseCode());
                response.setHeaders(con.getHeaderFields());
            } catch (final Exception e) {
                throw new CurlException("Failed to access the response.", e);
            }
            writeContent(() -> {
                try {
                    if (con.getResponseCode() < 400) {
                        if (GZIP.equals(con.getContentEncoding())) {
                            return new GZIPInputStream(con.getInputStream());
                        } else {
                            return con.getInputStream();
                        }
                    } else if (Method.HEAD.toString().equalsIgnoreCase(con.getRequestMethod())) {
                        return new ByteArrayInputStream(new byte[0]);
                    } else {
                        if (GZIP.equals(con.getContentEncoding())) {
                            return new GZIPInputStream(con.getErrorStream());
                        } else {
                            return con.getErrorStream();
                        }
                    }
                } catch (IOException e) {
                    throw new CurlException("Failed to process a request.", e);
                }
            });
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
