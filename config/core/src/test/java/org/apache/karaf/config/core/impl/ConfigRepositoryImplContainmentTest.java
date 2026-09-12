/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.config.core.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

import org.apache.felix.utils.properties.TypedProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Verifies that {@link ConfigRepositoryImpl} refuses to persist a configuration outside of
 * {@code ${karaf.etc}}, whether the escape is attempted through a crafted PID or through a
 * caller-supplied {@code felix.fileinstall.filename} property.
 */
public class ConfigRepositoryImplContainmentTest {

    private Path etc;
    private Path outside;
    private String previousEtc;

    @Before
    public void setUp() throws Exception {
        Path base = Files.createTempDirectory("karaf-config-containment");
        etc = Files.createDirectories(base.resolve("etc"));
        outside = Files.createDirectories(base.resolve("outside"));
        previousEtc = System.getProperty("karaf.etc");
        System.setProperty("karaf.etc", etc.toFile().getCanonicalPath());
    }

    @After
    public void tearDown() {
        if (previousEtc == null) {
            System.clearProperty("karaf.etc");
        } else {
            System.setProperty("karaf.etc", previousEtc);
        }
    }

    private ConfigRepositoryImpl repository(String pid) throws Exception {
        ConfigurationAdmin admin = createMock(ConfigurationAdmin.class);
        Configuration config = createMock(Configuration.class);
        expect(admin.getConfiguration(pid, "?")).andReturn(config).anyTimes();
        expect(config.getProcessedProperties(anyObject())).andReturn(new Hashtable<>()).anyTimes();
        expect(config.getPid()).andReturn(pid).anyTimes();
        expect(config.getFactoryPid()).andReturn(null).anyTimes();
        config.update(anyObject());
        expectLastCall().anyTimes();
        replay(admin, config);
        return new ConfigRepositoryImpl(admin);
    }

    @Test
    public void traversalPidIsRejected() throws Exception {
        String pid = "../outside/evil";
        try {
            repository(pid).update(pid, new TypedProperties());
            fail("expected the traversal PID to be rejected");
        } catch (IOException e) {
            // expected
        }
        assertFalse("no file must be written outside etc", Files.exists(outside.resolve("evil.cfg")));
    }

    @Test
    public void fileinstallFilenameOutsideEtcIsRejected() throws Exception {
        String pid = "my.legit.pid";
        File escape = outside.resolve("evil.cfg").toFile();
        TypedProperties properties = new TypedProperties();
        properties.put("felix.fileinstall.filename", escape.toURI().toString());
        try {
            repository(pid).update(pid, properties);
            fail("expected the out-of-etc felix.fileinstall.filename to be rejected");
        } catch (IOException e) {
            // expected
        }
        assertFalse("no file must be written outside etc", escape.exists());
    }

    @Test
    public void factoryAliasTraversalIsRejected() throws Exception {
        ConfigurationAdmin admin = createMock(ConfigurationAdmin.class);
        Configuration config = createMock(Configuration.class);
        expect(admin.createFactoryConfiguration("my.factory", "?")).andReturn(config).anyTimes();
        expect(config.getPid()).andReturn("my.factory.generated").anyTimes();
        config.update(anyObject());
        expectLastCall().anyTimes();
        replay(admin, config);

        // the alias is concatenated as "<factoryPid>-<alias>.<suffix>", so an escaping payload
        // needs a leading path segment before the ".." sequence
        try {
            new ConfigRepositoryImpl(admin)
                    .createFactoryConfiguration("my.factory", "x/../../outside/evil", new TypedProperties());
            fail("expected the traversal alias to be rejected");
        } catch (IOException e) {
            // expected
        }
        assertFalse(Files.exists(outside.resolve("evil.cfg")));
    }

    @Test
    public void regularUpdateStillWritesInsideEtc() throws Exception {
        String pid = "my.regular.pid";
        TypedProperties properties = new TypedProperties();
        properties.put("hello", "world");
        repository(pid).update(pid, properties);
        assertTrue("the cfg file must land in etc", Files.exists(etc.resolve("my.regular.pid.cfg")));
    }
}
