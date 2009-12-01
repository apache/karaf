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
package org.apache.felix.dependencymanager.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.util.Properties;

import org.apache.felix.dependencymanager.DependencyManager;
import org.apache.felix.dependencymanager.Service;
import org.apache.felix.dependencymanager.impl.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@RunWith(JUnit4TestRunner.class)
public class AspectTest {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.2.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    

    @Test
    public void testServiceRegistrationAndConsumption(BundleContext context) {
        DependencyManager m = new DependencyManager(context, new Logger(context));
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Service sp = m.createService().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Service sc = m.createService().setImplementation(new ServiceConsumer(e)).add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true));
        Service sa = m.createAspectService(ServiceInterface.class, "(|(!(" + Constants.SERVICE_RANKING + "=*))(" + Constants.SERVICE_RANKING + "<=0))", new ServiceAspect(e), new Properties() {{ put(Constants.SERVICE_RANKING, Integer.valueOf(1)); }} );
        m.add(sc);
        m.add(sp);
        e.waitForStep(3, 2000);
        m.add(sa);
        e.waitForStep(4, 2000);
        e.step(5);
        e.waitForStep(8, 2000);
        m.remove(sa);
        e.waitForStep(9, 2000);
        e.step(10);
        e.waitForStep(11, 2000);
        m.remove(sp);
        m.remove(sc);
    }
    
    static interface ServiceInterface {
        public void invoke(Runnable run);
    }

    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke(Runnable run) {
            run.run();
        }
    }
    
    static class ServiceAspect implements ServiceInterface {
        private final Ensure m_ensure;
        private volatile ServiceInterface m_originalService;
        
        public ServiceAspect(Ensure e) {
            m_ensure = e;
        }
        public void start() {
            m_ensure.step(4);
        }
        public void invoke(Runnable run) {
            m_ensure.step(6);
            m_originalService.invoke(run);
        }
        
        public void stop() {
            m_ensure.step(9);
        }
    }

    static class ServiceConsumer implements Runnable {
        private volatile ServiceInterface m_service;
        private final Ensure m_ensure;

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
            m_ensure.step(1);
            m_service.invoke(Ensure.createRunnableStep(m_ensure, 2));
            m_ensure.step(3);
            m_ensure.waitForStep(5, 2000);
            m_service.invoke(Ensure.createRunnableStep(m_ensure, 7));
            m_ensure.step(8);
            m_ensure.waitForStep(10, 2000);
            m_service.invoke(Ensure.createRunnableStep(m_ensure, 11));
        }
    }
}
