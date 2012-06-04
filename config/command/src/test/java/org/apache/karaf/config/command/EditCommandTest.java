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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Dictionary;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.config.command.ConfigCommandSupport;
import org.apache.karaf.config.command.EditCommand;
import org.apache.karaf.config.core.impl.ConfigRepositoryImpl;
import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test cases for {@link EditCommand}
 */
public class EditCommandTest extends TestCase {

    private static final String PID = "my.test.persistent.id";

    private EditCommand command;
    private BundleContext context;
    private ConfigurationAdmin admin;
    private CommandSession session;

    @Override
    protected void setUp() throws Exception {
        command = new EditCommand();

        
        context = EasyMock.createMock(BundleContext.class);
        command.setBundleContext(context);
        
        admin = createMock(ConfigurationAdmin.class);
        command.setConfigRepository(new ConfigRepositoryImpl(admin));
        expect(context.getBundle(0)).andReturn(null).anyTimes();

        replay(context);
        
        session = new MockCommandSession();
    }
    
    public void testExecuteOnExistingPid() throws Exception {        
        Configuration config = createMock(Configuration.class);
        expect(admin.getConfiguration(PID)).andReturn(config);
        replay(admin);
        
        // the ConfigAdmin service returns a Dictionary for an existing PID
        Dictionary props = new Properties();
        expect(config.getProperties()).andReturn(props);
        replay(config);
        
        command.pid = PID; 
        command.execute(session);
        
        // the PID and Dictionary should have been set on the session
        assertEquals("The PID should be set on the session",
                     PID, session.get(ConfigCommandSupport.PROPERTY_CONFIG_PID));
        assertSame("The Dictionary returned by the ConfigAdmin service should be set on the session",
                   props, session.get(ConfigCommandSupport.PROPERTY_CONFIG_PROPS));
    }
    
    @SuppressWarnings("rawtypes")
    public void testExecuteOnNewPid() throws Exception {        
        Configuration config = createMock(Configuration.class);
        expect(admin.getConfiguration(PID)).andReturn(config);
        replay(admin);
        
        // the ConfigAdmin service does not return a Dictionary for a new PID
        expect(config.getProperties()).andReturn(null);
        replay(config);
        
        command.pid = PID; 
        command.execute(session);

        // the PID and an empty Dictionary should have been set on the session        
        assertEquals("The PID should be set on the session",
                     PID, session.get(ConfigCommandSupport.PROPERTY_CONFIG_PID));
        Dictionary props = (Dictionary) session.get(ConfigCommandSupport.PROPERTY_CONFIG_PROPS);
        assertNotNull("Should have a Dictionary on the session", props);
        assertTrue("Should have an empty Dictionary on the session", props.isEmpty());
    }

}
