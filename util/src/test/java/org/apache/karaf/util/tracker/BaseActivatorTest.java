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
package org.apache.karaf.util.tracker;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class BaseActivatorTest {

    private final Map<ServiceReference<?>, Runnable> services = new ConcurrentHashMap<>();
    private volatile ServiceReference<?> registered;

    private BundleContext bundleContext;
    private Capture<ServiceListener> listener;
    private TestActivator activator;

    @Before
    public void setUp() throws Exception {
        Bundle bundle = niceMock(Bundle.class);
        bundleContext = niceMock(BundleContext.class);
        listener = newCapture();
        expect(bundleContext.getBundle()).andStubReturn(bundle);
        bundleContext.addServiceListener(capture(listener), anyString());
        expectLastCall().asStub();
        expect(bundleContext.getServiceReferences(eq(Runnable.class.getName()), anyObject()))
                .andStubAnswer(() -> registered != null ? new ServiceReference<?>[] { registered } : null);
        expect(bundleContext.getService(anyObject()))
                .andStubAnswer(() -> services.get(getCurrentArguments()[0]));
        replay(bundle, bundleContext);

        activator = new TestActivator();
    }

    @After
    public void tearDown() throws Exception {
        activator.stop(bundleContext);
    }

    @Test
    public void startsOnceWhenServiceShowsUpWhileStarting() throws Exception {
        // the tracked service is registered while the activator is processing a previous change
        ServiceReference<?> reference = reference();
        activator.onStop = () -> register(reference);

        activator.start(bundleContext);
        activator.await();

        assertEquals(Collections.singletonList(reference), activator.starts);
    }

    @Test
    public void startsWhenServiceShowsUpLater() throws Exception {
        ServiceReference<?> reference = reference();

        activator.start(bundleContext);
        activator.await();
        assertEquals(Collections.emptyList(), activator.starts);

        register(reference);
        activator.await();

        assertEquals(Collections.singletonList(reference), activator.starts);
    }

    @Test
    public void restartsWhenServiceIsReplaced() throws Exception {
        ServiceReference<?> first = reference();
        ServiceReference<?> second = reference();
        registered = first;

        activator.start(bundleContext);
        activator.await();
        assertEquals(Collections.singletonList(first), activator.starts);

        registered = second;
        listener.getValue().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, first));
        activator.await();

        assertEquals(Arrays.asList(first, second), activator.starts);
    }

    @Test
    public void restartsWhenConfigurationIsUpdated() throws Exception {
        ServiceReference<?> reference = reference();
        registered = reference;

        activator.start(bundleContext);
        activator.await();
        assertEquals(Collections.singletonList(reference), activator.starts);

        activator.updated(new Hashtable<>());
        activator.await();

        assertEquals(Arrays.asList(reference, reference), activator.starts);
    }

    private ServiceReference<?> reference() {
        ServiceReference<?> reference = niceMock(ServiceReference.class);
        replay(reference);
        services.put(reference, () -> { });
        return reference;
    }

    private void register(ServiceReference<?> reference) {
        registered = reference;
        listener.getValue().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
    }

    /**
     * Tracks a {@link Runnable} service and records the references it has been started with.
     */
    private static class TestActivator extends BaseActivator {

        final List<ServiceReference<?>> starts = Collections.synchronizedList(new ArrayList<>());
        volatile Runnable onStop;

        @Override
        protected void doOpen() throws Exception {
            trackService(Runnable.class);
        }

        @Override
        protected void doStart() throws Exception {
            if (getTrackedService(Runnable.class) != null) {
                starts.add(getTrackedServiceRef(Runnable.class));
            }
        }

        @Override
        protected void doStop() {
            Runnable action = onStop;
            onStop = null;
            if (action != null) {
                action.run();
            }
            super.doStop();
        }

        /**
         * Waits for the changes notified so far to be processed.
         */
        void await() throws Exception {
            // a change notified while another one is processed is queued behind the first marker
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> { }).get(10, TimeUnit.SECONDS);
            }
        }
    }

}
