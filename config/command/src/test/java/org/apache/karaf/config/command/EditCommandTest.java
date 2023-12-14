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
package org.apache.karaf.config.command;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;
import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.config.core.impl.ConfigRepositoryImpl;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

/**
 * Test cases for {@link EditCommand}
 */
public class EditCommandTest extends TestCase {

    private static final String PID = "my.test.persistent.id";

    private EditCommand command;
    private ConfigurationAdmin admin;
    private Session session;

    @Override
    protected void setUp() throws Exception {
        command = new EditCommand();

        admin = createMock(ConfigurationAdmin.class);
        command.setConfigRepository(new ConfigRepositoryImpl(admin));

        session = new MockCommandSession();
        command.setSession(session);
    }
    
    public void testExecuteOnExistingPid() throws Exception {        
        Configuration config = createMock(Configuration.class);
        expect(admin.getConfiguration(PID, null)).andReturn(config);
        replay(admin);
        
        // the ConfigAdmin service returns a Dictionary for an existing PID
        Dictionary<String, Object> props = new Hashtable<>();
        expect(config.getProcessedProperties(null)).andReturn(props);
        replay(config);
        
        command.pid = PID; 
        command.execute();
        
        // the PID and Dictionary should have been set on the session
        assertEquals("The PID should be set on the session",
                     PID, session.get(ConfigCommandSupport.PROPERTY_CONFIG_PID));
        assertEquals("The Dictionary returned by the ConfigAdmin service should be set on the session",
                   props, session.get(ConfigCommandSupport.PROPERTY_CONFIG_PROPS));
    }
    
    @SuppressWarnings("rawtypes")
    public void testExecuteOnNewPid() throws Exception {        
        Configuration config = createMock(Configuration.class);
        expect(admin.getConfiguration(PID, null)).andReturn(config);
        replay(admin);
        
        // the ConfigAdmin service does not return a Dictionary for a new PID
        expect(config.getProcessedProperties(null)).andReturn(null);
        replay(config);
        
        command.pid = PID; 
        command.execute();

        // the PID and an empty Dictionary should have been set on the session        
        assertEquals("The PID should be set on the session",
                     PID, session.get(ConfigCommandSupport.PROPERTY_CONFIG_PID));
        TypedProperties props = (TypedProperties) session.get(ConfigCommandSupport.PROPERTY_CONFIG_PROPS);
        assertNotNull("Should have a Dictionary on the session", props);
        assertTrue("Should have an empty Dictionary on the session", props.isEmpty());
    }

}
