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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.utils.properties.TypedProperties;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Verifies that {@link ConfigRepositoryImpl} does not lose properties when FileInstall, or the JSON installer,
 * writes a configuration back to its file at the same time: the file is truncated first, so it is empty for a moment.
 */
public class ConfigRepositoryImplWriteBackTest {

    private static final String PID = "my.pid";
    private static final String FILEINSTALL_FILE_NAME = "felix.fileinstall.filename";

    private Path etc;
    private String previousEtc;

    @Before
    public void setUp() throws Exception {
        etc = Files.createDirectories(Files.createTempDirectory("karaf-config-write-back").resolve("etc"));
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

    /**
     * A repository holding a configuration backed by the given file, with the given stored properties. The processed
     * properties are the stored ones, with {@code test2} overridden as {@link KarafConfigurationPlugin} does from an
     * environment variable. The properties pushed to ConfigAdmin are captured.
     */
    private ConfigRepositoryImpl repository(Path file, Hashtable<String, Object> stored, Capture<Dictionary<String, ?>> pushed) throws Exception {
        stored.put(Constants.SERVICE_PID, PID);
        stored.put(FILEINSTALL_FILE_NAME, file.toUri().toString());
        Hashtable<String, Object> processed = new Hashtable<>(stored);
        processed.put("test2", "fromEnvironment");
        ConfigurationAdmin admin = createMock(ConfigurationAdmin.class);
        Configuration config = createMock(Configuration.class);
        expect(admin.getConfiguration(PID, "?")).andReturn(config).anyTimes();
        expect(admin.getConfiguration(PID, null)).andReturn(config).anyTimes();
        expect(config.getProcessedProperties(anyObject())).andReturn(processed).anyTimes();
        expect(config.getProperties()).andReturn(stored).anyTimes();
        expect(config.getPid()).andReturn(PID).anyTimes();
        expect(config.getFactoryPid()).andReturn(null).anyTimes();
        config.update(capture(pushed));
        expectLastCall().anyTimes();
        replay(admin, config);
        return new ConfigRepositoryImpl(admin) {
            @Override
            void store(TypedProperties properties, File file) throws IOException {
                super.store(properties, file);
                // FileInstall starts to write the configuration back for a previous update
                Files.write(file.toPath(), new byte[0]);
            }
        };
    }

    private static Hashtable<String, Object> stored() {
        Hashtable<String, Object> stored = new Hashtable<>();
        stored.put("test1", "data1");
        stored.put("test2", "data2");
        return stored;
    }

    @Test
    public void editingATruncatedFileKeepsTheConfigurationProperties() throws Exception {
        Path file = Files.write(etc.resolve(PID + ".cfg"), new byte[0]);
        TypedProperties props = repository(file, stored(), Capture.newInstance()).getConfig(PID);
        assertEquals("data1", props.get("test1"));
        assertEquals("the stored value, not the one overridden by the environment", "data2", props.get("test2"));
        assertEquals(file.toUri().toString(), props.get(FILEINSTALL_FILE_NAME));
        assertFalse(props.containsKey(Constants.SERVICE_PID));
    }

    @Test
    public void editingATruncatedJsonFileKeepsTheConfigurationProperties() throws Exception {
        Path file = Files.write(etc.resolve(PID + "." + JsonConfigInstaller.getExtension()), new byte[0]);
        TypedProperties props = repository(file, stored(), Capture.newInstance()).getConfig(PID);
        assertEquals("data1", props.get("test1"));
        assertEquals("data2", props.get("test2"));
    }

    @Test
    public void editingAnIncompleteFileOnlyTakesTheMissingProperties() throws Exception {
        Path file = Files.write(etc.resolve(PID + ".cfg"), "test1 = ${karaf.etc}/data\n".getBytes(UTF_8));
        TypedProperties props = repository(file, stored(), Capture.newInstance()).getConfig(PID);
        assertEquals(etc.toFile().getCanonicalPath() + "/data", props.get("test1"));
        assertEquals("data2", props.get("test2"));
    }

    @Test
    public void updatePushesWhatWasStoredWhileTheFileIsWrittenBack() throws Exception {
        Path file = Files.write(etc.resolve(PID + ".cfg"), "test1 = data1\ntest2 = data2\n".getBytes(UTF_8));
        Capture<Dictionary<String, ?>> pushed = Capture.newInstance();
        TypedProperties properties = new TypedProperties();
        properties.put("test1", "${karaf.etc}/data");
        properties.put("test2", "data2");
        repository(file, stored(), pushed).update(PID, properties);
        assertEquals("placeholders are substituted as when reading the file",
                etc.toFile().getCanonicalPath() + "/data", pushed.getValue().get("test1"));
        assertEquals("data2", pushed.getValue().get("test2"));
        assertEquals(file.toFile().toURI().toString(), pushed.getValue().get(FILEINSTALL_FILE_NAME));
    }

    @Test
    public void updatePushesWhatWasStoredWhileTheJsonFileIsWrittenBack() throws Exception {
        Path file = Files.write(etc.resolve(PID + "." + JsonConfigInstaller.getExtension()),
                "{ \"test1\": \"data1\", \"test2\": \"data2\" }".getBytes(UTF_8));
        Capture<Dictionary<String, ?>> pushed = Capture.newInstance();
        TypedProperties properties = new TypedProperties();
        properties.put("test1", "data1new");
        properties.put("test2", "data2");
        repository(file, stored(), pushed).update(PID, properties);
        assertEquals("data1new", pushed.getValue().get("test1"));
        assertEquals("data2", pushed.getValue().get("test2"));
        assertEquals(file.toFile().toURI().toString(), pushed.getValue().get(FILEINSTALL_FILE_NAME));
    }
}
