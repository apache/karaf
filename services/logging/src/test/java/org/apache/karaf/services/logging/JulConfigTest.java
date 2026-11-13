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
package org.apache.karaf.services.logging;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Hashtable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class JulConfigTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private JulConfig julConfig;
    private LogReaderServiceImpl logReaderService;

    @Before
    public void setUp() {
        logReaderService = new LogReaderServiceImpl();
        julConfig = new JulConfig();
        julConfig.init(logReaderService);
    }

    @After
    public void tearDown() {
        julConfig.close();
    }

    @Test
    public void testParseLevel() {
        assertEquals(Level.FINEST, JulConfig.parseLevel("TRACE", Level.INFO));
        assertEquals(Level.FINE, JulConfig.parseLevel("DEBUG", Level.INFO));
        assertEquals(Level.INFO, JulConfig.parseLevel("INFO", Level.WARNING));
        assertEquals(Level.WARNING, JulConfig.parseLevel("WARN", Level.INFO));
        assertEquals(Level.WARNING, JulConfig.parseLevel("WARNING", Level.INFO));
        assertEquals(Level.SEVERE, JulConfig.parseLevel("ERROR", Level.INFO));
        assertEquals(Level.OFF, JulConfig.parseLevel("OFF", Level.INFO));
        assertEquals(Level.INFO, JulConfig.parseLevel("UNKNOWN", Level.INFO));
    }

    @Test
    public void testParseSize() {
        assertEquals(10L * 1024 * 1024, JulConfig.parseSize("10MB", 1));
        assertEquals(10L * 1024 * 1024, JulConfig.parseSize("10M", 1));
        assertEquals(512L * 1024, JulConfig.parseSize("512kb", 1));
        assertEquals(2L * 1024 * 1024 * 1024, JulConfig.parseSize("2G", 1));
        assertEquals(1000L, JulConfig.parseSize("1000", 1));
        assertEquals(1L, JulConfig.parseSize("invalid", 1));
    }

    @Test
    public void testSubstitute() {
        System.setProperty("test.karaf.logging.prop", "value");
        try {
            assertEquals("value/karaf.log", JulConfig.substitute("${test.karaf.logging.prop}/karaf.log"));
            assertEquals("a-value-b", JulConfig.substitute("a-${test.karaf.logging.prop}-b"));
            assertEquals("OFF", JulConfig.substitute("${test.karaf.logging.missing:-OFF}"));
            assertEquals("${test.karaf.logging.missing}", JulConfig.substitute("${test.karaf.logging.missing}"));
        } finally {
            System.clearProperty("test.karaf.logging.prop");
        }
    }

    @Test
    public void testDynamicUpdate() {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("level", "DEBUG");
        props.put("logger.org.apache.karaf.test.level", "WARN");

        julConfig.update(props);

        assertEquals(Level.FINE, Logger.getLogger("").getLevel());
        assertEquals(Level.WARNING, Logger.getLogger("org.apache.karaf.test").getLevel());
    }

    @Test
    public void testConsoleDisabledByDefault() {
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            assertFalse(handler instanceof ConsoleHandler);
        }
    }

    @Test
    public void testFileHandlerHonoursLoggerLevels() throws Exception {
        File logFile = new File(tmp.getRoot(), "karaf.log");
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("level", "INFO");
        props.put("file", logFile.getPath());
        props.put("logger.org.apache.karaf.test.debug", "DEBUG");
        julConfig.update(props);

        Logger.getLogger("org.apache.karaf.test.debug.child").fine("debug message");
        Logger.getLogger("org.apache.karaf.test.other").fine("filtered message");

        String content = read(logFile);
        assertTrue(content.contains("| DEBUG |"));
        assertTrue(content.contains("debug message"));
        assertFalse(content.contains("filtered message"));
    }

    @Test
    public void testConfiguredLoggerLevelSurvivesGc() {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("logger.org.apache.karaf.test.gc", "WARN");
        julConfig.update(props);

        for (int i = 0; i < 5; i++) {
            System.gc();
        }

        assertEquals(Level.WARNING, Logger.getLogger("org.apache.karaf.test.gc").getLevel());
    }

    @Test
    public void testRemovedLoggerLevelIsReset() {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("logger.org.apache.karaf.test.removed", "WARN");
        julConfig.update(props);
        Logger logger = Logger.getLogger("org.apache.karaf.test.removed");
        assertEquals(Level.WARNING, logger.getLevel());

        julConfig.update(new Hashtable<>());

        assertNull(logger.getLevel());
    }

    @Test
    public void testInitInterpolatesConfigurationFile() throws Exception {
        julConfig.close();
        File etc = tmp.newFolder("etc");
        File logDir = tmp.newFolder("log");
        Files.write(new File(etc, Activator.CONFIGURATION_PID + ".cfg").toPath(),
                "level = WARN\nfile = ${test.karaf.logging.dir}/custom.log\n".getBytes(StandardCharsets.UTF_8));
        System.setProperty("karaf.etc", etc.getPath());
        System.setProperty("test.karaf.logging.dir", logDir.getPath());
        try {
            julConfig = new JulConfig();
            julConfig.init(logReaderService);
        } finally {
            System.clearProperty("karaf.etc");
            System.clearProperty("test.karaf.logging.dir");
        }

        Logger.getLogger("org.apache.karaf.test.interpolation").warning("interpolated");

        assertEquals(Level.WARNING, Logger.getLogger("").getLevel());
        assertTrue(read(new File(logDir, "custom.log")).contains("interpolated"));
    }

    @Test
    public void testCloseRestoresRootLevel() {
        julConfig.close();
        Level original = Logger.getLogger("").getLevel();

        julConfig = new JulConfig();
        julConfig.init(logReaderService);
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("level", "DEBUG");
        julConfig.update(props);
        julConfig.close();

        assertEquals(original, Logger.getLogger("").getLevel());
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
