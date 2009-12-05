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
package org.apache.felix.karaf.shell.dev.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.felix.karaf.shell.dev.util.IO;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for {@link org.apache.felix.karaf.shell.dev.framework.Equinox}
 */
public class EquinoxTest {

    private File base;
    private File etc;

    private Equinox equinox;

    @Before
    public void setUp() {
        // creating a dummy karaf instance folder
        base = new File("target/instances/" + System.currentTimeMillis());
        base.mkdirs();

        // make sure the etc directory exists
        etc = new File(base, "etc");
        etc.mkdirs();

        equinox = new Equinox(base);
    }

    @Test
    public void enableDebug() throws IOException {
        IO.copyTextToFile(
                EquinoxTest.class.getResourceAsStream("config.properties"),
                new File(etc, "config.properties"));

        equinox.enableDebug(base);

        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(base, "etc/config.properties")));
        assertNotNull(properties.get("osgi.debug"));
        assertEquals("etc/equinox-debug.properties", properties.get("osgi.debug"));

        assertTrue("Should have created the default Equinox debug config file",
                   new File(etc, "equinox-debug.properties").exists());
    }

    @Test
    public void testDisableDebug() throws IOException {
        IO.copyTextToFile(
                EquinoxTest.class.getResourceAsStream("enabled-config.properties"),
                new File(etc, "config.properties"));

        equinox.disableDebug(base);

        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(etc, "config.properties")));
        assertFalse("osgi.debug property should have been removed from the file",
                    properties.containsKey("osgi.debug"));
    }
}
