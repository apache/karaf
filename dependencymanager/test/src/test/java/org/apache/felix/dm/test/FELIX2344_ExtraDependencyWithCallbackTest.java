/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.service.Service;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

/**
 * Tests for extra dependencies which are declared from service's init method.
 */
@RunWith(JUnit4TestRunner.class)
public class FELIX2344_ExtraDependencyWithCallbackTest extends Base {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    

    /**
     * Checks if an extra optional/required dependency is properly injected into a consumer, using callbacks.
     */
    @Test
    public void testExtraDependencyWithCallback(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service consumer and provider
        Service sp = m.createService().setInterface(ProviderInterface.class.getName(), null).setImplementation(ProviderImpl.class);
        Service sc = m.createService().setImplementation(new Client(e, false, 1));
        Service sc2 = m.createService().setImplementation(new Client(e, true, 5));
        Service sc3 = m.createService().setImplementation(new Client(e, true, 9));
        
        // add the provider first, then add the consumer which initially will have no dependencies
        // but via the init() method an optional dependency with a callback method will be added
        m.add(sp);
        m.add(sc);
        // remove the consumer again
        m.remove(sc);
        e.waitForStep(4, 5000);

        // next up, add a second consumer, identical to the first, but with a required dependency
        // with a callback method which will be added in the init() method
        m.add(sc2);
        // remove the consumer again
        m.remove(sc2);
        e.waitForStep(8, 5000);

        // now remove the provider, add a third consumer, identical to the second, and after the
        // consumer has started, add the provider again
        m.remove(sp);
        m.add(sc3);
        m.add(sp);
        e.waitForStep(12, 5000);
    }
    
    public interface ProviderInterface {
    }

    public static class ProviderImpl implements ProviderInterface {
    }

    public static class Client {
        ProviderInterface m_provider;
        private Ensure m_ensure;
        private final boolean m_required;
        private final int m_startStep;

        public Client(Ensure e, boolean required, int startStep) {
            m_ensure = e;
            m_required = required;
            m_startStep = startStep;
        }

        public void init(DependencyManager dm, Service s) {
            m_ensure.step(m_startStep);
            s.add(dm.createServiceDependency()
                .setInstanceBound(true)
                .setService(ProviderInterface.class)
                .setRequired(m_required)
                .setCallbacks("bind", null));
        }

        // called before start() for required dependency, or after start for optional dependency
        void bind(ProviderInterface provider) {
            System.out.println("bind");
            m_ensure.step(m_required ? m_startStep + 1 : m_startStep + 3);
            m_provider = provider;
        }

        public void start() {
            System.out.println("start");
            m_ensure.step(m_required ? m_startStep + 2: m_startStep + 1);
            if (m_required) {
                Assert.assertNotNull("Dependendency should have been injected", m_provider);
            }
            m_ensure.step(m_required ? m_startStep + 3: m_startStep + 2);
        }
    }    
}
