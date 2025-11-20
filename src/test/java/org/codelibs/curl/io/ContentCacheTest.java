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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.codelibs.curl.Curl;
import org.junit.After;
import org.junit.Test;

/**
 * Test class for ContentCache.
 * Tests memory-based and file-based content caching.
 */
public class ContentCacheTest {

    private File tempFile;

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void testMemoryBasedCacheConstructor() {
        byte[] data = "Hello, World!".getBytes();
        ContentCache cache = new ContentCache(data);

        assertNotNull(cache);
    }

    @Test
    public void testMemoryBasedCacheConstructorWithNull() {
        try {
            new ContentCache((byte[]) null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("data must not be null"));
        }
    }

    @Test
    public void testMemoryBasedCacheDefensiveCopy() throws IOException {
        byte[] data = "Hello, World!".getBytes();
        byte[] originalData = data.clone();
        ContentCache cache = new ContentCache(data);

        // Modify the original array
        data[0] = 'X';

        // Verify that the cache's data is not affected
        try (InputStream stream = cache.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead = stream.read(buffer);
            byte[] result = new byte[bytesRead];
            System.arraycopy(buffer, 0, result, 0, bytesRead);
            assertArrayEquals(originalData, result);
        }
    }

    @Test
    public void testFileBasedCacheConstructor() throws IOException {
        tempFile = File.createTempFile("test", ".tmp", Curl.tmpDir);
        ContentCache cache = new ContentCache(tempFile);

        assertNotNull(cache);
    }

    @Test
    public void testFileBasedCacheConstructorWithNull() {
        try {
            new ContentCache((File) null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("file must not be null"));
        }
    }

    @Test
    public void testMemoryBasedCacheGetInputStream() throws IOException {
        String testContent = "Hello, World!";
        byte[] data = testContent.getBytes("UTF-8");
        ContentCache cache = new ContentCache(data);

        try (InputStream stream = cache.getInputStream()) {
            assertNotNull(stream);
            byte[] buffer = new byte[1024];
            int bytesRead = stream.read(buffer);
            assertEquals(data.length, bytesRead);

            String result = new String(buffer, 0, bytesRead, "UTF-8");
            assertEquals(testContent, result);
        }
    }

    @Test
    public void testFileBasedCacheGetInputStream() throws IOException {
        String testContent = "Hello, World!";
        tempFile = File.createTempFile("test", ".tmp", Curl.tmpDir);

        // Write test content to file
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(testContent.getBytes("UTF-8"));
        }

        ContentCache cache = new ContentCache(tempFile);

        try (InputStream stream = cache.getInputStream()) {
            assertNotNull(stream);
            byte[] buffer = new byte[1024];
            int bytesRead = stream.read(buffer);
            assertEquals(testContent.length(), bytesRead);

            String result = new String(buffer, 0, bytesRead, "UTF-8");
            assertEquals(testContent, result);
        }
    }

    @Test
    public void testMemoryBasedCacheMultipleReads() throws IOException {
        String testContent = "Hello, World!";
        byte[] data = testContent.getBytes("UTF-8");
        ContentCache cache = new ContentCache(data);

        // First read
        try (InputStream stream1 = cache.getInputStream()) {
            byte[] buffer1 = new byte[1024];
            int bytesRead1 = stream1.read(buffer1);
            assertEquals(testContent, new String(buffer1, 0, bytesRead1, "UTF-8"));
        }

        // Second read - should work independently
        try (InputStream stream2 = cache.getInputStream()) {
            byte[] buffer2 = new byte[1024];
            int bytesRead2 = stream2.read(buffer2);
            assertEquals(testContent, new String(buffer2, 0, bytesRead2, "UTF-8"));
        }
    }

    @Test
    public void testFileBasedCacheMultipleReads() throws IOException {
        String testContent = "Hello, World!";
        tempFile = File.createTempFile("test", ".tmp", Curl.tmpDir);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(testContent.getBytes("UTF-8"));
        }

        ContentCache cache = new ContentCache(tempFile);

        // First read
        try (InputStream stream1 = cache.getInputStream()) {
            byte[] buffer1 = new byte[1024];
            int bytesRead1 = stream1.read(buffer1);
            assertEquals(testContent, new String(buffer1, 0, bytesRead1, "UTF-8"));
        }

        // Second read - should work independently
        try (InputStream stream2 = cache.getInputStream()) {
            byte[] buffer2 = new byte[1024];
            int bytesRead2 = stream2.read(buffer2);
            assertEquals(testContent, new String(buffer2, 0, bytesRead2, "UTF-8"));
        }
    }

    @Test
    public void testMemoryBasedCacheWithEmptyData() throws IOException {
        byte[] data = new byte[0];
        ContentCache cache = new ContentCache(data);

        try (InputStream stream = cache.getInputStream()) {
            assertNotNull(stream);
            assertEquals(-1, stream.read()); // Should indicate end of stream
        }
    }

    @Test
    public void testFileBasedCacheWithEmptyFile() throws IOException {
        tempFile = File.createTempFile("empty", ".tmp", Curl.tmpDir);
        // File is created but empty

        ContentCache cache = new ContentCache(tempFile);

        try (InputStream stream = cache.getInputStream()) {
            assertNotNull(stream);
            assertEquals(-1, stream.read()); // Should indicate end of stream
        }
    }

    @Test
    public void testMemoryBasedCacheWithLargeData() throws IOException {
        // Create a large byte array (1MB)
        byte[] data = new byte[1024 * 1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }

        ContentCache cache = new ContentCache(data);

        try (InputStream stream = cache.getInputStream()) {
            assertNotNull(stream);
            byte[] buffer = new byte[data.length];
            int totalBytesRead = 0;
            int bytesRead;

            while ((bytesRead = stream.read(buffer, totalBytesRead, buffer.length - totalBytesRead)) != -1) {
                totalBytesRead += bytesRead;
            }

            assertEquals(data.length, totalBytesRead);
            assertArrayEquals(data, buffer);
        }
    }

    @Test
    public void testMemoryBasedCacheClose() throws IOException {
        byte[] data = "Hello, World!".getBytes();
        ContentCache cache = new ContentCache(data);

        // Memory-based cache close should not throw exception
        cache.close();

        // Should still be able to get input stream after close for memory cache
        try (InputStream stream = cache.getInputStream()) {
            assertNotNull(stream);
        }
    }

    @Test
    public void testFileBasedCacheClose() throws IOException {
        tempFile = File.createTempFile("test", ".tmp", Curl.tmpDir);
        Files.write(tempFile.toPath(), "Hello, World!".getBytes());
        assertTrue(tempFile.exists());

        ContentCache cache = new ContentCache(tempFile);

        // Close should delete the file
        cache.close();

        assertFalse(tempFile.exists());
    }

    @Test
    public void testFileBasedCacheGetInputStreamAfterClose() throws IOException {
        tempFile = File.createTempFile("test", ".tmp", Curl.tmpDir);
        Files.write(tempFile.toPath(), "Hello, World!".getBytes());

        ContentCache cache = new ContentCache(tempFile);
        cache.close(); // This deletes the file

        try {
            cache.getInputStream();
            fail("Expected IOException for deleted file");
        } catch (IOException e) {
            // Expected - file no longer exists
        }
    }

    @Test
    public void testFileBasedCacheCloseNonExistentFile() {
        tempFile = new File(Curl.tmpDir, "non-existent-file.tmp");
        assertFalse(tempFile.exists());

        ContentCache cache = new ContentCache(tempFile);

        // Should throw IOException when file doesn't exist
        try {
            cache.close();
            fail("Expected IOException for non-existent file");
        } catch (IOException e) {
            // Expected - file doesn't exist
        }
    }

    @Test
    public void testMemoryCacheWithBinaryData() throws IOException {
        // Test with binary data containing all byte values
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }

        ContentCache cache = new ContentCache(data);

        try (InputStream stream = cache.getInputStream()) {
            for (int i = 0; i < 256; i++) {
                int byteValue = stream.read();
                assertEquals("Byte at position " + i, i & 0xFF, byteValue);
            }
            assertEquals(-1, stream.read()); // End of stream
        }
    }

    @Test
    public void testStreamReadPartial() throws IOException {
        String testContent = "Hello, World!";
        byte[] data = testContent.getBytes("UTF-8");
        ContentCache cache = new ContentCache(data);

        try (InputStream stream = cache.getInputStream()) {
            // Read first 5 bytes
            byte[] buffer = new byte[5];
            int bytesRead = stream.read(buffer);
            assertEquals(5, bytesRead);
            assertEquals("Hello", new String(buffer, 0, bytesRead, "UTF-8"));

            // Read remaining bytes
            byte[] remaining = new byte[20];
            int remainingBytes = stream.read(remaining);
            assertEquals(8, remainingBytes); // ", World!"
            assertEquals(", World!", new String(remaining, 0, remainingBytes, "UTF-8"));
        }
    }
}