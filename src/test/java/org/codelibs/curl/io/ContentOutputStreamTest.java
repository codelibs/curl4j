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

import java.io.File;
import java.io.IOException;

import org.codelibs.curl.Curl;
import org.junit.Test;

public class ContentOutputStreamTest {

    @Test
    public void inMemory() throws IOException {
        ContentOutputStream cos = new ContentOutputStream(10, Curl.tmpDir);
        cos.write(new byte[] { 0, 1, 2, 3, 4 });
        assertFalse(cos.done);
        assertTrue(cos.isInMemory());
        cos.close();
        assertFalse(cos.done);
    }

    @Test
    public void inFile() throws IOException {
        ContentOutputStream cos = new ContentOutputStream(10, Curl.tmpDir);
        cos.write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 });
        assertFalse(cos.done);
        assertFalse(cos.isInMemory());
        File file = cos.getFile();
        assertTrue(cos.done);
        assertTrue(file.exists());
        cos.close();
        assertTrue(cos.done);
        assertTrue(file.exists());
    }

    @Test
    public void inFileWithoutGet() throws IOException {
        ContentOutputStream cos = new ContentOutputStream(10, Curl.tmpDir);
        cos.write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 });
        assertFalse(cos.done);
        assertFalse(cos.isInMemory());
        cos.close();
        assertFalse(cos.done);
        assertFalse(cos.getFile().exists());
    }

    @Test
    public void testThresholdZero() throws IOException {
        // With threshold 0, everything should go to file immediately
        ContentOutputStream cos = new ContentOutputStream(0, Curl.tmpDir);
        cos.write(new byte[] { 1 });
        assertFalse(cos.done);
        assertFalse(cos.isInMemory());
        File file = cos.getFile();
        assertTrue(file.exists());
        cos.close();
        assertTrue(file.exists());
    }

    @Test
    public void testThresholdOne() throws IOException {
        // With threshold 1, writing 1 byte should stay in memory
        ContentOutputStream cos = new ContentOutputStream(1, Curl.tmpDir);
        cos.write(new byte[] { 1 });
        assertTrue(cos.isInMemory());
        assertFalse(cos.done);
        cos.close();
    }

    @Test
    public void testThresholdOneExceeded() throws IOException {
        // With threshold 1, writing 2 bytes should go to file
        ContentOutputStream cos = new ContentOutputStream(1, Curl.tmpDir);
        cos.write(new byte[] { 1, 2 });
        assertFalse(cos.isInMemory());
        assertFalse(cos.done);
        File file = cos.getFile();
        assertTrue(file.exists());
        cos.close();
    }

    @Test
    public void testExactThreshold() throws IOException {
        // Writing exactly threshold bytes should stay in memory
        ContentOutputStream cos = new ContentOutputStream(5, Curl.tmpDir);
        cos.write(new byte[] { 0, 1, 2, 3, 4 });
        assertTrue(cos.isInMemory());
        assertFalse(cos.done);
        cos.close();
    }

    @Test
    public void testExactThresholdPlusOne() throws IOException {
        // Writing threshold + 1 bytes should go to file
        ContentOutputStream cos = new ContentOutputStream(5, Curl.tmpDir);
        cos.write(new byte[] { 0, 1, 2, 3, 4, 5 });
        assertFalse(cos.isInMemory());
        assertFalse(cos.done);
        File file = cos.getFile();
        assertTrue(file.exists());
        cos.close();
    }

    @Test
    public void testMultipleWrites() throws IOException {
        // Multiple write calls should accumulate
        ContentOutputStream cos = new ContentOutputStream(10, Curl.tmpDir);
        cos.write(new byte[] { 0, 1, 2 });
        assertTrue(cos.isInMemory());
        cos.write(new byte[] { 3, 4 });
        assertTrue(cos.isInMemory());
        cos.write(new byte[] { 5 });
        assertTrue(cos.isInMemory());
        assertFalse(cos.done);
        cos.close();
    }

    @Test
    public void testMultipleWritesExceedingThreshold() throws IOException {
        // Multiple writes that exceed threshold should switch to file
        ContentOutputStream cos = new ContentOutputStream(5, Curl.tmpDir);
        cos.write(new byte[] { 0, 1, 2 });
        assertTrue(cos.isInMemory());
        cos.write(new byte[] { 3, 4, 5 }); // Total 6 bytes, exceeds threshold of 5
        assertFalse(cos.isInMemory());
        File file = cos.getFile();
        assertTrue(file.exists());
        cos.close();
    }

    @Test
    public void testEmptyWrite() throws IOException {
        // Writing empty array should work
        ContentOutputStream cos = new ContentOutputStream(10, Curl.tmpDir);
        cos.write(new byte[] {});
        assertTrue(cos.isInMemory());
        assertFalse(cos.done);
        cos.close();
    }

    @Test
    public void testSingleByteWrites() throws IOException {
        // Writing one byte at a time
        ContentOutputStream cos = new ContentOutputStream(5, Curl.tmpDir);
        for (int i = 0; i < 3; i++) {
            cos.write(i);
        }
        assertTrue(cos.isInMemory());
        assertFalse(cos.done);
        cos.close();
    }

    @Test
    public void testSingleByteWritesExceedingThreshold() throws IOException {
        // Writing one byte at a time, exceeding threshold
        ContentOutputStream cos = new ContentOutputStream(3, Curl.tmpDir);
        cos.write(1);
        cos.write(2);
        cos.write(3);
        assertTrue(cos.isInMemory());
        cos.write(4); // Exceeds threshold
        assertFalse(cos.isInMemory());
        File file = cos.getFile();
        assertTrue(file.exists());
        cos.close();
    }

    @Test
    public void testGetDataInMemory() throws IOException {
        // getData() should return the written data when in memory
        ContentOutputStream cos = new ContentOutputStream(10, Curl.tmpDir);
        byte[] data = new byte[] { 0, 1, 2, 3, 4 };
        cos.write(data);
        assertTrue(cos.isInMemory());
        byte[] result = cos.getData();
        assertNotNull(result);
        assertArrayEquals(data, result);
        cos.close();
    }

    @Test
    public void testFlushInMemory() throws IOException {
        // flush() should work with in-memory data
        ContentOutputStream cos = new ContentOutputStream(10, Curl.tmpDir);
        cos.write(new byte[] { 0, 1, 2 });
        cos.flush();
        assertTrue(cos.isInMemory());
        cos.close();
    }

    @Test
    public void testFlushInFile() throws IOException {
        // flush() should work with file-based data
        ContentOutputStream cos = new ContentOutputStream(5, Curl.tmpDir);
        cos.write(new byte[] { 0, 1, 2, 3, 4, 5 });
        cos.flush();
        assertFalse(cos.isInMemory());
        File file = cos.getFile();
        assertTrue(file.exists());
        cos.close();
    }

    @Test
    public void testLargeData() throws IOException {
        // Test with large data exceeding typical threshold
        ContentOutputStream cos = new ContentOutputStream(100, Curl.tmpDir);
        byte[] largeData = new byte[500];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        cos.write(largeData);
        assertFalse(cos.isInMemory());
        File file = cos.getFile();
        assertTrue(file.exists());
        assertEquals(500, file.length());
        cos.close();
    }

    @Test
    public void testBoundaryAtThresholdMinusOne() throws IOException {
        // Writing threshold - 1 bytes should stay in memory
        ContentOutputStream cos = new ContentOutputStream(10, Curl.tmpDir);
        cos.write(new byte[9]);
        assertTrue(cos.isInMemory());
        cos.close();
    }

    @Test
    public void testWriteWithOffsetAndLength() throws IOException {
        // Test write(byte[], int, int) method
        ContentOutputStream cos = new ContentOutputStream(10, Curl.tmpDir);
        byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        cos.write(data, 2, 3); // Write bytes at index 2, 3, 4
        assertTrue(cos.isInMemory());
        cos.close();
    }

    @Test
    public void testWriteWithOffsetAndLengthExceedingThreshold() throws IOException {
        // Test write(byte[], int, int) exceeding threshold
        ContentOutputStream cos = new ContentOutputStream(5, Curl.tmpDir);
        byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        cos.write(data, 0, 6); // Write 6 bytes, exceeds threshold of 5
        assertFalse(cos.isInMemory());
        File file = cos.getFile();
        assertTrue(file.exists());
        cos.close();
    }

    @Test
    public void testLargeThreshold() throws IOException {
        // Test with very large threshold
        ContentOutputStream cos = new ContentOutputStream(1024 * 1024, Curl.tmpDir);
        cos.write(new byte[1000]);
        assertTrue(cos.isInMemory());
        cos.close();
    }

    @Test
    public void testMultipleGetFile() throws IOException {
        // Calling getFile() multiple times should return the same file
        ContentOutputStream cos = new ContentOutputStream(5, Curl.tmpDir);
        cos.write(new byte[] { 0, 1, 2, 3, 4, 5 });
        File file1 = cos.getFile();
        File file2 = cos.getFile();
        assertEquals(file1.getAbsolutePath(), file2.getAbsolutePath());
        assertTrue(file1.exists());
        cos.close();
    }
}
