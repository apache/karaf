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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.stream.Stream;

import org.apache.felix.utils.properties.TypedProperties;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Verifies that {@link ConfigRepositoryImpl} only persists a configuration to its own file: inside
 * {@code ${karaf.etc}}, with a configuration extension, and named after the PID. The target is
 * attempted through a crafted PID, factory alias, storage type (suffix) or caller-supplied
 * {@code felix.fileinstall.filename} property.
 */
public class ConfigRepositoryImplContainmentTest {

    private static final String USERS = "karaf = karaf,_g_:admingroup\n_g_\\:admingroup = group,admin,manager,viewer\n";
    private static final String JMX_ACL = "update(java.lang.String,java.util.Map)[/jmx[.]acl.*/,/.*/] = admin\n";
    private static final String FILEINSTALL_FILE_NAME = "felix.fileinstall.filename";

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

    @Test
    public void fileinstallFilenameToReservedEtcFileIsRejected() throws Exception {
        Path users = Files.write(etc.resolve("users.properties"), USERS.getBytes(UTF_8));
        String pid = "my.new.pid";
        TypedProperties properties = new TypedProperties();
        properties.put("felix.fileinstall.filename", users.toUri().toString());
        properties.put("intruder", "intruder,_g_:admingroup");
        ConfigRepositoryImpl repository = repository(pid);
        assertThrows(IOException.class, () -> repository.update(pid, properties));
        assertEquals("users.properties must be left untouched", USERS, new String(Files.readAllBytes(users), UTF_8));
    }

    @Test
    public void fileinstallFilenameToAnotherPidFileIsRejected() throws Exception {
        Path acl = Files.write(etc.resolve("jmx.acl.org.apache.karaf.config.cfg"), JMX_ACL.getBytes(UTF_8));
        String pid = "my.new.pid";
        TypedProperties properties = new TypedProperties();
        properties.put("felix.fileinstall.filename", acl.toUri().toString());
        properties.put("update(java.lang.String,java.util.Map)", "manager");
        ConfigRepositoryImpl repository = repository(pid);
        assertThrows(IOException.class, () -> repository.update(pid, properties));
        assertEquals("the ACL file must be left untouched", JMX_ACL, new String(Files.readAllBytes(acl), UTF_8));
    }

    @Test
    public void suffixNamingReservedEtcFileIsRejected() throws Exception {
        Path users = Files.write(etc.resolve("users.properties"), USERS.getBytes(UTF_8));
        String pid = "users";
        TypedProperties properties = new TypedProperties();
        properties.put("intruder", "intruder,_g_:admingroup");
        ConfigRepositoryImpl repository = repository(pid);
        assertThrows(IOException.class, () -> repository.update(pid, properties, "properties"));
        assertEquals("users.properties must be left untouched", USERS, new String(Files.readAllBytes(users), UTF_8));
    }

    @Test
    public void suffixNamingAnotherPidFileIsRejected() throws Exception {
        Path acl = Files.write(etc.resolve("jmx.acl.org.apache.karaf.config.cfg"), JMX_ACL.getBytes(UTF_8));
        String pid = "jmx";
        TypedProperties properties = new TypedProperties();
        properties.put("update(java.lang.String,java.util.Map)", "manager");
        ConfigRepositoryImpl repository = repository(pid);
        assertThrows(IOException.class, () -> repository.update(pid, properties, "acl.org.apache.karaf.config.cfg"));
        assertEquals("the ACL file must be left untouched", JMX_ACL, new String(Files.readAllBytes(acl), UTF_8));
    }

    @Test
    public void pidDifferingOnlyByCaseIsRejectedOnCaseInsensitiveFileSystems() throws Exception {
        Path acl = Files.write(etc.resolve("jmx.acl.org.apache.karaf.config.cfg"), JMX_ACL.getBytes(UTF_8));
        assumeTrue("needs a case-insensitive file system", Files.exists(etc.resolve("JMX.ACL.org.apache.karaf.config.cfg")));
        // the ACL patterns are case sensitive, so this PID is not protected by them
        String pid = "JMX.ACL.org.apache.karaf.config";
        TypedProperties properties = new TypedProperties();
        properties.put("update(java.lang.String,java.util.Map)", "manager");
        ConfigRepositoryImpl repository = repository(pid);
        assertThrows(IOException.class, () -> repository.update(pid, properties));
        assertEquals("the ACL file must be left untouched", JMX_ACL, new String(Files.readAllBytes(acl), UTF_8));
    }

    @Test
    public void factoryUnsupportedSuffixIsRejected() throws Exception {
        ConfigurationAdmin admin = createMock(ConfigurationAdmin.class);
        replay(admin);
        ConfigRepositoryImpl repository = new ConfigRepositoryImpl(admin);
        assertThrows(IOException.class,
                () -> repository.createFactoryConfiguration("my.factory", null, new TypedProperties(), "properties"));
        assertThrows(IOException.class,
                () -> repository.createFactoryConfiguration("my.factory", "alias", new TypedProperties(), "properties"));
        try (Stream<Path> files = Files.list(etc)) {
            assertEquals("no file must be written", 0, files.count());
        }
    }

    @Test
    public void fileinstallFilenameOfThePidIsHonored() throws Exception {
        String pid = "my.json.pid";
        Path json = etc.resolve(pid + ".cfg.json");
        TypedProperties properties = new TypedProperties();
        properties.put("felix.fileinstall.filename", json.toUri().toString());
        properties.put("hello", "world");
        repository(pid).update(pid, properties);
        assertTrue("the configuration must be written to the provided file", Files.exists(json));
        assertFalse(Files.exists(etc.resolve(pid + ".cfg")));
    }

    @Test
    public void jsonSuffixStillWritesInsideEtc() throws Exception {
        String pid = "my.json.pid";
        TypedProperties properties = new TypedProperties();
        properties.put("hello", "world");
        repository(pid).update(pid, properties, "cfg.json");
        assertTrue("the json file must land in etc", Files.exists(etc.resolve("my.json.pid.cfg.json")));
    }

    @Test
    public void factoryAliasStillWritesInsideEtc() throws Exception {
        ConfigurationAdmin admin = createMock(ConfigurationAdmin.class);
        Configuration config = createMock(Configuration.class);
        expect(admin.createFactoryConfiguration("my.factory", "?")).andReturn(config).anyTimes();
        expect(config.getPid()).andReturn("my.factory.generated").anyTimes();
        config.update(anyObject());
        expectLastCall().anyTimes();
        replay(admin, config);
        TypedProperties properties = new TypedProperties();
        properties.put("hello", "world");
        new ConfigRepositoryImpl(admin).createFactoryConfiguration("my.factory", "alias", properties);
        assertTrue("the cfg file must land in etc", Files.exists(etc.resolve("my.factory-alias.cfg")));
    }

    /**
     * A repository whose configuration already has a backing file, recorded in the stored
     * {@code felix.fileinstall.filename} property. The properties pushed to ConfigAdmin are captured.
     */
    private ConfigRepositoryImpl repository(String pid, Path storedFile, Capture<Dictionary<String, ?>> pushed) throws Exception {
        ConfigurationAdmin admin = createMock(ConfigurationAdmin.class);
        Configuration config = createMock(Configuration.class);
        Hashtable<String, Object> stored = new Hashtable<>();
        stored.put(FILEINSTALL_FILE_NAME, storedFile.toUri().toString());
        expect(admin.getConfiguration(pid, "?")).andReturn(config).anyTimes();
        expect(config.getProcessedProperties(anyObject())).andReturn(stored).anyTimes();
        expect(config.getPid()).andReturn(pid).anyTimes();
        expect(config.getFactoryPid()).andReturn(null).anyTimes();
        config.update(capture(pushed));
        expectLastCall().anyTimes();
        replay(admin, config);
        return new ConfigRepositoryImpl(admin);
    }

    @Test
    public void storedFilenameToNonConfigFileIsRejected() throws Exception {
        Path users = Files.write(etc.resolve("users.properties"), USERS.getBytes(UTF_8));
        String pid = "my.legit.pid";
        TypedProperties properties = new TypedProperties();
        properties.put("intruder", "intruder,_g_:admingroup");
        ConfigRepositoryImpl repository = repository(pid, users, Capture.newInstance());
        assertThrows(IOException.class, () -> repository.update(pid, properties));
        assertEquals("users.properties must be left untouched", USERS, new String(Files.readAllBytes(users), UTF_8));
    }

    @Test
    public void storedCustomFilenameIsStillHonored() throws Exception {
        Path custom = Files.write(etc.resolve("custom-name.cfg"), "hello = world\n".getBytes(UTF_8));
        String pid = "my.legit.pid";
        TypedProperties properties = new TypedProperties();
        properties.put("hello", "karaf");
        repository(pid, custom, Capture.newInstance()).update(pid, properties);
        assertEquals("karaf", ConfigRepositoryImpl.load(custom.toFile()).get("hello"));
        assertFalse(Files.exists(etc.resolve(pid + ".cfg")));
    }

    @Test
    public void storedFilenameOutsideEtcIsStillHonored() throws Exception {
        // FileInstall also loads configurations from other watched directories, such as deploy
        Path deployed = Files.write(outside.resolve("my.deployed.pid.cfg"), "hello = world\n".getBytes(UTF_8));
        String pid = "my.deployed.pid";
        TypedProperties properties = new TypedProperties();
        properties.put("hello", "karaf");
        repository(pid, deployed, Capture.newInstance()).update(pid, properties);
        assertEquals("karaf", ConfigRepositoryImpl.load(deployed.toFile()).get("hello"));
    }

    @Test
    public void fileinstallFilenameIsNotPersistedOnUpdate() throws Exception {
        Path file = Files.write(etc.resolve("my.legit.pid.cfg"), "hello = world\n".getBytes(UTF_8));
        String pid = "my.legit.pid";
        // config:edit puts the stored felix.fileinstall.filename in the edited properties, and a
        // caller can change it
        TypedProperties properties = new TypedProperties();
        properties.put(FILEINSTALL_FILE_NAME, etc.resolve("users.properties").toUri().toString());
        properties.put("hello", "karaf");
        Capture<Dictionary<String, ?>> pushed = Capture.newInstance();
        repository(pid, file, pushed).update(pid, properties);
        TypedProperties written = ConfigRepositoryImpl.load(file.toFile());
        assertEquals("karaf", written.get("hello"));
        assertFalse("felix.fileinstall.filename must not be written to the file", written.containsKey(FILEINSTALL_FILE_NAME));
        assertEquals("ConfigAdmin must get the actual backing file",
                file.toFile().toURI().toString(), pushed.getValue().get(FILEINSTALL_FILE_NAME));
    }

    @Test
    public void fileinstallFilenameIsNotPersistedOnCreation() throws Exception {
        String pid = "my.new.pid";
        Path file = etc.resolve(pid + ".cfg");
        TypedProperties properties = new TypedProperties();
        properties.put(FILEINSTALL_FILE_NAME, file.toUri().toString());
        properties.put("hello", "world");
        repository(pid).update(pid, properties);
        assertFalse("felix.fileinstall.filename must not be written to the file",
                ConfigRepositoryImpl.load(file.toFile()).containsKey(FILEINSTALL_FILE_NAME));
    }

    @Test
    public void fileinstallFilenameIsNotPersistedOnFactoryCreation() throws Exception {
        ConfigurationAdmin admin = createMock(ConfigurationAdmin.class);
        Configuration config = createMock(Configuration.class);
        expect(admin.createFactoryConfiguration("my.factory", "?")).andReturn(config).anyTimes();
        expect(config.getPid()).andReturn("my.factory.generated").anyTimes();
        config.update(anyObject());
        expectLastCall().anyTimes();
        replay(admin, config);
        TypedProperties properties = new TypedProperties();
        properties.put(FILEINSTALL_FILE_NAME, etc.resolve("users.properties").toUri().toString());
        properties.put("hello", "world");
        new ConfigRepositoryImpl(admin).createFactoryConfiguration("my.factory", "alias", properties);
        assertFalse("felix.fileinstall.filename must not be written to the file",
                ConfigRepositoryImpl.load(etc.resolve("my.factory-alias.cfg").toFile()).containsKey(FILEINSTALL_FILE_NAME));
    }
}
