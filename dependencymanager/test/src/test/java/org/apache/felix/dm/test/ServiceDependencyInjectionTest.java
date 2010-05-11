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

@RunWith(JUnit4TestRunner.class)
public class ServiceDependencyInjectionTest {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    
    
    @Test
    public void testServiceInjection(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // create a service provider and consumer
        ServiceProvider provider = new ServiceProvider();
        Service sp = m.createService().setImplementation(provider).setInterface(ServiceInterface2.class.getName(), null);
        Service sc = m.createService().setImplementation(new ServiceConsumer()).add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(true));
        Service sc2 = m.createService()
            .setImplementation(new ServiceConsumerNamedInjection())
            .add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(false).setAutoConfig("m_service"))
            .add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(false).setAutoConfig("m_service2"))
            .add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(false).setAutoConfig("m_service3"))
            ;
        m.add(sp);
        m.add(sc);
        m.remove(sc);
        m.add(sc2);
        m.remove(sc2);
        m.remove(sp);
        provider.validate(2, 3);
    }
    
    static interface ServiceInterface {
        public void invoke();
    }
    
    static interface ServiceInterface2 extends ServiceInterface {
        public void invoke2();
    }

    static class ServiceProvider implements ServiceInterface2 {
        private int m_counter1 = 0;
        private int m_counter2 = 0;
        
        public void invoke() {
            m_counter1++;
        }
        
        public void invoke2() {
            m_counter2++;
        }
        
        public void validate(int c1, int c2) {
            Assert.assertEquals("invoke() should have been called this many times", c1, m_counter1);
            Assert.assertEquals("invoke2() should have been called this many times", c2, m_counter2);
        }
    }

    static class ServiceConsumer {
        private volatile ServiceInterface2 m_service;
        private volatile ServiceInterface2 m_service2;
        
        public void init() {
            // invoke the second method of the interface via both injected members, to ensure
            // neither of them is a null object (or null)
            m_service.invoke2();
            m_service2.invoke2();
            Assert.assertEquals("Both members should have been injected with the same service.", m_service, m_service2);
        }
    }

    static class ServiceConsumerNamedInjection {
        private volatile ServiceInterface2 m_service;
        private volatile ServiceInterface m_service2;
        private volatile Object m_service3;

        public void init() {
            // invoke the second method
            m_service.invoke2();
            // invoke the first method (twice)
            m_service2.invoke();
            ((ServiceInterface) m_service3).invoke();
            Assert.assertNotNull("Should have been injected", m_service);
            Assert.assertNotNull("Should have been injected", m_service2);
            Assert.assertNotNull("Should have been injected", m_service3);
            Assert.assertEquals("Members should have been injected with the same service.", m_service, m_service2);
            Assert.assertEquals("Members should have been injected with the same service.", m_service, m_service3);
        }
    }
}
