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
 * Test cases for {@link org.apache.felix.karaf.shell.dev.framework.Felix}
 */
public class FelixTest {

    private File base;
    private File etc;
    
    private Felix felix;

    @Before
    public void setUp() {
        // creating a dummy karaf instance folder
        base = new File("target/instances/" + System.currentTimeMillis());
        base.mkdirs();

        // make sure the etc directory exists
        etc = new File(base, "etc");
        etc.mkdirs();

        felix = new Felix(base);
    }

    @Test
    public void enableDebug() throws IOException {
        IO.copyTextToFile(
                FelixTest.class.getResourceAsStream("config.properties"),
                new File(etc, "config.properties"));

        felix.enableDebug(base);

        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(etc, "config.properties")));
        assertNotNull(properties.get("felix.log.level"));
        assertEquals("4", properties.get("felix.log.level"));
    }

    @Test
    public void testDisableDebug() throws IOException {
        IO.copyTextToFile(
                FelixTest.class.getResourceAsStream("enabled-config.properties"),
                new File(etc, "config.properties"));

        felix.disableDebug(base);

        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(etc, "config.properties")));
        assertFalse(properties.containsKey("felix.log.level"));
    }
}
