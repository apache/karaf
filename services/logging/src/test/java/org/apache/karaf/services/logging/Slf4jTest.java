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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.log.LogEntry;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import static org.junit.Assert.*;

public class Slf4jTest {

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
    public void testSlf4jLogging() {
        List<LogEntry> entries = new ArrayList<>();
        logReaderService.addLogListener(entries::add);

        org.slf4j.Logger logger = LoggerFactory.getLogger(Slf4jTest.class);
        logger.info("Hello SLF4J: {}", "karaf-logging");

        boolean found = entries.stream()
                .anyMatch(e -> e.getMessage() != null && e.getMessage().contains("Hello SLF4J: karaf-logging"));
        assertTrue("SLF4J message should be forwarded through JUL to LogReaderService", found);
    }

    @Test
    public void testStaticLoggerBinder() {
        StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
        assertNotNull(binder);
        assertNotNull(binder.getLoggerFactory());
        org.slf4j.Logger logger = binder.getLoggerFactory().getLogger("static.test");
        assertNotNull(logger);
    }

    @Test
    public void testCommonsLogging() {
        List<LogEntry> entries = new ArrayList<>();
        logReaderService.addLogListener(entries::add);

        org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog("commons.test");
        log.info("Hello Commons Logging");

        boolean found = entries.stream()
                .anyMatch(e -> e.getMessage() != null && e.getMessage().contains("Hello Commons Logging"));
        assertTrue("Commons Logging message should be forwarded through SLF4J to JUL to LogReaderService", found);
    }
}
