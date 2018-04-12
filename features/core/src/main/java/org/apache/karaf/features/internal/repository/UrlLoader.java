/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.internal.repository;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 */
public abstract class UrlLoader {

    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String GZIP = "gzip";

    private final String url;
    private final long expiration;
    private long lastModified;
    private long lastChecked;

    public UrlLoader(String url, long expiration) {
        this.url = url;
        this.expiration = expiration;
    }

    public String getUrl() {
        return url;
    }

    protected boolean checkAndLoadCache() {
        long time = System.currentTimeMillis();
        if (lastChecked > 0) {
            if (expiration < 0 || time - lastChecked < expiration) {
                return false;
            }
        }
        try {
            URL u = new java.net.URL(url);
            URLConnection connection = u.openConnection();
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection con = (HttpURLConnection) connection;
                if (lastModified > 0) {
                    con.setIfModifiedSince(lastModified);
                }
                con.setRequestProperty(HEADER_ACCEPT_ENCODING, GZIP);
                if (u.getUserInfo() != null)  {
                    String encoded = java.util.Base64.getEncoder().encodeToString((u.getUserInfo()).getBytes(StandardCharsets.UTF_8));
                    connection.setRequestProperty("Authorization", "Basic " + encoded);
                }
                int rc = con.getResponseCode();
                if (rc == HTTP_NOT_MODIFIED) {
                    lastChecked = time;
                    return false;
                }
                if (rc != HTTP_OK) {
                    throw new IOException("Unexpected http response loading " + url + " : " + rc + " " + con.getResponseMessage());
                }
            }
            
            if (didNotChange(connection)) {
                lastChecked = time;
                return false;
            }
            boolean wasRead = read(connection);
            lastModified = connection.getLastModified();
            lastChecked = time;
            return wasRead;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean didNotChange(URLConnection connection) {
        long lm = connection.getLastModified();
        return lm > 0 && lm <= lastModified;
    }

    private boolean read(URLConnection connection) throws IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(connection.getInputStream());
            if (isGzipStream(is)) {
                is = new GZIPInputStream(is);
            }
            return doRead(is);
        } finally {
            // cannot be use try-with-resources, as it would not close GZIPInpuStream
            if (is != null) {
                is.close();
            }
        }
    }

    private boolean isGzipStream(InputStream is) throws IOException {
        is.mark(512);
        int b0 = is.read();
        int b1 = is.read();
        is.reset();
        return (b0 == 0x1f && b1 == 0x8b);
    }

    protected abstract boolean doRead(InputStream is) throws IOException;

}
