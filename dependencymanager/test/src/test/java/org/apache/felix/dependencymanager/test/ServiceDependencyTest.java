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

import org.apache.felix.dependencymanager.DependencyManager;
import org.apache.felix.dependencymanager.Logger;
import org.apache.felix.dependencymanager.Service;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
public class ServiceDependencyTest {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.apache.felix").artifactId("org.osgi.compendium").versionAsInProject(),
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
        Service sc2 = m.createService().setImplementation(new ServiceConsumerCallbacks(e)).add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(false).setCallbacks("add", "remove"));
        m.add(sp);
        m.add(sc);
        m.remove(sc);
        m.add(sc2);
        m.remove(sp);
        m.remove(sc2);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
}

interface ServiceInterface {
    public void invoke();
}

class ServiceProvider implements ServiceInterface {
    private final Ensure m_ensure;
    public ServiceProvider(Ensure e) {
        m_ensure = e;
    }
    public void invoke() {
        m_ensure.step(2);
    }
}

class ServiceConsumer {
    private volatile ServiceInterface m_service;
    private final Ensure m_ensure;

    public ServiceConsumer(Ensure e) {
        m_ensure = e;
    }
    
    public void start() {
        m_ensure.step(1);
        m_service.invoke();
    }
    
    public void stop() {
        m_ensure.step(3);
    }
}

class ServiceConsumerCallbacks {
    private final Ensure m_ensure;

    public ServiceConsumerCallbacks(Ensure e) {
        m_ensure = e;
    }
    
    public void add(ServiceInterface service) {
        m_ensure.step(4);
    }
    public void remove(ServiceInterface service) {
        m_ensure.step(5);
    }
}
