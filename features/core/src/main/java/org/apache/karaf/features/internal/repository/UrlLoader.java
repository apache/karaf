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
import java.net.URLConnection;
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
            URLConnection connection = new java.net.URL(url).openConnection();
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection con = (HttpURLConnection) connection;
                if (lastModified > 0) {
                    con.setIfModifiedSince(lastModified);
                }
                con.setRequestProperty(HEADER_ACCEPT_ENCODING, GZIP);
                int rc = con.getResponseCode();
                if (rc == HTTP_NOT_MODIFIED) {
                    lastChecked = time;
                    return false;
                }
                if (rc != HTTP_OK) {
                    throw new IOException("Unexpected http response loading " + url + " : " + rc + " " + con.getResponseMessage());
                }
            }
            long lm = connection.getLastModified();
            if (lm > 0 && lm <= lastModified) {
                lastChecked = time;
                return false;
            }
            try (
                    BufferedInputStream bis = new BufferedInputStream(connection.getInputStream())
            ) {
                // Auto-detect gzipped streams
                InputStream is = bis;
                bis.mark(512);
                int b0 = bis.read();
                int b1 = bis.read();
                bis.reset();
                if (b0 == 0x1f && b1 == 0x8b) {
                    is = new GZIPInputStream(bis);
                }
                boolean r = doRead(is);
                lastModified = lm;
                lastChecked = time;
                return r;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract boolean doRead(InputStream is) throws IOException;

}
