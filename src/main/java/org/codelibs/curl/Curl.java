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

public class Curl {

    public static final File tmpDir = new File(System.getProperty("java.io.tmpdir"));

    protected Curl() {
        // nothing
    }

    public static CurlRequest get(final String url) {
        return new CurlRequest(Method.GET, url);
    }

    public static CurlRequest post(final String url) {
        return new CurlRequest(Method.POST, url);
    }

    public static CurlRequest put(final String url) {
        return new CurlRequest(Method.PUT, url);
    }

    public static CurlRequest delete(final String url) {
        return new CurlRequest(Method.DELETE, url);
    }

    public static CurlRequest head(final String url) {
        return new CurlRequest(Method.HEAD, url);
    }

    public static CurlRequest options(final String url) {
        return new CurlRequest(Method.OPTIONS, url);
    }

    public static CurlRequest connect(final String url) {
        return new CurlRequest(Method.CONNECT, url);
    }

    public enum Method {
        GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, CONNECT;
    }

}
