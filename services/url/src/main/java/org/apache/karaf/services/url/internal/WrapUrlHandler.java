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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi URL stream handler for the {@code wrap:} protocol, turning a plain jar into an OSGi bundle on the fly.
 * <p>
 * Syntax: {@code wrap:wrapped-jar-url[,wrapping-instructions-url][$wrapping-instructions]}
 */
public class WrapUrlHandler extends AbstractURLStreamHandlerService {

    private static final Logger LOG = LoggerFactory.getLogger(WrapUrlHandler.class);

    private static final String PROTOCOL = "wrap:";

    private static final String OVERWRITE = "overwrite";

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        LOG.debug("Opening connection for wrap: URL {}", url);
        return new WrapConnection(url);
    }

    static class WrapConnection extends URLConnection {

        private final WrapUrlParser parser;

        WrapConnection(URL url) throws MalformedURLException {
            super(url);
            // the wrapped URL and the instructions can contain ? and #, so the path is not enough
            String path = url.toExternalForm();
            if (path.startsWith(PROTOCOL)) {
                path = path.substring(PROTOCOL.length());
            }
            this.parser = new WrapUrlParser(path);
        }

        @Override
        public void connect() {
            // nothing to do, the wrapped jar is read when the stream is requested
        }

        @Override
        public InputStream getInputStream() throws IOException {
            Properties instructions = new Properties();
            if (parser.getInstructionsUrl() != null) {
                try (InputStream is = parser.getInstructionsUrl().openStream()) {
                    instructions.load(is);
                }
            }
            // the instructions provided in the URL win over the ones from the instructions file
            instructions.putAll(parser.getInstructions());
            OverwriteMode overwriteMode = OverwriteMode.parse((String) instructions.remove(OVERWRITE));
            try (InputStream is = parser.getWrappedJarUrl().openStream()) {
                return BundleWrapper.wrap(is, instructions, url.toExternalForm(), overwriteMode);
            }
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

    }

}
