/*
 * Copyright 2012-2022 CodeLibs Project and the Others.
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

public class CurlRequest {

    protected static final String GZIP = "gzip";

    protected static final Logger logger = Logger.getLogger(CurlRequest.class.getName());

    protected String url;

    protected Proxy proxy;

    protected String encoding = "UTF-8";

    protected int threshold = 1024 * 1024; // 1m

    protected Method method;

    protected List<String> paramList;

    protected List<String[]> headerList;

    protected String body;

    protected InputStream bodyStream;

    protected String compression = null;

    protected SSLSocketFactory sslSocketFactory = null;

    protected ForkJoinPool threadPool;

    private BiConsumer<CurlRequest, HttpURLConnection> connectionBuilder;

    public CurlRequest(final Method method, final String url) {
        this.method = method;
        this.url = url;
    }

    public Proxy proxy() {
        return proxy;
    }

    public String encoding() {
        return encoding;
    }

    public int threshold() {
        return threshold;
    }

    public Method method() {
        return method;
    }

    public String body() {
        return body;
    }

    public CurlRequest proxy(final Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public CurlRequest encoding(final String encoding) {
        if (paramList != null) {
            throw new CurlException("This method must be called before param method.");
        }
        this.encoding = encoding;
        return this;
    }

    public CurlRequest threshold(final int threshold) {
        this.threshold = threshold;
        return this;
    }

    public CurlRequest gzip() {
        return compression(GZIP);
    }

    public CurlRequest compression(final String compression) {
        this.compression = compression;
        return this;
    }

    public CurlRequest sslSocketFactory(final SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        return this;
    }

    public CurlRequest body(final String body) {
        if (bodyStream != null) {
            throw new CurlException("body method is already called.");
        }
        this.body = body;
        return this;
    }

    public CurlRequest body(final InputStream stream) {
        if (body != null) {
            throw new CurlException("body method is already called.");
        }
        this.bodyStream = stream;
        return this;
    }

    public CurlRequest onConnect(final BiConsumer<CurlRequest, HttpURLConnection> connectionBuilder) {
        this.connectionBuilder = connectionBuilder;
        return this;
    }

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

    public CurlRequest header(final String key, final String value) {
        if (headerList == null) {
            headerList = new ArrayList<>();
        }
        headerList.add(new String[] { key, value });
        return this;
    }

    public void connect(final Consumer<HttpURLConnection> actionListener, final Consumer<Exception> exceptionListener) {
        final Runnable task = () -> {
            if (paramList != null) {
                char sp;
                if (url.indexOf('?') == -1) {
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
                url = url + urlBuf.toString();
            }

            HttpURLConnection connection = null;
            try {
                logger.fine(() -> ">>> " + method + " " + url);
                final URL u = new URL(url);
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
                    try (final OutputStream out = connection.getOutputStream()) {
                        IOUtils.copy(bodyStream, out);
                        out.flush();
                    }
                }

                actionListener.accept(connection);
            } catch (final Exception e) {
                exceptionListener.accept(new CurlException("Failed to access to " + url, e));
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

    protected HttpURLConnection open(final URL u) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) (proxy != null ? u.openConnection(proxy) : u.openConnection());
        if (sslSocketFactory != null && connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        }
        return connection;
    }

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

    public CurlResponse execute() {
        this.threadPool = null;
        final RequestProcessor processor = new RequestProcessor(encoding, threshold);
        connect(processor, e -> {
            throw new CurlException("Failed to process a request.", e);
        });
        return processor.getResponse();
    }

    protected String encode(final String value) {
        try {
            return URLEncoder.encode(value, encoding);
        } catch (final UnsupportedEncodingException e) {
            throw new CurlException("Invalid encoding: " + encoding, e);
        }
    }

    public CurlRequest threadPool(final ForkJoinPool threadPool) {
        this.threadPool = threadPool;
        return this;
    }

    public static class RequestProcessor implements Consumer<HttpURLConnection> {
        protected CurlResponse response = new CurlResponse();

        private final String encoding;

        private int threshold;

        public RequestProcessor(final String encoding, final int threshold) {
            this.encoding = encoding;
            this.threshold = threshold;
        }

        public CurlResponse getResponse() {
            return response;
        }

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
                    } else if ("head".equalsIgnoreCase(con.getRequestMethod())) {
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

        private void writeContent(final Supplier<InputStream> handler) {
            try (BufferedInputStream bis = new BufferedInputStream(handler.get());
                    ContentOutputStream dfos = new ContentOutputStream(threshold, Curl.tmpDir)) {
                final byte[] bytes = new byte[4096];
                int length = bis.read(bytes);
                while (length != -1) {
                    if (length != 0) {
                        dfos.write(bytes, 0, length);
                    }
                    length = bis.read(bytes);
                    logger.fine(() -> {
                        try {
                            return "<<< " + new String(bytes, encoding);
                        } catch (UnsupportedEncodingException e) {
                            return "<<< <" + e.getMessage() + ">";
                        }
                    });
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
