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
package org.apache.felix.karaf.shell.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.osgi.service.cm.Configuration;

/**
 * Test cases for {@link SetLogLevel}
 */
@SuppressWarnings("unchecked")
public class SetLogLevelTest extends TestCase {
    
    private static final String ROOT_LOGGER = "log4j.rootLogger";
    private static final String PACKAGE_LOGGER = "log4j.logger.org.apache.karaf.test";
    private static final PrintStream ORIGINAL_STDERR = System.err;
    
    private SetLogLevel command;
    private Hashtable properties;
    private ByteArrayOutputStream stderr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        properties = new Hashtable();
        stderr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(stderr));

        final Configuration configuration = EasyMock.createMock(Configuration.class);
        EasyMock.expect(configuration.getProperties()).andReturn(properties);
        configuration.update(properties);
        EasyMock.replay(configuration);
        
        command = new SetLogLevel() {
            @Override
            protected Configuration getConfiguration() throws IOException {
                return configuration;
            }
        };
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.setErr(ORIGINAL_STDERR);
    }
    
    public void testInvalidLogLevel() throws Exception {
        runCommand("log:set INVALID");
        assertTrue("Expected an error message on System.err",
                   stderr.toString().contains("level must be set to"));
    }
    
    public void testSetLogLevel() throws Exception {
        runCommand("log:set INFO org.apache.karaf.test");
        
        assertEquals("INFO", properties.get(PACKAGE_LOGGER));
    }
    
    public void testSetRootLogLevel() throws Exception {
        runCommand("log:set INFO");
        
        assertEquals("INFO", properties.get(ROOT_LOGGER));
    }
    
    public void testSetLogLevelLowerCase() throws Exception {
        runCommand("log:set info org.apache.karaf.test");
        
        assertEquals("INFO", properties.get(PACKAGE_LOGGER));
    }
    
    public void testSetRootLogLevelLowerCase() throws Exception {
        runCommand("log:set info");
        
        assertEquals("INFO", properties.get(ROOT_LOGGER));
    }
    
    public void testChangeLogLevel() throws Exception {
        properties.put(PACKAGE_LOGGER, "DEBUG");
        
        runCommand("log:set INFO org.apache.karaf.test");
        
        assertEquals("INFO", properties.get(PACKAGE_LOGGER));
    }
    
    public void testChangeRootLogLevel() throws Exception {
        properties.put(ROOT_LOGGER, "DEBUG");
        
        runCommand("log:set INFO");
        
        assertEquals("INFO", properties.get(ROOT_LOGGER));
    }
    
    public void testChangeLogLevelWithAppender() throws Exception {
        properties.put(PACKAGE_LOGGER, "DEBUG, APPENDER1");
        
        runCommand("log:set INFO org.apache.karaf.test");
        
        assertEquals("INFO, APPENDER1", properties.get(PACKAGE_LOGGER));
    }
    
    public void testChangeRootLogLevelWithAppender() throws Exception {
        properties.put(ROOT_LOGGER, "DEBUG, APPENDER1");
        
        runCommand("log:set INFO");
        
        assertEquals("INFO, APPENDER1", properties.get(ROOT_LOGGER));
    }
    
    
    public void testUnsetLogLevel() throws Exception {
        properties.put(PACKAGE_LOGGER, "DEBUG");

        runCommand("log:set DEFAULT org.apache.karaf.test");
        
        assertFalse("Configuration for logger org.apache.karaf.test has been removed", 
                    properties.containsKey(PACKAGE_LOGGER));
    }
    
    
    public void testUnsetRootLogLevel() throws Exception {
        properties.put(ROOT_LOGGER, "INFO");
        
        runCommand("log:set DEFAULT");
        
        assertEquals("Configuration for root logger should not be removed",
                     "INFO", properties.get(ROOT_LOGGER));
        assertTrue("Expected an error message on System.err",
                   stderr.toString().contains("Can not unset the ROOT logger"));
    }
    
    /*
     * Simulate running the log:set command
     */
    private void runCommand(String commandline) throws Exception {
        String[] parts = commandline.split(" ");

        command.level = parts[1];
        if (parts.length == 3) {
            command.logger = "org.apache.karaf.test";
        }
        
        command.doExecute();
    }
}
