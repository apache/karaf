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

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.dependencies.BundleDependency;
import org.apache.felix.dm.dependencies.ConfigurationDependency;
import org.apache.felix.dm.dependencies.ResourceDependency;
import org.apache.felix.dm.dependencies.ServiceDependency;
import org.apache.felix.dm.resources.Resource;
import org.apache.felix.dm.resources.ResourceHandler;
import org.apache.felix.dm.service.Service;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

@RunWith(JUnit4TestRunner.class)
public class SharingDependenciesWithMultipleServicesTest {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.2.4")
            )
        );
    }    
    
    @Test
    public void testShareServiceDependencyWithMultipleServices(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Service provider = m.createService().setImplementation(new ServiceProvider()).setInterface(ServiceInterface.class.getName(), null);
        ServiceDependency dependency = m.createServiceDependency().setService(ServiceInterface.class).setRequired(true);
        Service consumer1 = m.createService().setImplementation(new ServiceConsumer(e, 1)).add(dependency);
        Service consumer2 = m.createService().setImplementation(new ServiceConsumer(e, 4)).add(dependency);
        
        m.add(provider);
        m.add(consumer1);
        e.waitForStep(3, 15000);
        m.add(consumer2);
        e.waitForStep(6, 15000);
        m.remove(provider);
        m.remove(consumer1);
        m.remove(consumer2);
    }
    
    @Test
    public void testShareConfigurationDependencyWithMultipleServices(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Service provider = m.createService().setImplementation(new ConfigurationProvider(e)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        ConfigurationDependency dependency = m.createConfigurationDependency().setPid("test");
        Service consumer1 = m.createService().setImplementation(new ConfigurationConsumer(e, 2)).add(dependency);
        Service consumer2 = m.createService().setImplementation(new ConfigurationConsumer(e, 3)).add(dependency);
        
        // add the configuration provider that should publish the configuration as step 1
        m.add(provider);
        // add the first consumer, and wait until its updated() method is invoked
        m.add(consumer1);
        e.waitForStep(2, 15000);
        // add the second consumer, and wait until its updated() method is invoked
        m.add(consumer2);
        e.waitForStep(3, 15000);
        // break down the test again
        m.remove(consumer2);
        m.remove(consumer1);
        m.remove(provider);
    }
    
    @Test
    public void testShareBundleDependencyWithMultipleServices(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        BundleDependency dependency = m.createBundleDependency().setFilter("(Bundle-SymbolicName=org.apache.felix.dependencymanager)").setRequired(true);
        Service consumer1 = m.createService().setImplementation(new BundleConsumer(e, 1)).add(dependency);
        Service consumer2 = m.createService().setImplementation(new BundleConsumer(e, 2)).add(dependency);
        
        m.add(consumer1);
        e.waitForStep(1, 15000);
        m.add(consumer2);
        e.waitForStep(2, 15000);
        m.remove(consumer2);
        m.remove(consumer1);
    }
    
    @Test
    public void testShareResourceDependencyWithMultipleServices(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        ResourceDependency dependency = m.createResourceDependency().setFilter("(" + Resource.REPOSITORY + "=TestRepository)").setRequired(true);
        Service consumer1 = m.createService().setImplementation(new ResourceConsumer(e, 1)).add(dependency);
        Service consumer2 = m.createService().setImplementation(new ResourceConsumer(e, 2)).add(dependency);
        Service resourceProvider = m.createService().setImplementation(new ResourceProvider()).add(m.createServiceDependency().setService(ResourceHandler.class).setCallbacks("add", "remove"));;
        m.add(resourceProvider);
        m.add(consumer1);
        e.waitForStep(1, 15000);
        m.add(consumer2);
        e.waitForStep(2, 15000);
        m.remove(consumer2);
        m.remove(consumer1);
        m.remove(resourceProvider);
    }
    
    
    static interface ServiceInterface {
        public void invoke(Runnable r);
    }

    static class ServiceProvider implements ServiceInterface {
        public void invoke(Runnable r) {
            r.run();
        }
    }
    
    static class ServiceConsumer implements Runnable {
        private volatile ServiceInterface m_service;
        private final Ensure m_ensure;
        private int m_step;

        public ServiceConsumer(Ensure e, int step) {
            m_ensure = e;
            m_step = step;
        }

        public void start() {
            Thread t = new Thread(this);
            t.start();
        }

        public void run() {
            m_ensure.step(m_step);
            m_service.invoke(new Runnable() { public void run() { m_ensure.step(m_step + 1); } });
            m_ensure.step(m_step + 2);
        }
    }
    
    static class ConfigurationConsumer implements ManagedService {
        private final Ensure m_ensure;
        private int m_step;

        public ConfigurationConsumer(Ensure e, int step) {
            m_ensure = e;
            m_step = step;
        }

        public void updated(Dictionary properties) throws ConfigurationException {
            if (properties != null) {
                m_ensure.step(m_step);
            }
        }
    }
    
    static class ConfigurationProvider {
        private final Ensure m_ensure;
        private volatile ConfigurationAdmin m_configAdmin;
        
        public ConfigurationProvider(Ensure ensure) {
            m_ensure = ensure;
        }
        
        public void init() {
            try {
                org.osgi.service.cm.Configuration conf = m_configAdmin.getConfiguration("test", null);
                conf.update(new Properties() {{ put("testkey", "testvalue"); }} );
                m_ensure.step(1);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    static class BundleConsumer {
        private final Ensure m_ensure;
        private int m_step;

        public BundleConsumer(Ensure e, int step) {
            m_ensure = e;
            m_step = step;
        }
        
        public void start() {
            m_ensure.step(m_step);
        }
    }

    static class ResourceConsumer {
        private final Ensure m_ensure;
        private int m_step;

        public ResourceConsumer(Ensure e, int step) {
            m_ensure = e;
            m_step = step;
        }
        
        public void start() {
            m_ensure.step(m_step);
        }
    }
    
    static class ResourceProvider {
        private volatile BundleContext m_context;
        private StaticResource[] m_resources = {
            new StaticResource("test1.txt", "/test", "TestRepository"),
            new StaticResource("test2.txt", "/test", "TestRepository")
        };

        public void add(ServiceReference ref, ResourceHandler handler) {
            String filterString = (String) ref.getProperty("filter");
            try {
                Filter filter = m_context.createFilter(filterString);
                for (int i = 0; i < m_resources.length; i++) {
                    if (filter.match(m_resources[i].getProperties())) {
                        handler.added(m_resources[i]);
                    }
                }
            }
            catch (InvalidSyntaxException e) {
                e.printStackTrace();
            }
        }

        public void remove(ServiceReference ref, ResourceHandler handler) {
            String filterString = (String) ref.getProperty("filter");
            try {
                Filter filter = m_context.createFilter(filterString);
                for (int i = 0; i < m_resources.length; i++) {
                    if (filter.match(m_resources[i].getProperties())) {
                        handler.removed(m_resources[i]);
                    }
                }
            }
            catch (InvalidSyntaxException e) {
                e.printStackTrace();
            }
        }
    }
    
    static class StaticResource implements Resource {
        private String m_id;
        private String m_name;
        private String m_path;
        private String m_repository;

        public StaticResource(String name, String path, String repository) {
            m_id = repository + ":" + path + "/" + name;
            m_name = name;
            m_path = path;
            m_repository = repository;
        }
        
        public String getID() {
            return m_id;
        }

        public String getName() {
            return m_name;
        }

        public String getPath() {
            return m_path;
        }

        public String getRepository() {
            return m_repository;
        }
        
        public Dictionary getProperties() {
            return new Properties() {{
                put(Resource.NAME, getName());
                put(Resource.PATH, getPath());
                put(Resource.REPOSITORY, getRepository());
            }};
        }

        public InputStream openStream() throws IOException {
            return null;
        }
    }
}
