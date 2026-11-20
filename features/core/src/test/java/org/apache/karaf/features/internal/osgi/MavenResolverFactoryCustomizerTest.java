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
package org.apache.karaf.features.internal.osgi;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.karaf.features.spi.MavenResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class MavenResolverFactoryCustomizerTest {

    private final MavenResolverFactory paxUrl = createMock(MavenResolverFactory.class);
    private final MavenResolverFactory karafUrl = createMock(MavenResolverFactory.class);
    private final List<MavenResolverFactory> selected = new ArrayList<>();
    private BundleContext bundleContext;
    private MavenResolverFactoryCustomizer customizer;

    @Before
    public void setUp() {
        bundleContext = niceMock(BundleContext.class);
        customizer = new MavenResolverFactoryCustomizer(bundleContext, selected::add);
    }

    @Test
    public void testFirstRegisteredFactoryIsKeptForTheSameRanking() {
        ServiceReference<MavenResolverFactory> paxUrlRef = reference(10, new AtomicInteger());
        ServiceReference<MavenResolverFactory> karafUrlRef = reference(20, new AtomicInteger());
        expectServices(paxUrlRef, karafUrlRef);

        customizer.addingService(paxUrlRef);
        customizer.addingService(karafUrlRef);

        assertEquals(Arrays.asList(paxUrl), selected);
    }

    @Test
    public void testHigherRankedFactoryIsSelected() {
        ServiceReference<MavenResolverFactory> paxUrlRef = reference(10, new AtomicInteger());
        ServiceReference<MavenResolverFactory> karafUrlRef = reference(20, new AtomicInteger(100));
        expectServices(paxUrlRef, karafUrlRef);

        customizer.addingService(paxUrlRef);
        customizer.addingService(karafUrlRef);

        assertEquals(Arrays.asList(paxUrl, karafUrl), selected);
    }

    @Test
    public void testFallBackToRemainingFactoryWhenSelectedOneIsRemoved() {
        ServiceReference<MavenResolverFactory> paxUrlRef = reference(10, new AtomicInteger());
        ServiceReference<MavenResolverFactory> karafUrlRef = reference(20, new AtomicInteger());
        expectServices(paxUrlRef, karafUrlRef);

        customizer.addingService(paxUrlRef);
        customizer.addingService(karafUrlRef);
        customizer.removedService(paxUrlRef, paxUrl);

        assertEquals(Arrays.asList(paxUrl, karafUrl), selected);
    }

    @Test
    public void testRemovingAnotherFactoryKeepsTheSelectedOne() {
        ServiceReference<MavenResolverFactory> paxUrlRef = reference(10, new AtomicInteger());
        ServiceReference<MavenResolverFactory> karafUrlRef = reference(20, new AtomicInteger());
        expectServices(paxUrlRef, karafUrlRef);

        customizer.addingService(paxUrlRef);
        customizer.addingService(karafUrlRef);
        customizer.removedService(karafUrlRef, karafUrl);

        assertEquals(Arrays.asList(paxUrl), selected);
    }

    @Test
    public void testNoFactoryWhenTheLastOneIsRemoved() {
        ServiceReference<MavenResolverFactory> karafUrlRef = reference(20, new AtomicInteger());
        expectServices(karafUrlRef);

        customizer.addingService(karafUrlRef);
        customizer.removedService(karafUrlRef, karafUrl);

        assertEquals(2, selected.size());
        assertNull(selected.get(1));
    }

    @Test
    public void testRankingChangeIsTakenIntoAccount() {
        ServiceReference<MavenResolverFactory> paxUrlRef = reference(10, new AtomicInteger());
        AtomicInteger karafUrlRanking = new AtomicInteger();
        ServiceReference<MavenResolverFactory> karafUrlRef = reference(20, karafUrlRanking);
        expectServices(paxUrlRef, karafUrlRef);

        customizer.addingService(paxUrlRef);
        customizer.addingService(karafUrlRef);
        karafUrlRanking.set(100);
        customizer.modifiedService(karafUrlRef, karafUrl);

        assertEquals(Arrays.asList(paxUrl, karafUrl), selected);
    }

    private void expectServices(ServiceReference<MavenResolverFactory> paxUrlRef,
                                ServiceReference<MavenResolverFactory> karafUrlRef) {
        expect(bundleContext.getService(paxUrlRef)).andReturn(paxUrl).anyTimes();
        expectServices(karafUrlRef);
    }

    private void expectServices(ServiceReference<MavenResolverFactory> karafUrlRef) {
        expect(bundleContext.getService(karafUrlRef)).andReturn(karafUrl).anyTimes();
        expect(bundleContext.ungetService(anyObject())).andReturn(true).anyTimes();
        replay(bundleContext);
    }

    /**
     * A service reference ordered as specified by {@link ServiceReference#compareTo(Object)}.
     */
    @SuppressWarnings("unchecked")
    private static ServiceReference<MavenResolverFactory> reference(long id, AtomicInteger ranking) {
        return (ServiceReference<MavenResolverFactory>) Proxy.newProxyInstance(
                ServiceReference.class.getClassLoader(), new Class<?>[] { ServiceReference.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getProperty":
                            if (Constants.SERVICE_ID.equals(args[0])) {
                                return id;
                            }
                            return Constants.SERVICE_RANKING.equals(args[0]) ? ranking.get() : null;
                        case "compareTo":
                            ServiceReference<?> other = (ServiceReference<?>) args[0];
                            int result = Integer.compare(ranking.get(),
                                    (Integer) other.getProperty(Constants.SERVICE_RANKING));
                            return result != 0 ? result
                                    : Long.compare((Long) other.getProperty(Constants.SERVICE_ID), id);
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "toString":
                            return "ServiceReference[" + id + "]";
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

}
