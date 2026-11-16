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
package org.apache.karaf.services.web.internal.whiteboard;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

/**
 * Bundles registering whiteboard services, the entries of the bundles being the resources of the test class path.
 */
final class TestBundles {

    private final Map<ServiceReference<?>, Function<Bundle, Object>> services = new HashMap<>();
    private final Map<Bundle, AtomicReference<BundleContext>> contexts = new HashMap<>();
    private long serviceId = 100;

    Bundle bundle(long id) {
        Bundle bundle = niceMock(Bundle.class);
        AtomicReference<BundleContext> context = new AtomicReference<>();
        ClassLoader classLoader = getClass().getClassLoader();
        expect(bundle.getBundleId()).andStubReturn(id);
        expect(bundle.getState()).andStubReturn(Bundle.ACTIVE);
        expect(bundle.getBundleContext()).andStubAnswer(context::get);
        expect(bundle.getEntry(anyString())).andStubAnswer(
                () -> classLoader.getResource((String) getCurrentArguments()[0]));
        replay(bundle);
        contexts.put(bundle, context);
        restart(bundle);
        return bundle;
    }

    /**
     * Gives the bundle a new bundle context, as when it is stopped and started again.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    void restart(Bundle bundle) {
        BundleContext context = niceMock(BundleContext.class);
        expect(context.getBundle()).andStubReturn(bundle);
        expect(context.getServiceObjects(anyObject())).andStubAnswer(() -> {
            Function<Bundle, Object> factory = services.get((ServiceReference<?>) getCurrentArguments()[0]);
            return factory == null ? null : (ServiceObjects) serviceObjects(factory.apply(bundle));
        });
        replay(context);
        contexts.get(bundle).set(context);
    }

    /**
     * @param factory the service object got by each bundle.
     */
    @SuppressWarnings("unchecked")
    WhiteboardService register(WhiteboardService.Kind kind, Bundle bundle, Function<Bundle, Object> factory,
                               Object... properties) {
        Map<String, Object> values = new HashMap<>();
        values.put(Constants.SERVICE_ID, serviceId++);
        for (int i = 0; i < properties.length; i += 2) {
            values.put((String) properties[i], properties[i + 1]);
        }
        ServiceReference<Object> reference = niceMock(ServiceReference.class);
        expect(reference.getProperty(anyString())).andStubAnswer(() -> values.get(getCurrentArguments()[0]));
        expect(reference.getPropertyKeys()).andStubReturn(values.keySet().toArray(new String[0]));
        expect(reference.getBundle()).andStubReturn(bundle);
        expect(reference.compareTo(anyObject())).andStubAnswer(
                () -> compare(values, (ServiceReference<?>) getCurrentArguments()[0]));
        replay(reference);
        services.put(reference, factory);
        return new WhiteboardService(kind, reference);
    }

    /**
     * Compares service references as the framework does: the highest ranking, then the lowest service id, is greater.
     */
    private static int compare(Map<String, Object> values, ServiceReference<?> other) {
        int ranking = Integer.compare(ranking(values.get(Constants.SERVICE_RANKING)),
                ranking(other.getProperty(Constants.SERVICE_RANKING)));
        if (ranking != 0) {
            return ranking;
        }
        return Long.compare((Long) other.getProperty(Constants.SERVICE_ID), (Long) values.get(Constants.SERVICE_ID));
    }

    private static int ranking(Object ranking) {
        return ranking instanceof Integer ? (Integer) ranking : 0;
    }

    private static ServiceObjects<Object> serviceObjects(Object instance) {
        return new ServiceObjects<Object>() {
            @Override
            public Object getService() {
                return instance;
            }

            @Override
            public void ungetService(Object service) {
            }

            @Override
            public ServiceReference<Object> getServiceReference() {
                return null;
            }
        };
    }

}
