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
public class TemporalServiceDependencyTest {
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
    public void testServiceConsumptionAndIntermittentAvailability(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Service sp = m.createService().setImplementation(new TemporalServiceProvider(e)).setInterface(TemporalServiceInterface.class.getName(), null);
        Service sp2 = m.createService().setImplementation(new TemporalServiceProvider2(e)).setInterface(TemporalServiceInterface.class.getName(), null);
        Service sc = m.createService().setImplementation(new TemporalServiceConsumer(e)).add(m.createTemporalServiceDependency().setService(TemporalServiceInterface.class).setRequired(true));
        // add the service consumer
        m.add(sc);
        // now add the first provider
        m.add(sp);
        e.waitForStep(2, 1000);
        // and remove it again (this should not affect the consumer yet)
        m.remove(sp);
        // now add the second provider
        m.add(sp2);
        e.step(3);
        e.waitForStep(4, 1000);
        // and remove it again
        m.remove(sp2);
        // finally remove the consumer
        m.remove(sc);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }

    static interface TemporalServiceInterface {
        public void invoke();
    }

    static class TemporalServiceProvider implements TemporalServiceInterface {
        private final Ensure m_ensure;
        public TemporalServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(2);
        }
    }

    static class TemporalServiceProvider2 implements TemporalServiceInterface {
        private final Ensure m_ensure;
        public TemporalServiceProvider2(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(4);
        }
    }

    static class TemporalServiceConsumer implements Runnable {
        private volatile TemporalServiceInterface m_service;
        private final Ensure m_ensure;

        public TemporalServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            m_ensure.step(1);
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
            m_service.invoke();
            m_ensure.waitForStep(3, 1000);
            m_service.invoke();
        }
        
        public void destroy() {
            m_ensure.step(5);
        }
    }
}
