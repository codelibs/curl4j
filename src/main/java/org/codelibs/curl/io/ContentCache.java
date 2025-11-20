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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * ContentCache is a class that provides a way to cache content either in memory or in a file.
 * It implements the Closeable interface to ensure that resources are properly released.
 *
 * <p>This class supports two types of content caching:
 * <ul>
 *   <li>In-memory caching using a byte array</li>
 *   <li>File-based caching using a File object</li>
 * </ul>
 *
 * <p>When an instance of ContentCache is created with a byte array, the content is cached in memory.
 * When an instance is created with a File object, the content is cached in the specified file.
 *
 * <p>The {@code close()} method deletes the file if the content is cached in a file.
 *
 * <p>The {@code getInputStream()} method provides an InputStream to read the cached content.
 * If the content is cached in a file, it returns a FileInputStream. If the content is cached in memory,
 * it returns a ByteArrayInputStream.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * // In-memory caching
 * byte[] data = "example data".getBytes();
 * try (ContentCache cache = new ContentCache(data)) {
 *     InputStream inputStream = cache.getInputStream();
 *     // Read from inputStream
 * }
 *
 * // File-based caching
 * File file = new File("example.txt");
 * try (ContentCache cache = new ContentCache(file)) {
 *     InputStream inputStream = cache.getInputStream();
 *     // Read from inputStream
 * }
 * }
 * </pre>
 *
 */
public class ContentCache implements Closeable {

    /**
     * The logger for this class.
     */
    protected static final Logger logger = Logger.getLogger(ContentCache.class.getName());

    /**
     * A byte array that holds the cached content data.
     */
    private final byte[] data;

    /**
     * The file that is used to cache the content.
     */
    private final File file;

    /**
     * Constructs a ContentCache with the given byte array data.
     *
     * @param data the byte array containing the content
     * @throws IllegalArgumentException if data is null
     */
    public ContentCache(final byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        this.data = data.clone();
        this.file = null;
    }

    /**
     * Constructs a ContentCache with the given file.
     *
     * @param file the file containing the content
     * @throws IllegalArgumentException if file is null
     */
    public ContentCache(final File file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        this.data = null;
        this.file = file;
    }

    @Override
    public void close() throws IOException {
        if (file != null) {
            Files.delete(file.toPath());
        }
    }

    /**
     * Returns an InputStream to read the content from the cache.
     * If the content is stored in a file, a FileInputStream is returned.
     * Otherwise, a ByteArrayInputStream is returned to read the data from memory.
     *
     * @return an InputStream to read the cached content
     * @throws IOException if an I/O error occurs while creating the InputStream
     */
    public InputStream getInputStream() throws IOException {
        if (file != null) {
            return new FileInputStream(file);
        }
        return new ByteArrayInputStream(data);
    }
}
