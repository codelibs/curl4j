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

import static org.junit.Assert.assertFalse;
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
}
