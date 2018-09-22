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
package org.codelibs.curl.io;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

public class ContentCache implements Closeable {

    protected static final Logger logger = Logger.getLogger(ContentCache.class.getName());

    private final byte[] data;

    private final File file;

    public ContentCache(final byte[] data) {
        this.data = data;
        this.file = null;
    }

    public ContentCache(final File file) {
        this.data = null;
        this.file = file;
    }

    @Override
    public void close() throws IOException {
        if (file != null) {
            Files.delete(file.toPath());
        }
    }

    public InputStream getInputStream() throws IOException {
        if (file != null) {
            return new FileInputStream(file);
        }
        return new ByteArrayInputStream(data);
    }
}
