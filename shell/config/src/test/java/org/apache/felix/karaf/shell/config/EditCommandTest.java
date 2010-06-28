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
package org.apache.felix.karaf.shell.config;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.command.CommandSession;

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
        
        ServiceReference reference = createMock(ServiceReference.class);
        expect(context.getServiceReference(ConfigurationAdmin.class.getName())).andReturn(reference);
        
        admin = createMock(ConfigurationAdmin.class);
        expect(context.getService(reference)).andReturn(admin);
        expect(context.ungetService(reference)).andReturn(Boolean.TRUE);
        
        replay(context);
        
        session = new MockCommandSession();
    }
    
    public void testExecuteOnExistingPid() throws Exception {        
        Configuration config = createMock(Configuration.class);
        expect(admin.getConfiguration(PID)).andReturn(config);
        replay(admin);
        
        // the ConfigAdmin service returns a Dictionary for an existing PID
        Properties props = new Properties();
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
    
    @SuppressWarnings("unchecked")
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
    
    /*
     * A mock CommandSession implementation that only cares about the properties set on the session
     */
    private class MockCommandSession implements CommandSession {
        
        private Map<String, Object> properties = new HashMap<String, Object>();

        public void close() {
            // not implemented
        }

        public Object convert(Class<?> type, Object instance) {
            // not implemented
            return null;
        }

        public Object execute(CharSequence commandline) throws Exception {
            // not implemented
            return null;
        }

        public CharSequence format(Object target, int level) {
            // not implemented
            return null;
        }

        public Object get(String name) {
            return properties.get(name);
        }

        public PrintStream getConsole() {
            // not implemented
            return null;
        }

        public InputStream getKeyboard() {
            // not implemented
            return null;
        }

        public void put(String name, Object value) {
            properties.put(name, value);
        }
    }
}
