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
public class CompositionTest extends Base {
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
    public void testComposition(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Service sp = m.createService().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Service sc = m.createService().setImplementation(new ServiceConsumer(e))
                                      .setComposition("getComposition")
                                      .add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true).setCallbacks("add", null));
        m.add(sp);
        m.add(sc);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
    
    static interface ServiceInterface {
        public void invoke();
    }

    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(4);
        }
    }

    static class ServiceConsumer {
        private final Ensure m_ensure;
        private ServiceConsumerComposite m_composite;
        private ServiceInterface m_service;

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
            m_composite = new ServiceConsumerComposite(m_ensure);
        }
        
        public Object[] getComposition() {
            return new Object[] { this, m_composite };
        }
        
        void add(ServiceInterface service) {
            m_ensure.step(1);
            m_service = service; // This method seems to not being called anymore 
        }
        
        void start() {
            m_composite.invoke();
            m_ensure.step(5); 
        }
    }
    
    static class ServiceConsumerComposite {
        ServiceInterface m_service;
        private Ensure m_ensure;
        
        ServiceConsumerComposite(Ensure ensure)
        {
            m_ensure = ensure;
        }

        void add(ServiceInterface service) {

            m_ensure.step(2);
            m_service = service;
        }

        void invoke()
        {
            m_ensure.step(3);
            m_service.invoke();
        }
    }
}
