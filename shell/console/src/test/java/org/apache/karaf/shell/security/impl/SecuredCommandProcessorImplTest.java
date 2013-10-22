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
package org.apache.karaf.shell.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;

import org.apache.felix.gogo.api.CommandSessionListener;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class SecuredCommandProcessorImplTest {
    @Test
    public void testCommandProcessor() throws Exception {
        ThreadIO tio = EasyMock.createMock(ThreadIO.class);
        EasyMock.replay(tio);

        @SuppressWarnings("unchecked")
        ServiceReference<ThreadIO> tioRef = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(tioRef);

        final BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.getServiceReference(ThreadIO.class)).andReturn(tioRef).anyTimes();
        EasyMock.expect(bc.getService(tioRef)).andReturn(tio).anyTimes();
        EasyMock.expect(bc.createFilter(EasyMock.isA(String.class))).andAnswer(new IAnswer<Filter>() {
            @Override
            public Filter answer() throws Throwable {
                return FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.expect(bc.getServiceReferences((String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(null).anyTimes();

        // Capture the listeners
        final Map<String, ServiceListener> listeners = new HashMap<String, ServiceListener>();

        // Here are the expected calls
        final String commandFilter = "(&(osgi.command.scope=*)(osgi.command.function=*)" +
                "(|(org.apache.karaf.service.guard.roles=aaabbbccc)(!(org.apache.karaf.service.guard.roles=*))))";
        expectServiceTracker(bc, commandFilter, listeners);
        expectServiceTracker(bc, "(objectClass=" + Converter.class.getName() + ")", listeners);
        expectServiceTracker(bc, "(objectClass=" + CommandSessionListener.class.getName() + ")", listeners);
        EasyMock.replay(bc);

        Subject subject = new Subject();
        subject.getPrincipals().add(new RolePrincipal("aaabbbccc"));

        Subject.doAs(subject, new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                MySecuredCommandProcessorImpl scp = new MySecuredCommandProcessorImpl(bc) {};

                assertEquals(3, scp.getCommands().size());
                assertTrue(scp.getCommands().contains("osgi:addcommand"));
                assertTrue(scp.getCommands().contains("osgi:removecommand"));
                assertTrue(scp.getCommands().contains("osgi:eval"));
                assertEquals(1, scp.getConstants().size());
                assertEquals(bc, scp.getConstants().get(".context"));

                // Now let's make a command appear...
                ServiceListener commandListener = listeners.get(commandFilter);

                ServiceReference<?> cdRef = EasyMock.createMock(ServiceReference.class);
                EasyMock.expect(cdRef.getProperty(CommandProcessor.COMMAND_SCOPE)).andReturn("foo");
                EasyMock.expect(cdRef.getProperty(CommandProcessor.COMMAND_FUNCTION)).andReturn("bar");
                EasyMock.replay(cdRef);

                ServiceEvent event = new ServiceEvent(ServiceEvent.REGISTERED, cdRef);
                commandListener.serviceChanged(event);
                assertEquals(4, scp.getCommands().size());
                assertTrue(scp.getCommands().contains("foo:bar"));

                ServiceReference<?> cd2Ref = EasyMock.createMock(ServiceReference.class);
                EasyMock.expect(cd2Ref.getProperty(CommandProcessor.COMMAND_SCOPE)).andReturn("xxx");
                EasyMock.expect(cd2Ref.getProperty(CommandProcessor.COMMAND_FUNCTION)).andReturn(
                        new String[] {"aaa", "bbb"});
                EasyMock.replay(cd2Ref);

                ServiceEvent event2 = new ServiceEvent(ServiceEvent.REGISTERED, cd2Ref);
                commandListener.serviceChanged(event2);
                assertEquals(6, scp.getCommands().size());
                assertTrue(scp.getCommands().contains("xxx:aaa"));
                assertTrue(scp.getCommands().contains("xxx:bbb"));

                return null;
            }
        });
    }

    void expectServiceTracker(final BundleContext bc, final String expectedFilter, final Map<String, ServiceListener> listeners) throws InvalidSyntaxException {
        bc.addServiceListener(EasyMock.isA(ServiceListener.class), EasyMock.eq(expectedFilter));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                listeners.put(expectedFilter, (ServiceListener) EasyMock.getCurrentArguments()[0]);
                return null;
            }
        }).once();
    }

    // Subclass to provide access to some protected members
    static class MySecuredCommandProcessorImpl extends SecuredCommandProcessorImpl {
        public MySecuredCommandProcessorImpl(BundleContext bc) {
            super(bc);
        }

        Map<String, Object> getConstants() {
            return constants;
        }
    };
}
