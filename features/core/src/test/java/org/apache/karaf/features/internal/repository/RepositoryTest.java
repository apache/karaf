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
package org.apache.karaf.features.internal.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

import org.apache.felix.utils.repository.BaseRepository;
import org.apache.karaf.util.StreamUtils;
import org.junit.Test;
import org.osgi.resource.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.osgi.framework.namespace.BundleNamespace.BUNDLE_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

public class RepositoryTest {

    @Test
    public void testXml() throws Exception {
        URL url = getClass().getResource("repo.xml");
        XmlRepository repo = new XmlRepository(url.toExternalForm(), 0, false);
        verify(repo);
    }

    @Test
    public void testJson() throws Exception {
        URL url = getClass().getResource("repo.json");
        JsonRepository repo = new JsonRepository(url.toExternalForm(), 0, false);
        verify(repo);
    }

    @Test
    public void testXmlGzip() throws Exception {
        URL url = getClass().getResource("repo.xml");
        url = gzip(url);
        XmlRepository repo = new XmlRepository(url.toExternalForm(), 0, false);
        verify(repo);
    }

    @Test
    public void testJsonGzip() throws Exception {
        URL url = getClass().getResource("repo.json");
        url = gzip(url);
        JsonRepository repo = new JsonRepository(url.toExternalForm(), 0, false);
        verify(repo);
    }

    private void verify(BaseRepository repo) {
        assertNotNull(repo.getResources());
        assertEquals(1, repo.getResources().size());
        Resource resource = repo.getResources().get(0);
        assertNotNull(resource);
        assertEquals(1, resource.getCapabilities(IDENTITY_NAMESPACE).size());
        assertEquals(1, resource.getCapabilities(BUNDLE_NAMESPACE).size());
        assertEquals(1, resource.getCapabilities(PACKAGE_NAMESPACE).size());
        assertEquals(1, resource.getRequirements(PACKAGE_NAMESPACE).size());
    }

    private URL gzip(URL url) throws IOException {
        File temp = File.createTempFile("repo", ".tmp");
        try (
            OutputStream os = new GZIPOutputStream(new FileOutputStream(temp));
            InputStream is = url.openStream()
        ) {
            StreamUtils.copy(is, os);
        }
        return temp.toURI().toURL();
    }

}
