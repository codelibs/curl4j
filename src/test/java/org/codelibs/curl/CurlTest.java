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
