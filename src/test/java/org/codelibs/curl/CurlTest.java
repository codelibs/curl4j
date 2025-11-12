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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codelibs.curl.Curl.Method;
import org.junit.Test;

public class CurlTest {
    private static final Logger logger = Logger.getLogger(CurlTest.class.getName());

    @Test
    public void test_Get() {
        Curl.get("https://www.codelibs.org/").execute(response -> {
            final String content = response.getContentAsString();
            logger.info(content);
            assertTrue(content.length() > 0);
        }, e -> {
            logger.log(Level.SEVERE, "error", e);
            fail();
        });
    }

    @Test
    public void test_GetFactoryMethod() {
        // ## Arrange ##
        final String url = "http://example.com";

        // ## Act ##
        final CurlRequest request = Curl.get(url);

        // ## Assert ##
        assertNotNull(request);
        assertEquals(Method.GET, request.method());
    }

    @Test
    public void test_PostFactoryMethod() {
        // ## Arrange ##
        final String url = "http://example.com";

        // ## Act ##
        final CurlRequest request = Curl.post(url);

        // ## Assert ##
        assertNotNull(request);
        assertEquals(Method.POST, request.method());
    }

    @Test
    public void test_PutFactoryMethod() {
        // ## Arrange ##
        final String url = "http://example.com";

        // ## Act ##
        final CurlRequest request = Curl.put(url);

        // ## Assert ##
        assertNotNull(request);
        assertEquals(Method.PUT, request.method());
    }

    @Test
    public void test_DeleteFactoryMethod() {
        // ## Arrange ##
        final String url = "http://example.com";

        // ## Act ##
        final CurlRequest request = Curl.delete(url);

        // ## Assert ##
        assertNotNull(request);
        assertEquals(Method.DELETE, request.method());
    }

    @Test
    public void test_HeadFactoryMethod() {
        // ## Arrange ##
        final String url = "http://example.com";

        // ## Act ##
        final CurlRequest request = Curl.head(url);

        // ## Assert ##
        assertNotNull(request);
        assertEquals(Method.HEAD, request.method());
    }

    @Test
    public void test_OptionsFactoryMethod() {
        // ## Arrange ##
        final String url = "http://example.com";

        // ## Act ##
        final CurlRequest request = Curl.options(url);

        // ## Assert ##
        assertNotNull(request);
        assertEquals(Method.OPTIONS, request.method());
    }

    @Test
    public void test_ConnectFactoryMethod() {
        // ## Arrange ##
        final String url = "http://example.com";

        // ## Act ##
        final CurlRequest request = Curl.connect(url);

        // ## Assert ##
        assertNotNull(request);
        assertEquals(Method.CONNECT, request.method());
    }

    @Test
    public void test_TmpDirInitialization() {
        // ## Act ##
        final File tmpDir = Curl.tmpDir;

        // ## Assert ##
        assertNotNull(tmpDir);
        assertTrue(tmpDir.exists());
        assertTrue(tmpDir.isDirectory());
        assertTrue(tmpDir.canRead());
        assertTrue(tmpDir.canWrite());
        assertEquals(new File(System.getProperty("java.io.tmpdir")).getAbsolutePath(), tmpDir.getAbsolutePath());
    }

    @Test
    public void test_AllHttpMethodFactories() {
        // ## Test that all factory methods create requests with correct methods ##

        // ## Act & Assert ##
        assertEquals(Method.GET, Curl.get("http://test.com").method());
        assertEquals(Method.POST, Curl.post("http://test.com").method());
        assertEquals(Method.PUT, Curl.put("http://test.com").method());
        assertEquals(Method.DELETE, Curl.delete("http://test.com").method());
        assertEquals(Method.HEAD, Curl.head("http://test.com").method());
        assertEquals(Method.OPTIONS, Curl.options("http://test.com").method());
        assertEquals(Method.CONNECT, Curl.connect("http://test.com").method());
    }

    @Test
    public void test_FactoryMethodsWithDifferentUrls() {
        // ## Test factory methods with various URL formats ##

        // ## Act & Assert ##
        assertNotNull(Curl.get("https://secure.example.com/path?param=value"));
        assertNotNull(Curl.post("http://api.example.com/v1/resource"));
        assertNotNull(Curl.put("ftp://files.example.com/upload"));
        assertNotNull(Curl.delete("http://localhost:8080/delete"));
        assertNotNull(Curl.head("https://cdn.example.com/assets/file.js"));
        assertNotNull(Curl.options("http://cors.example.com/api"));
        assertNotNull(Curl.connect("http://proxy.example.com:3128"));
    }

    @Test
    public void test_HttpMethodEnumValues() {
        // ## Test that all expected HTTP methods are present in the enum ##

        // ## Act ##
        final Method[] methods = Method.values();

        // ## Assert ##
        assertEquals(8, methods.length); // GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, CONNECT

        // Verify each method exists
        boolean hasGet = false, hasPost = false, hasPut = false, hasDelete = false;
        boolean hasHead = false, hasOptions = false, hasTrace = false, hasConnect = false;

        for (final Method method : methods) {
            switch (method) {
            case GET:
                hasGet = true;
                break;
            case POST:
                hasPost = true;
                break;
            case PUT:
                hasPut = true;
                break;
            case DELETE:
                hasDelete = true;
                break;
            case HEAD:
                hasHead = true;
                break;
            case OPTIONS:
                hasOptions = true;
                break;
            case TRACE:
                hasTrace = true;
                break;
            case CONNECT:
                hasConnect = true;
                break;
            }
        }

        assertTrue("GET method should exist", hasGet);
        assertTrue("POST method should exist", hasPost);
        assertTrue("PUT method should exist", hasPut);
        assertTrue("DELETE method should exist", hasDelete);
        assertTrue("HEAD method should exist", hasHead);
        assertTrue("OPTIONS method should exist", hasOptions);
        assertTrue("TRACE method should exist", hasTrace);
        assertTrue("CONNECT method should exist", hasConnect);
    }

    @Test
    public void test_FactoryMethodsReturnNewInstances() {
        // ## Test that each factory method call returns a new instance ##

        // ## Act ##
        final CurlRequest request1 = Curl.get("http://example.com");
        final CurlRequest request2 = Curl.get("http://example.com");

        // ## Assert ##
        assertNotNull(request1);
        assertNotNull(request2);
        assertTrue("Factory methods should return different instances", request1 != request2);
    }

    @Test
    public void test_ProtectedConstructor() throws Exception {
        // ## Test that protected constructor can be accessed via reflection ##

        // ## Act ##
        final Constructor<Curl> constructor = Curl.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Curl instance = constructor.newInstance();

        // ## Assert ##
        assertNotNull(instance);
    }

    @Test
    public void test_TraceMethodInEnum() {
        // ## Test that TRACE method exists in the Method enum ##

        // ## Act ##
        final Method traceMethod = Method.TRACE;

        // ## Assert ##
        assertNotNull(traceMethod);
        assertEquals("TRACE", traceMethod.toString());
    }

    @Test
    public void test_TraceMethodWithCurlRequest() {
        // ## Test creating a CurlRequest with TRACE method directly ##
        // ## Note: There's no Curl.trace() factory method, but we can use the constructor ##

        // ## Act ##
        final CurlRequest request = new CurlRequest(Method.TRACE, "http://example.com");

        // ## Assert ##
        assertNotNull(request);
        assertEquals(Method.TRACE, request.method());
    }

    @Test
    public void test_MethodEnumValueOf() {
        // ## Test Method.valueOf() for all methods ##

        // ## Act & Assert ##
        assertEquals(Method.GET, Method.valueOf("GET"));
        assertEquals(Method.POST, Method.valueOf("POST"));
        assertEquals(Method.PUT, Method.valueOf("PUT"));
        assertEquals(Method.DELETE, Method.valueOf("DELETE"));
        assertEquals(Method.HEAD, Method.valueOf("HEAD"));
        assertEquals(Method.OPTIONS, Method.valueOf("OPTIONS"));
        assertEquals(Method.TRACE, Method.valueOf("TRACE"));
        assertEquals(Method.CONNECT, Method.valueOf("CONNECT"));
    }

    @Test
    public void test_MethodEnumValues() {
        // ## Test that Method.values() returns all 8 methods ##

        // ## Act ##
        final Method[] values = Method.values();

        // ## Assert ##
        assertEquals(8, values.length);
    }

    @Test
    public void test_TmpDirIsReadable() {
        // ## Test that tmpDir is readable ##

        // ## Act & Assert ##
        assertTrue(Curl.tmpDir.canRead());
    }

    @Test
    public void test_TmpDirIsWritable() {
        // ## Test that tmpDir is writable ##

        // ## Act & Assert ##
        assertTrue(Curl.tmpDir.canWrite());
    }

    @Test
    public void test_TmpDirIsDirectory() {
        // ## Test that tmpDir is a directory ##

        // ## Act & Assert ##
        assertTrue(Curl.tmpDir.isDirectory());
    }

    @Test
    public void test_TmpDirExists() {
        // ## Test that tmpDir exists ##

        // ## Act & Assert ##
        assertTrue(Curl.tmpDir.exists());
    }

    @Test
    public void test_TmpDirMatchesSystemProperty() {
        // ## Test that tmpDir matches system property ##

        // ## Act ##
        final String systemTmpDir = System.getProperty("java.io.tmpdir");
        final String curlTmpDir = Curl.tmpDir.getAbsolutePath();

        // ## Assert ##
        assertEquals(new File(systemTmpDir).getAbsolutePath(), curlTmpDir);
    }

    @Test
    public void test_FactoryMethodsWithNullUrl() {
        // ## Test factory methods with null URL (should not throw exception during creation) ##

        // ## Act ##
        final CurlRequest getRequest = Curl.get(null);
        final CurlRequest postRequest = Curl.post(null);
        final CurlRequest putRequest = Curl.put(null);
        final CurlRequest deleteRequest = Curl.delete(null);
        final CurlRequest headRequest = Curl.head(null);
        final CurlRequest optionsRequest = Curl.options(null);
        final CurlRequest connectRequest = Curl.connect(null);

        // ## Assert ##
        assertNotNull(getRequest);
        assertNotNull(postRequest);
        assertNotNull(putRequest);
        assertNotNull(deleteRequest);
        assertNotNull(headRequest);
        assertNotNull(optionsRequest);
        assertNotNull(connectRequest);
    }

    @Test
    public void test_FactoryMethodsWithEmptyUrl() {
        // ## Test factory methods with empty URL ##

        // ## Act ##
        final String emptyUrl = "";
        final CurlRequest getRequest = Curl.get(emptyUrl);
        final CurlRequest postRequest = Curl.post(emptyUrl);
        final CurlRequest putRequest = Curl.put(emptyUrl);
        final CurlRequest deleteRequest = Curl.delete(emptyUrl);
        final CurlRequest headRequest = Curl.head(emptyUrl);
        final CurlRequest optionsRequest = Curl.options(emptyUrl);
        final CurlRequest connectRequest = Curl.connect(emptyUrl);

        // ## Assert ##
        assertNotNull(getRequest);
        assertNotNull(postRequest);
        assertNotNull(putRequest);
        assertNotNull(deleteRequest);
        assertNotNull(headRequest);
        assertNotNull(optionsRequest);
        assertNotNull(connectRequest);
    }

    @Test
    public void test_FactoryMethodsWithSpecialCharactersInUrl() {
        // ## Test factory methods with special characters in URL ##

        // ## Act ##
        final String specialUrl = "http://example.com/path?query=value&param=123#fragment";
        final CurlRequest getRequest = Curl.get(specialUrl);
        final CurlRequest postRequest = Curl.post(specialUrl);
        final CurlRequest putRequest = Curl.put(specialUrl);
        final CurlRequest deleteRequest = Curl.delete(specialUrl);
        final CurlRequest headRequest = Curl.head(specialUrl);
        final CurlRequest optionsRequest = Curl.options(specialUrl);
        final CurlRequest connectRequest = Curl.connect(specialUrl);

        // ## Assert ##
        assertNotNull(getRequest);
        assertNotNull(postRequest);
        assertNotNull(putRequest);
        assertNotNull(deleteRequest);
        assertNotNull(headRequest);
        assertNotNull(optionsRequest);
        assertNotNull(connectRequest);
    }

    @Test
    public void test_MethodEnumName() {
        // ## Test that Method enum name() returns correct values ##

        // ## Act & Assert ##
        assertEquals("GET", Method.GET.name());
        assertEquals("POST", Method.POST.name());
        assertEquals("PUT", Method.PUT.name());
        assertEquals("DELETE", Method.DELETE.name());
        assertEquals("HEAD", Method.HEAD.name());
        assertEquals("OPTIONS", Method.OPTIONS.name());
        assertEquals("TRACE", Method.TRACE.name());
        assertEquals("CONNECT", Method.CONNECT.name());
    }

    @Test
    public void test_MethodEnumOrdinal() {
        // ## Test that Method enum ordinal values are sequential ##

        // ## Act & Assert ##
        assertEquals(0, Method.GET.ordinal());
        assertEquals(1, Method.POST.ordinal());
        assertEquals(2, Method.PUT.ordinal());
        assertEquals(3, Method.DELETE.ordinal());
        assertEquals(4, Method.HEAD.ordinal());
        assertEquals(5, Method.OPTIONS.ordinal());
        assertEquals(6, Method.TRACE.ordinal());
        assertEquals(7, Method.CONNECT.ordinal());
    }

    @Test
    public void test_CreateAllMethodsViaConstructor() {
        // ## Test creating CurlRequest with all methods via constructor ##

        // ## Act & Assert ##
        for (final Method method : Method.values()) {
            final CurlRequest request = new CurlRequest(method, "http://example.com");
            assertNotNull(request);
            assertEquals(method, request.method());
        }
    }

    @Test
    public void test_FactoryMethodsConsistency() {
        // ## Test that factory methods are consistent with constructor ##

        // ## Act ##
        final String url = "http://example.com";
        final CurlRequest getViaFactory = Curl.get(url);
        final CurlRequest getViaConstructor = new CurlRequest(Method.GET, url);

        // ## Assert ##
        assertEquals(getViaFactory.method(), getViaConstructor.method());
        assertEquals(getViaFactory.encoding(), getViaConstructor.encoding());
        assertEquals(getViaFactory.threshold(), getViaConstructor.threshold());
    }

    /*
    @Test
    public void test_Get_ssl() throws Exception {
        final String filename = "config/certs/http_ca.crt";
        try (InputStream in = new FileInputStream(filename)) {
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(in);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("server", certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            Curl.get("https://localhost:9200/").sslSocketFactory(sslContext.getSocketFactory()).execute(response -> {
                final String content = response.getContentAsString();
                logger.info(content);
                assertTrue(content.length() > 0);
            }, e -> {
                logger.log(Level.SEVERE, "error", e);
                fail();
            });
        }
    }*/
}
