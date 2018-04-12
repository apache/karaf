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
package org.apache.karaf.features.internal.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.karaf.features.internal.download.DownloadCallback;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.util.MultiException;

public class TestDownloadManager implements DownloadManager, Downloader {

    private final MultiException exception = new MultiException("Error");
    private final ConcurrentMap<String, StreamProvider> providers = new ConcurrentHashMap<>();
    private final Class<?> loader;
    private final String dir;

    public TestDownloadManager(Class<?> loader, String dir) {
        this.loader = loader;
        this.dir = dir;
    }

    @Override
    public Downloader createDownloader() {
        return this;
    }

    @Override
    public Map<String, StreamProvider> getProviders() {
        return providers;
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
            exception.addSuppressed(e);
        }
    }

    protected StreamProvider createProvider(String location) throws MalformedURLException {
        return new TestProvider(location);
    }

    class TestProvider implements StreamProvider {
        private final String location;
        private final IOException exception;
        private final byte[] data;

        TestProvider(String location) {
            byte[] data = null;
            IOException exception = null;
            try {
                String loc = dir + "/" + location + ".mf";
                InputStream is = loader.getResourceAsStream(loc);
                if (is == null) {
                    throw new IllegalStateException("Could not find resource: " + loc);
                }
                Manifest man = new Manifest(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JarOutputStream jos = new JarOutputStream(baos, man);
                jos.close();
                data = baos.toByteArray();
            } catch (IOException e) {
                exception = e;
            }
            this.location = location;
            this.data = data;
            this.exception = exception;
        }

        public String getUrl() {
            return location;
        }

        @Override
        public InputStream open() throws IOException {
            if (exception != null)
                throw exception;
            return new ByteArrayInputStream(data);
        }

        @Override
        public File getFile() throws IOException {
            throw new UnsupportedOperationException();
        }

    }
}
