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
import java.util.Hashtable;

import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MavenConfigurationTest {

    @Test
    public void testDefaultConfiguration() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        MavenConfiguration config = new MavenConfiguration(context, null);

        assertEquals(new File(System.getProperty("user.home"), ".m2/repository"), config.getLocalRepository());
        assertTrue(config.getDefaultRepositories().isEmpty());
        assertTrue(config.getRepositories().isEmpty());
        assertNull(config.getUpdatePolicy());
        assertNull(config.getChecksumPolicy());
        assertEquals(5000, config.getConnectionTimeout());
        assertEquals(30000, config.getReadTimeout());
        assertTrue(config.isCertificateCheck());
        assertEquals(3, config.getRetryCount());

        verify(context);
    }

    @Test
    public void testCustomLocalRepository() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("localRepository", "/tmp/test-repo");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(new File("/tmp/test-repo"), config.getLocalRepository());

        verify(context);
    }

    @Test
    public void testFullyQualifiedPropertyKey() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("org.apache.karaf.url.mvn.localRepository", "/tmp/fq-repo");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(new File("/tmp/fq-repo"), config.getLocalRepository());

        verify(context);
    }

    @Test
    public void testRepositoriesParsing() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("repositories", "https://repo1.maven.org/maven2@id=central, https://repo.apache.org@id=apache");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(2, config.getRepositories().size());
        assertEquals("https://repo1.maven.org/maven2@id=central", config.getRepositories().get(0));
        assertEquals("https://repo.apache.org@id=apache", config.getRepositories().get(1));

        verify(context);
    }

    @Test
    public void testDefaultRepositoriesParsing() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("defaultRepositories", "/opt/karaf/system@id=system@snapshots, /opt/karaf/data/kar@id=kar");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(2, config.getDefaultRepositories().size());

        verify(context);
    }

    @Test
    public void testTimeoutConfiguration() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("timeout", "10000");
        props.put("socket.connectionTimeout", "3000");
        props.put("socket.readTimeout", "60000");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(3000, config.getConnectionTimeout());
        assertEquals(60000, config.getReadTimeout());

        verify(context);
    }

    @Test
    public void testTimeoutFallsBackToDefaultTimeout() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("timeout", "7000");

        MavenConfiguration config = new MavenConfiguration(context, props);

        // connectionTimeout should fall back to the "timeout" value
        assertEquals(7000, config.getConnectionTimeout());

        verify(context);
    }

    @Test
    public void testCertificateCheckDisabled() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("certificateCheck", "false");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertFalse(config.isCertificateCheck());

        verify(context);
    }

    @Test
    public void testRetryCount() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("connection.retryCount", "5");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(5, config.getRetryCount());

        verify(context);
    }

    @Test
    public void testPolicies() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("globalUpdatePolicy", "daily");
        props.put("globalChecksumPolicy", "fail");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals("daily", config.getUpdatePolicy());
        assertEquals("fail", config.getChecksumPolicy());

        verify(context);
    }

    @Test
    public void testVariableSubstitution() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty("karaf.home")).andReturn("/opt/karaf").anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".localRepository")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".defaultRepositories")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".repositories")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".globalUpdatePolicy")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".globalChecksumPolicy")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".timeout")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".socket.connectionTimeout")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".socket.readTimeout")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".certificateCheck")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".connection.retryCount")).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("defaultRepositories", "${karaf.home}/system@id=system");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(1, config.getDefaultRepositories().size());
        assertEquals("/opt/karaf/system@id=system", config.getDefaultRepositories().get(0));

        verify(context);
    }

    @Test
    public void testRelativeLocalRepositoryResolved() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty("karaf.home")).andReturn("/opt/karaf").anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".localRepository")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".defaultRepositories")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".repositories")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".globalUpdatePolicy")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".globalChecksumPolicy")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".timeout")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".socket.connectionTimeout")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".socket.readTimeout")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".certificateCheck")).andReturn(null).anyTimes();
        expect(context.getProperty(MavenConfiguration.PID + ".connection.retryCount")).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("localRepository", "data/repo");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(new File("/opt/karaf", "data/repo"), config.getLocalRepository());

        verify(context);
    }

    @Test
    public void testInvalidIntPropertyUsesDefault() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("connection.retryCount", "notanumber");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(3, config.getRetryCount());

        verify(context);
    }

    @Test
    public void testEmptyRepositoriesString() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("repositories", "   ");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertTrue(config.getRepositories().isEmpty());

        verify(context);
    }

}
