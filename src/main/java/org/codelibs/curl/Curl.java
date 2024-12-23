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
package org.codelibs.curl;

import java.io.File;

/**
 * The Curl class provides a simple interface for creating HTTP requests using various HTTP methods.
 * It includes static methods for each HTTP method that return a CurlRequest object.
 *
 * <p>Example usage:</p>
 * <pre>
 * CurlRequest request = Curl.get("http://example.com");
 * </pre>
 *
 * <p>Supported HTTP methods:</p>
 * <ul>
 *   <li>GET</li>
 *   <li>POST</li>
 *   <li>PUT</li>
 *   <li>DELETE</li>
 *   <li>HEAD</li>
 *   <li>OPTIONS</li>
 *   <li>CONNECT</li>
 * </ul>
 *
 * <p>The Curl class also defines an enum {@link Method} which lists all supported HTTP methods.</p>
 *
 * <p>The temporary directory used by Curl is defined by the {@code tmpDir} field, which is initialized
 * to the system's temporary directory.</p>
 */
public class Curl {

    public static final File tmpDir = new File(System.getProperty("java.io.tmpdir"));

    protected Curl() {
        // nothing
    }

    /**
     * Creates a new CurlRequest with the HTTP GET method for the specified URL.
     *
     * @param url the URL to send the GET request to
     * @return a CurlRequest object configured with the GET method and the specified URL
     */
    public static CurlRequest get(final String url) {
        return new CurlRequest(Method.GET, url);
    }

    /**
     * Creates a new CurlRequest with the HTTP POST method for the specified URL.
     *
     * @param url the URL to which the POST request will be sent
     * @return a new CurlRequest object configured with the POST method and the specified URL
     */
    public static CurlRequest post(final String url) {
        return new CurlRequest(Method.POST, url);
    }

    /**
     * Creates a new CurlRequest with the HTTP PUT method for the specified URL.
     *
     * @param url the URL to which the PUT request will be sent
     * @return a new CurlRequest object configured with the PUT method and the specified URL
     */
    public static CurlRequest put(final String url) {
        return new CurlRequest(Method.PUT, url);
    }

    /**
     * Creates a new CurlRequest with the DELETE method for the specified URL.
     *
     * @param url the URL to which the DELETE request will be sent
     * @return a CurlRequest object configured with the DELETE method and the specified URL
     */
    public static CurlRequest delete(final String url) {
        return new CurlRequest(Method.DELETE, url);
    }

    /**
     * Creates a new CurlRequest with the HTTP HEAD method for the specified URL.
     *
     * @param url the URL to which the HEAD request is to be made
     * @return a CurlRequest object configured with the HEAD method and the specified URL
     */
    public static CurlRequest head(final String url) {
        return new CurlRequest(Method.HEAD, url);
    }

    /**
     * Creates a new CurlRequest with the HTTP OPTIONS method for the specified URL.
     *
     * @param url the URL to which the OPTIONS request is sent
     * @return a new CurlRequest object configured with the OPTIONS method and the specified URL
     */
    public static CurlRequest options(final String url) {
        return new CurlRequest(Method.OPTIONS, url);
    }

    /**
     * Creates a new CurlRequest with the CONNECT method for the specified URL.
     *
     * @param url the URL to connect to
     * @return a new CurlRequest object with the CONNECT method
     */
    public static CurlRequest connect(final String url) {
        return new CurlRequest(Method.CONNECT, url);
    }

    /**
     * Enumeration representing HTTP methods.
     * <ul>
     *   <li>GET - Requests data from a specified resource.</li>
     *   <li>POST - Submits data to be processed to a specified resource.</li>
     *   <li>PUT - Updates a current resource with new data.</li>
     *   <li>DELETE - Deletes the specified resource.</li>
     *   <li>HEAD - Same as GET but returns only HTTP headers and no document body.</li>
     *   <li>OPTIONS - Returns the HTTP methods that the server supports.</li>
     *   <li>TRACE - Echoes back the received request, used for debugging.</li>
     *   <li>CONNECT - Converts the request connection to a transparent TCP/IP tunnel.</li>
     * </ul>
     */
    public enum Method {
        GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, CONNECT;
    }

}
