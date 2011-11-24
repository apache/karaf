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
package org.apache.karaf.shell.config;


import java.util.Properties;

import junit.framework.TestCase;
import org.apache.felix.service.command.CommandSession;
import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

/**
 * Test cases for {@link EditCommand}
 */
public class UpdateCommandTest extends TestCase {

    private static final String PID = "my.test.persistent.id-other";

    private UpdateCommand command;
    private BundleContext context;
    private ConfigurationAdmin admin;
    private CommandSession session;

    @Override
    protected void setUp() throws Exception {
        command = new UpdateCommand();

        context = EasyMock.createMock(BundleContext.class);
        command.setBundleContext(context);

        ServiceReference reference = createMock(ServiceReference.class);
        expect(context.getServiceReference(ConfigurationAdmin.class.getName())).andReturn(reference).anyTimes();

        admin = createMock(ConfigurationAdmin.class);
        expect(context.getService(reference)).andReturn(admin);
        expect(context.ungetService(reference)).andReturn(Boolean.TRUE);

        replay(context);

        session = new MockCommandSession();
    }

    public void testupdateOnNewFactoryPid() throws Exception {
        Properties props = new Properties();

        session.put(ConfigCommandSupport.PROPERTY_CONFIG_PID, PID);
        session.put(ConfigCommandSupport.PROPERTY_CONFIG_PROPS, props);

        Configuration configNew = createMock(Configuration.class);
        expect(admin.getConfiguration(PID, null)).andReturn(configNew);
        expect(configNew.getProperties()).andReturn(null);


        Configuration configFac = createMock(Configuration.class);
        expect(admin.createFactoryConfiguration(PID.substring(0, PID.indexOf('-')), null)).andReturn(configFac);
        configFac.update(props);
        expect(configFac.getBundleLocation()).andReturn(null);
        replay(admin);
        replay(configNew);
        replay(configFac);

        command.execute(session);

    }

}
