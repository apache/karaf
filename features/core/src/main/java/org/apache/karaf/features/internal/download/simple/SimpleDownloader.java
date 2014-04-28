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
package org.apache.karaf.features.internal.download.simple;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.karaf.features.internal.download.DownloadCallback;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.util.MultiException;

import static java.util.jar.JarFile.MANIFEST_NAME;

public class SimpleDownloader implements DownloadManager, Downloader {

    protected final MultiException exception = new MultiException("Error");

    protected final ConcurrentMap<String, StreamProvider> providers = new ConcurrentHashMap<String, StreamProvider>();

    @Override
    public Downloader createDownloader() {
        return this;
    }

    @Override
    public void await() throws InterruptedException, MultiException {
        exception.throwIfExceptions();
    }

    @Override
    public void download(final String location, final DownloadCallback downloadCallback) throws MalformedURLException {
        if (!providers.containsKey(location)) {
            providers.putIfAbsent(location, createProvider(location));
        }
        try {
            if (downloadCallback != null) {
                downloadCallback.downloaded(providers.get(location));
            }
        } catch (Exception e) {
            exception.addException(e);
        }
    }

    protected StreamProvider createProvider(String location) throws MalformedURLException {
        return new UrlProvider(new URL(location));
    }

    public Map<String, StreamProvider> getProviders() {
        return providers;
    }

    static class UrlProvider implements StreamProvider {
        private final URL url;
        private volatile Map<String, String> metadata;

        UrlProvider(URL url) {
            this.url = url;
        }

        @Override
        public InputStream open() throws IOException {
            return url.openStream();
        }

        @Override
        public Map<String, String> getMetadata() throws IOException {
            if (metadata == null) {
                synchronized (this) {
                    if (metadata == null) {
                        metadata = doGetMetadata();
                    }
                }
            }
            return metadata;
        }

        protected Map<String, String> doGetMetadata() throws IOException {
            try (
                    InputStream is = open()
            ) {
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (MANIFEST_NAME.equals(entry.getName())) {
                        Attributes attributes = new Manifest(zis).getMainAttributes();
                        Map<String, String> headers = new HashMap<String, String>();
                        for (Map.Entry attr : attributes.entrySet()) {
                            headers.put(attr.getKey().toString(), attr.getValue().toString());
                        }
                        return headers;
                    }
                }
            }
            throw new IllegalArgumentException("Resource " + url + " does not contain a manifest");
        }
    }
}
