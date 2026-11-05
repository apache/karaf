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

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JulConfigTest {

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
    public void testDynamicUpdate() {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("level", "DEBUG");
        props.put("logger.org.apache.karaf.test.level", "WARN");

        julConfig.update(props);

        assertEquals(Level.FINE, Logger.getLogger("").getLevel());
        assertEquals(Level.WARNING, Logger.getLogger("org.apache.karaf.test").getLevel());
    }
}
