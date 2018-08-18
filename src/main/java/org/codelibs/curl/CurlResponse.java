/*
 * Copyright 2012-2018 CodeLibs Project and the Others.
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
import java.util.function.Function;

import org.codelibs.curl.io.ContentCache;

public class CurlResponse implements Closeable {

    private int httpStatusCode;

    private ContentCache contentCache;

    private String encoding;

    private Exception contentException;

    @Override
    public void close() throws IOException {
        if (contentCache != null) {
            contentCache.close();
        }
    }

    public <T> T getContent(final Function<CurlResponse, T> parser) {
        return parser.apply(this);
    }

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

    public void setContentCache(final ContentCache contentCache) {
        this.contentCache = contentCache;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(final int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    public void setContentException(final Exception e) {
        contentException = e;
    }

    public Exception getContentException() {
        return contentException;
    }
}
