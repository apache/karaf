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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.download.simple.SimpleDownloader;

public class TestDownloadManager extends SimpleDownloader {

    private final Class loader;
    private final String dir;

    public TestDownloadManager(Class loader, String dir) {
        this.loader = loader;
        this.dir = dir;
    }

    @Override
    protected StreamProvider createProvider(String location) throws MalformedURLException {
        return new TestProvider(location);
    }

    class TestProvider implements StreamProvider {
        private final IOException exception;
        private final Map<String, String> headers;
        private final byte[] data;

        TestProvider(String location) {
            Map<String, String> headers = null;
            byte[] data = null;
            IOException exception = null;
            try {
                Manifest man = new Manifest(loader.getResourceAsStream(dir + "/" + location + ".mf"));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JarOutputStream jos = new JarOutputStream(baos, man);
                jos.close();
                data = baos.toByteArray();
                headers = new HashMap<>();
                for (Map.Entry attr : man.getMainAttributes().entrySet()) {
                    headers.put(attr.getKey().toString(), attr.getValue().toString());
                }
            } catch (IOException e) {
                exception = e;
            }
            this.headers = headers;
            this.data = data;
            this.exception = exception;
        }

        @Override
        public InputStream open() throws IOException {
            if (exception != null)
                throw exception;
            return new ByteArrayInputStream(data);
        }

        @Override
        public Map<String, String> getMetadata() throws IOException {
            if (exception != null)
                throw exception;
            return headers;
        }
    }
}
