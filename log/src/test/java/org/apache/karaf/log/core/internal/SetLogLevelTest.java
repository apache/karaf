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
package org.apache.karaf.log.core.internal;

import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.karaf.log.core.LogMBean;
import org.apache.karaf.log.core.LogService;
import org.easymock.EasyMock;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test cases for {@link org.apache.karaf.log.command.SetLogLevel}
 */
@SuppressWarnings("unchecked")
public class SetLogLevelTest extends TestCase {
    
    private static final String ROOT_LOGGER = "log4j.rootLogger";
    private static final String PACKAGE_LOGGER = "log4j.logger.org.apache.karaf.test";
    
    private LogService logService;
    private LogMBean logMBean;
    @SuppressWarnings("rawtypes")
    private Hashtable properties;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        properties = new Hashtable<String, String>();
        properties.put(ROOT_LOGGER, "info");

        final Configuration configuration = EasyMock.createMock(Configuration.class);
        EasyMock.expect(configuration.getProcessedProperties(null)).andReturn(properties);
        configuration.update(properties);        
        ConfigurationAdmin configAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(configAdmin.getConfiguration(LogServiceImpl.CONFIGURATION_PID, null)).andReturn(configuration);
        logService = new LogServiceImpl(configAdmin, 100);
        logMBean = new LogMBeanImpl(logService);
        EasyMock.replay(configAdmin);
        EasyMock.replay(configuration);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testInvalidLogLevel() throws Exception {
        try {
            logMBean.setLevel("INVALID");
            fail("Exception expected");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }
    
    public void testSetLogLevel() throws Exception {
        logMBean.setLevel("org.apache.karaf.test", "INFO");
        assertEquals("INFO", properties.get(PACKAGE_LOGGER));
    }
    
    public void testSetRootLogLevel() throws Exception {
        logMBean.setLevel("INFO");
        assertEquals("INFO", properties.get(ROOT_LOGGER));
    }
    
    public void testSetLogLevelLowerCase() throws Exception {
        logMBean.setLevel("org.apache.karaf.test", "info");
        assertEquals("INFO", properties.get(PACKAGE_LOGGER));
    }
    
    public void testSetRootLogLevelLowerCase() throws Exception {
        logMBean.setLevel("info");
        assertEquals("INFO", properties.get(ROOT_LOGGER));
    }
    
    public void testChangeLogLevel() throws Exception {
        properties.put(PACKAGE_LOGGER, "DEBUG");
        logMBean.setLevel("org.apache.karaf.test", "INFO");
        assertEquals("INFO", properties.get(PACKAGE_LOGGER));
    }
    
    public void testChangeRootLogLevel() throws Exception {
        properties.put(ROOT_LOGGER, "DEBUG");
        logMBean.setLevel("INFO");
        assertEquals("INFO", properties.get(ROOT_LOGGER));
    }
    
    public void testChangeLogLevelWithAppender() throws Exception {
        properties.put(PACKAGE_LOGGER, "DEBUG, APPENDER1");
        logMBean.setLevel("org.apache.karaf.test", "INFO");
        assertEquals("INFO, APPENDER1", properties.get(PACKAGE_LOGGER));
    }
    
    public void testChangeRootLogLevelWithAppender() throws Exception {
        properties.put(ROOT_LOGGER, "DEBUG, APPENDER1");
        logMBean.setLevel("INFO");
        assertEquals("INFO, APPENDER1", properties.get(ROOT_LOGGER));
    }

    public void testUnsetLogLevel() throws Exception {
        properties.put(PACKAGE_LOGGER, "DEBUG");
        logMBean.setLevel("org.apache.karaf.test", "DEFAULT");
        assertFalse("Configuration for logger org.apache.karaf.test has been removed", properties.containsKey(PACKAGE_LOGGER));
    }

    public void testUnsetRootLogLevel() throws Exception {
        properties.put(ROOT_LOGGER, "INFO");
        logMBean.setLevel("org.apache.karaf.test", "DEFAULT");
        assertEquals("Configuration for root logger should not be removed", "INFO", properties.get(ROOT_LOGGER));
    }
    
}
