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
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.codelibs.curl.io.ContentCache;

/**
 * The CurlResponse class represents the response from a cURL request.
 * It implements the Closeable interface to allow proper resource management.
 */
public class CurlResponse implements Closeable {

    /**
     * The HTTP status code of the response.
     */
    private int httpStatusCode;

    /**
     * The content cache that stores the response content.
     */
    private ContentCache contentCache;

    /**
     * The encoding used for the response content.
     */
    private String encoding;

    /**
     * The exception that occurred while accessing the content, if any.
     */
    private Exception contentException;

    /**
     * The headers of the response.
     */
    private Map<String, List<String>> headers;

    /**
     * Closes the content cache if it is not null.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (contentCache != null) {
            contentCache.close();
        }
    }

    /**
     * Gets the content of the response using the provided parser function.
     *
     * @param <T> the type of the parsed content.
     * @param parser the function to parse the content.
     * @return the parsed content.
     */
    public <T> T getContent(final Function<CurlResponse, T> parser) {
        return parser.apply(this);
    }

    /**
     * Gets the content of the response as a string.
     *
     * @return the content as a string.
     * @throws CurlException if an error occurs while accessing the content.
     */
    public String getContentAsString() {
        final byte[] bytes = new byte[4096];
        try (BufferedInputStream bis = new BufferedInputStream(getContentAsStream());
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            int length = bis.read(bytes);
            while (length != -1) {
                if (length != 0) {
                    baos.write(bytes, 0, length);
                }
                length = bis.read(bytes);
            }
            return baos.toString(encoding);
        } catch (final Exception e) {
            throw new CurlException("Failed to access the content.", e);
        }
    }

    /**
     * Gets the content of the response as an InputStream.
     *
     * @return the content as an InputStream.
     * @throws IOException if an I/O error occurs.
     * @throws CurlException if the content does not exist.
     */
    public InputStream getContentAsStream() throws IOException {
        if (contentCache == null) {
            if (contentException != null) {
                throw new CurlException("The content does not exist.", contentException);
            } else {
                throw new CurlException("The content does not exist.");
            }
        }
        return contentCache.getInputStream();
    }

    /**
     * Sets the content cache for the response.
     *
     * @param contentCache the content cache to set.
     */
    public void setContentCache(final ContentCache contentCache) {
        this.contentCache = contentCache;
    }

    /**
     * Gets the HTTP status code of the response.
     *
     * @return the HTTP status code.
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Sets the HTTP status code for the response.
     *
     * @param httpStatusCode the HTTP status code to set.
     */
    public void setHttpStatusCode(final int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Gets the encoding used for the response content.
     *
     * @return the encoding.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Sets the encoding for the response content.
     *
     * @param encoding the encoding to set.
     */
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    /**
     * Sets the exception that occurred while accessing the content.
     *
     * @param e the exception to set.
     */
    public void setContentException(final Exception e) {
        contentException = e;
    }

    /**
     * Gets the exception that occurred while accessing the content, if any.
     *
     * @return the exception, or null if no exception occurred.
     */
    public Exception getContentException() {
        return contentException;
    }

    /**
     * Sets the headers for the response.
     *
     * @param headers the headers to set.
     */
    public void setHeaders(final Map<String, List<String>> headers) {
        if (headers != null) {
            final Map<String, List<String>> map = new HashMap<>();
            headers.entrySet().stream().filter(e -> e.getKey() != null)
                    .forEach(e -> map.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue()));
            this.headers = map;
        }
    }

    /**
     * Gets the headers of the response.
     *
     * @return the headers.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Gets the values of the specified header.
     *
     * @param name the name of the header.
     * @return an array of header values, or an empty array if the header does not exist.
     */
    public String[] getHeaderValues(final String name) {
        final List<String> list = headers.get(name.toLowerCase(Locale.ROOT));
        if (list == null) {
            return new String[0];
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Gets the value of the specified header.
     *
     * @param name the name of the header.
     * @return the header value, or null if the header does not exist.
     */
    public String getHeaderValue(final String name) {
        final String[] values = getHeaderValues(name);
        if (values.length == 0) {
            return null;
        }
        return values[0];
    }
}
