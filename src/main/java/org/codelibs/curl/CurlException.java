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

/**
 * Custom exception class for handling errors related to Curl operations.
 * This class extends {@link RuntimeException} and provides constructors
 * to create an exception instance with a message and an optional cause.
 *
 * <p>Usage examples:</p>
 * <pre>
 *     throw new CurlException("Error message");
 *     throw new CurlException("Error message", cause);
 * </pre>
 *
 * @see RuntimeException
 */
public class CurlException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new CurlException with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public CurlException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new CurlException with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public CurlException(final String message) {
        super(message);
    }

}
