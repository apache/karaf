/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.services.url.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.karaf.services.url.MavenResolver;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi URL stream handler for the {@code mvn:} protocol.
 * <p>
 * Syntax: {@code mvn:[repository_url!]groupId/artifactId[/[version][/[type][/classifier]]]}
 */
public class MvnUrlHandler extends AbstractURLStreamHandlerService {

    private static final Logger LOG = LoggerFactory.getLogger(MvnUrlHandler.class);

    private final MavenResolver resolver;

    public MvnUrlHandler(MavenResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        LOG.debug("Opening connection for mvn: URL {}", url);
        return new MvnConnection(url, resolver);
    }

    static class MvnConnection extends URLConnection {

        private final MavenResolver resolver;
        private File resolvedFile;

        MvnConnection(URL url, MavenResolver resolver) {
            super(url);
            this.resolver = resolver;
        }

        @Override
        public void connect() throws IOException {
            if (resolvedFile == null) {
                String mvnUrl = url.toExternalForm();
                resolvedFile = resolver.resolve(mvnUrl);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return new FileInputStream(resolvedFile);
        }

        @Override
        public long getContentLengthLong() {
            try {
                connect();
                return resolvedFile.length();
            } catch (IOException e) {
                return -1;
            }
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

    }

}
