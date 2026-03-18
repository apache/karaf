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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.karaf.services.url.MavenResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MvnUrlHandlerTest {

    private File tempDir;
    private File testFile;

    @Before
    public void setUp() throws Exception {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "karaf-url-handler-test-" + System.nanoTime());
        tempDir.mkdirs();
        testFile = new File(tempDir, "test-artifact.jar");
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write("test-jar-content".getBytes());
        }
    }

    @After
    public void tearDown() {
        if (testFile != null) {
            testFile.delete();
        }
        if (tempDir != null) {
            tempDir.delete();
        }
    }

    @Test
    public void testOpenConnection() throws Exception {
        MavenResolver resolver = createMock(MavenResolver.class);
        replay(resolver);

        MvnUrlHandler handler = new MvnUrlHandler(resolver);
        URL url = new URL("http://localhost/mvn:org.example/test/1.0.0");
        URLConnection connection = handler.openConnection(url);

        assertNotNull(connection);

        verify(resolver);
    }

    @Test
    public void testConnectionGetInputStream() throws Exception {
        MavenResolver resolver = createMock(MavenResolver.class);
        expect(resolver.resolve(anyString())).andReturn(testFile);
        replay(resolver);

        URL url = new URL("http://localhost/mvn:org.example/test/1.0.0");
        MvnUrlHandler.MvnConnection connection = new MvnUrlHandler.MvnConnection(url, resolver);

        try (InputStream is = connection.getInputStream()) {
            assertNotNull(is);
            byte[] content = is.readAllBytes();
            assertEquals("test-jar-content", new String(content));
        }

        verify(resolver);
    }

    @Test
    public void testConnectionGetContentLengthLong() throws Exception {
        MavenResolver resolver = createMock(MavenResolver.class);
        expect(resolver.resolve(anyString())).andReturn(testFile);
        replay(resolver);

        URL url = new URL("http://localhost/mvn:org.example/test/1.0.0");
        MvnUrlHandler.MvnConnection connection = new MvnUrlHandler.MvnConnection(url, resolver);

        assertEquals(testFile.length(), connection.getContentLengthLong());

        verify(resolver);
    }

    @Test
    public void testConnectionGetContentLengthLongOnError() throws Exception {
        MavenResolver resolver = createMock(MavenResolver.class);
        expect(resolver.resolve(anyString())).andThrow(new IOException("not found"));
        replay(resolver);

        URL url = new URL("http://localhost/mvn:org.example/test/1.0.0");
        MvnUrlHandler.MvnConnection connection = new MvnUrlHandler.MvnConnection(url, resolver);

        assertEquals(-1, connection.getContentLengthLong());

        verify(resolver);
    }

    @Test
    public void testConnectionGetContentType() throws Exception {
        MavenResolver resolver = createMock(MavenResolver.class);
        replay(resolver);

        URL url = new URL("http://localhost/mvn:org.example/test/1.0.0");
        MvnUrlHandler.MvnConnection connection = new MvnUrlHandler.MvnConnection(url, resolver);

        assertEquals("application/octet-stream", connection.getContentType());

        verify(resolver);
    }

    @Test
    public void testConnectionConnectIsIdempotent() throws Exception {
        MavenResolver resolver = createMock(MavenResolver.class);
        // resolve should only be called once even if connect is called multiple times
        expect(resolver.resolve(anyString())).andReturn(testFile).once();
        replay(resolver);

        URL url = new URL("http://localhost/mvn:org.example/test/1.0.0");
        MvnUrlHandler.MvnConnection connection = new MvnUrlHandler.MvnConnection(url, resolver);

        connection.connect();
        connection.connect();

        verify(resolver);
    }

    @Test(expected = IOException.class)
    public void testConnectionGetInputStreamThrowsOnResolveFailure() throws Exception {
        MavenResolver resolver = createMock(MavenResolver.class);
        expect(resolver.resolve(anyString())).andThrow(new IOException("artifact not found"));
        replay(resolver);

        URL url = new URL("http://localhost/mvn:org.example/test/1.0.0");
        MvnUrlHandler.MvnConnection connection = new MvnUrlHandler.MvnConnection(url, resolver);

        connection.getInputStream();
    }

}
