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
package org.codelibs.curl.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

import org.apache.commons.io.output.DeferredFileOutputStream;

/**
 * ContentOutputStream is a custom output stream that extends DeferredFileOutputStream.
 * It writes data to a temporary file once the data size exceeds a specified threshold.
 *
 * <p>This class ensures that the temporary file is deleted if it is not needed.
 * It uses a logger to log warnings if there are issues deleting the temporary file.</p>
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code logger} - Logger instance for logging warnings.</li>
 *   <li>{@code PREFIX} - Prefix for the temporary file name.</li>
 *   <li>{@code SUFFIX} - Suffix for the temporary file name.</li>
 *   <li>{@code done} - Flag indicating whether the file has been retrieved.</li>
 * </ul>
 *
 * <p>Constructors:</p>
 * <ul>
 *   <li>{@code ContentOutputStream(int threshold, File tmpDir)} - Constructs a ContentOutputStream with a specified threshold and temporary directory.</li>
 * </ul>
 *
 * <p>Methods:</p>
 * <ul>
 *   <li>{@code getFile()} - Marks the file as retrieved and returns the file.</li>
 *   <li>{@code close()} - Closes the stream and deletes the temporary file if it is not needed.</li>
 * </ul>
 */
public class ContentOutputStream extends DeferredFileOutputStream {

    protected static final Logger logger = Logger.getLogger(ContentOutputStream.class.getName());

    protected static final String PREFIX = "curl4j-";

    protected static final String SUFFIX = ".tmp";

    protected boolean done = false;

    public ContentOutputStream(final int threshold, final File tmpDir) {
        super(threshold, PREFIX, SUFFIX, tmpDir);
    }

    @Override
    public File getFile() {
        done = true;
        return super.getFile();
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (!isInMemory() && !done) {
                final File file = super.getFile();
                if (file != null) {
                    try {
                        Files.deleteIfExists(file.toPath());
                    } catch (final IOException e) {
                        logger.warning(e.getLocalizedMessage());
                    }
                }
            }
        }
    }
}
