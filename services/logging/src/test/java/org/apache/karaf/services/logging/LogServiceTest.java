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
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;

import static org.junit.Assert.*;

public class LogServiceTest {

    private JulConfig julConfig;
    private LogReaderServiceImpl logReaderService;
    private LogServiceImpl logService;

    @Before
    public void setUp() {
        logReaderService = new LogReaderServiceImpl();
        julConfig = new JulConfig();
        julConfig.init(logReaderService);
        logService = new LogServiceImpl(null);
    }

    @After
    public void tearDown() {
        julConfig.close();
    }

    @Test
    public void testLogServiceLevels() {
        List<LogEntry> received = new ArrayList<>();
        LogListener listener = received::add;
        logReaderService.addLogListener(listener);

        logService.log(LogService.LOG_INFO, "Info message");
        logService.log(LogService.LOG_WARNING, "Warning message");
        logService.log(LogService.LOG_ERROR, "Error message", new RuntimeException("Test error"));

        assertTrue(received.size() >= 3);

        boolean foundInfo = false;
        boolean foundWarn = false;
        boolean foundError = false;

        for (LogEntry entry : received) {
            if ("Info message".equals(entry.getMessage())) {
                assertEquals(LogService.LOG_INFO, entry.getLevel());
                assertEquals(LogLevel.INFO, entry.getLogLevel());
                foundInfo = true;
            } else if ("Warning message".equals(entry.getMessage())) {
                assertEquals(LogService.LOG_WARNING, entry.getLevel());
                assertEquals(LogLevel.WARN, entry.getLogLevel());
                foundWarn = true;
            } else if ("Error message".equals(entry.getMessage())) {
                assertEquals(LogService.LOG_ERROR, entry.getLevel());
                assertEquals(LogLevel.ERROR, entry.getLogLevel());
                assertNotNull(entry.getException());
                assertEquals("Test error", entry.getException().getMessage());
                foundError = true;
            }
        }

        assertTrue(foundInfo);
        assertTrue(foundWarn);
        assertTrue(foundError);

        logReaderService.removeLogListener(listener);
    }

    @Test
    public void testOsgiLoggerWithParameters() {
        List<LogEntry> received = new ArrayList<>();
        logReaderService.addLogListener(received::add);

        Logger logger = logService.getLogger("test.logger");
        assertTrue(logger.isInfoEnabled());

        logger.info("Hello {}, number {}", "world", 42);

        boolean found = received.stream()
                .anyMatch(e -> "Hello world, number 42".equals(e.getMessage()));
        assertTrue(found);
    }

    @Test
    public void testOsgiFormatterLogger() {
        List<LogEntry> received = new ArrayList<>();
        logReaderService.addLogListener(received::add);

        FormatterLogger logger = logService.getLogger("test.formatter", FormatterLogger.class);
        logger.info("Values: %s and %d", "stringVal", 100);

        boolean found = received.stream()
                .anyMatch(e -> "Values: stringVal and 100".equals(e.getMessage()));
        assertTrue(found);
    }

    @Test
    public void testLoggerConsumer() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        Logger logger = logService.getLogger("test.consumer");
        logger.info(l -> {
            called.set(true);
            l.info("Consumer message");
        });
        assertTrue(called.get());
    }

    @Test
    public void testLogReaderGetLog() {
        logService.log(LogService.LOG_INFO, "Message 1");
        logService.log(LogService.LOG_INFO, "Message 2");

        Enumeration<LogEntry> logEnum = logReaderService.getLog();
        List<LogEntry> list = new ArrayList<>();
        while (logEnum.hasMoreElements()) {
            list.add(logEnum.nextElement());
        }

        assertTrue(list.size() >= 2);
    }
}
