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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

/**
 * Test class for CurlException.
 * Tests exception creation, message handling, and cause propagation.
 */
public class CurlExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String message = "Test error message";
        CurlException exception = new CurlException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Test error message";
        IOException cause = new IOException("IO error");
        CurlException exception = new CurlException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testConstructorWithNullMessage() {
        CurlException exception = new CurlException(null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithNullMessageAndCause() {
        IOException cause = new IOException("IO error");
        CurlException exception = new CurlException(null, cause);

        assertNull(exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndNullCause() {
        String message = "Test error message";
        CurlException exception = new CurlException(message, null);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testSerialVersionUID() {
        assertNotNull(CurlException.class.getDeclaredFields());
        boolean hasSerialVersionUID = false;
        try {
            CurlException.class.getDeclaredField("serialVersionUID");
            hasSerialVersionUID = true;
        } catch (NoSuchFieldException e) {
            // Field doesn't exist
        }
        assertTrue("CurlException should have serialVersionUID field", hasSerialVersionUID);
    }

    @Test
    public void testExceptionChaining() {
        IOException rootCause = new IOException("Root cause");
        RuntimeException intermediateCause = new RuntimeException("Intermediate", rootCause);
        CurlException exception = new CurlException("Final message", intermediateCause);

        assertEquals("Final message", exception.getMessage());
        assertSame(intermediateCause, exception.getCause());
        assertSame(rootCause, exception.getCause().getCause());
    }

    @Test
    public void testToString() {
        String message = "Test error message";
        CurlException exception = new CurlException(message);
        String toString = exception.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("CurlException"));
        assertTrue(toString.contains(message));
    }

    @Test
    public void testStackTrace() {
        CurlException exception = new CurlException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();

        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
        assertEquals("testStackTrace", stackTrace[0].getMethodName());
    }
}