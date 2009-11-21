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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.felix.dependencymanager.DependencyManager;
import org.apache.felix.dependencymanager.Logger;
import org.apache.felix.dependencymanager.Service;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

@RunWith(JUnit4TestRunner.class)
public class ConfigurationDependencyTest {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.apache.felix").artifactId("org.osgi.compendium").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.2.4"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    
    
    @Test
    public void testComponentWithRequiredConfigurationAndServicePropertyPropagation(BundleContext context) {
        DependencyManager m = new DependencyManager(context, new Logger(context));
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Service s1 = m.createService().setImplementation(new ConfigurationConsumer(e)).setInterface(Runnable.class.getName(), null).add(m.createConfigurationDependency().setPid("test").setPropagate(true));
        Service s2 = m.createService().setImplementation(new ConfigurationCreator(e)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Service s3 = m.createService().setImplementation(new ConfiguredServiceConsumer(e)).add(m.createServiceDependency().setService(Runnable.class, ("(testkey=testvalue)")).setRequired(true));
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(4, 2000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
        e.step(5);
    }
}

class ConfigurationCreator {
    private volatile ConfigurationAdmin m_ca;
    private final Ensure m_ensure;
    
    public ConfigurationCreator(Ensure e) {
        m_ensure = e;
    }

    public void start() {
        try {
            m_ensure.step(1);
            org.osgi.service.cm.Configuration conf = m_ca.getConfiguration("test", null);
            Properties props = new Properties();
            props.put("testkey", "testvalue");
            conf.update(props);
        }
        catch (IOException e) {
            Assert.fail("Could not create configuration: " + e.getMessage());
        }
    }
}

class ConfigurationConsumer implements ManagedService, Runnable {
    private final Ensure m_ensure;

    public ConfigurationConsumer(Ensure e) {
        m_ensure = e;
    }

    public void updated(Dictionary props) throws ConfigurationException {
        if (props != null) {
            m_ensure.step(2);
            if (!"testvalue".equals(props.get("testkey"))) {
                Assert.fail("Could not find the configured property.");
            }
        }
    }
    
    public void run() {
        m_ensure.step(4);
    }
}

class ConfiguredServiceConsumer {
    private final Ensure m_ensure;
    private volatile Runnable m_runnable;

    public ConfiguredServiceConsumer(Ensure e) {
        m_ensure = e;
    }
    
    public void start() {
        m_ensure.step(3);
        m_runnable.run();
    }
}
