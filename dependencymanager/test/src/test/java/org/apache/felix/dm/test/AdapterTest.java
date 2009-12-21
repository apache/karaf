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

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.service.Service;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
public class AdapterTest {
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
    public void testInstanceBoundDependency(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Service sp = m.createService().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Service sp2 = m.createService().setImplementation(new ServiceProvider2(e)).setInterface(ServiceInterface2.class.getName(), null);
        Service sc = m.createService().setImplementation(new ServiceConsumer()).add(m.createServiceDependency().setService(ServiceInterface3.class).setRequired(true));
        Service sa = m.createAdapterService(ServiceInterface.class, null, ServiceInterface3.class, new ServiceAdapter(e), null);
        m.add(sc);
        m.add(sp);
        m.add(sp2);
        m.add(sa);
        e.waitForStep(5, 15000);
        m.remove(sa);
        m.remove(sp2);
        m.remove(sp);
        m.remove(sc);
    }
    
    static interface ServiceInterface {
        public void invoke();
    }
    
    static interface ServiceInterface2 {
        public void invoke();
    }
    
    static interface ServiceInterface3 {
        public void invoke();
    }
    
    static class ServiceProvider2 implements ServiceInterface2 {
        private final Ensure m_ensure;

        public ServiceProvider2(Ensure ensure) {
            m_ensure = ensure;
        }

        public void invoke() {
            m_ensure.step(4);
        }
    }

    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(5);
        }
    }
    
    static class ServiceAdapter implements ServiceInterface3 {
        private Ensure m_ensure;
        private volatile ServiceInterface m_originalService;
        private volatile ServiceInterface2 m_injectedService;
        private volatile Service m_service;
        private volatile DependencyManager m_manager;
        
        public ServiceAdapter(Ensure e) {
            m_ensure = e;
        }
        public void init() {
            m_ensure.step(1);
            m_service.add(m_manager.createServiceDependency().setInstanceBound(true).setRequired(true).setService(ServiceInterface2.class));
        }
        public void start() {
            m_ensure.step(2);
        }
        public void invoke() {
            m_ensure.step(3);
            m_injectedService.invoke();
            m_originalService.invoke();
        }
        
        public void stop() {
            m_ensure.step(6);
        }
    }

    static class ServiceConsumer implements Runnable {
        private volatile ServiceInterface3 m_service;
        
        public void init() {
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
            m_service.invoke();
        }
    }
}


