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
package org.apache.karaf.services.web.internal.wab;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.AbstractResource;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * An entry of a bundle, file or directory.
 */
class BundleResource extends AbstractResource {

    private static final Log LOG = LogFactory.getLog(BundleResource.class);

    private final URL url;
    private final boolean directory;
    private final long bundleLastModified;
    private volatile long contentLength = -2;
    private volatile long lastModified = -2;

    BundleResource(WebResourceRoot root, String webAppPath, URL url, boolean directory, long bundleLastModified) {
        super(root, webAppPath);
        this.url = url;
        this.directory = directory;
        this.bundleLastModified = bundleLastModified;
    }

    private void readAttributes() {
        if (contentLength != -2) {
            return;
        }
        long length = -1;
        long modified = 0;
        if (!directory && url != null) {
            try {
                URLConnection connection = url.openConnection();
                length = connection.getContentLengthLong();
                modified = connection.getLastModified();
                connection.getInputStream().close();
            } catch (IOException e) {
                LOG.debug("Unable to read the attributes of " + url, e);
            }
        }
        lastModified = modified > 0 ? modified : bundleLastModified;
        contentLength = directory ? 0 : length;
    }

    @Override
    public long getLastModified() {
        readAttributes();
        return lastModified;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    @Override
    public boolean isFile() {
        return !directory;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public String getName() {
        String path = getWebappPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public long getContentLength() {
        readAttributes();
        return contentLength;
    }

    @Override
    public String getCanonicalPath() {
        return null;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    protected InputStream doGetInputStream() {
        if (directory || url == null) {
            return null;
        }
        try {
            return url.openStream();
        } catch (IOException e) {
            LOG.debug("Unable to read " + url, e);
            return null;
        }
    }

    @Override
    public byte[] getContent() {
        if (directory || getContentLength() > Integer.MAX_VALUE) {
            return null;
        }
        try (InputStream is = doGetInputStream()) {
            return is == null ? null : is.readAllBytes();
        } catch (IOException e) {
            LOG.debug("Unable to read " + url, e);
            return null;
        }
    }

    @Override
    public long getCreation() {
        return getLastModified();
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public URL getCodeBase() {
        return null;
    }

    @Override
    public Certificate[] getCertificates() {
        return null;
    }

    @Override
    public Manifest getManifest() {
        return null;
    }

    @Override
    protected Log getLog() {
        return LOG;
    }

}
