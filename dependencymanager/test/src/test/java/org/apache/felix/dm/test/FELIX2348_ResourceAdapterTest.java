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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.resources.Resource;
import org.apache.felix.dm.resources.ResourceHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(JUnit4TestRunner.class)
public class FELIX2348_ResourceAdapterTest extends Base {
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
    public void testBasicResourceAdapter(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        m.add(m.createResourceAdapterService("(&(path=/test)(name=*.txt)(repository=TestRepository))", 
                                            null, 
                                            (String) null, null,
                                            new ResourceAdapter(e), 
                                            false));
        m.add(m.createService().setImplementation(new ResourceProvider(e)).add(m.createServiceDependency().setService(ResourceHandler.class).setCallbacks("add", "remove")));
        e.waitForStep(3, 5000);
     }
    
    static class ResourceAdapter {
        protected Resource m_resource; // injected by reflection.
        private Ensure m_ensure;
        
        ResourceAdapter(Ensure e) {
            m_ensure = e;
        }
        
        public void start() {
            m_ensure.step(1);
            Assert.assertNotNull("resource not injected", m_resource);
            m_ensure.step(2);
            try {
                InputStream in= m_resource.openStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int c;
                while ((c = in.read()) != -1) {
                    out.write(c);
                }
                String msg = new String(out.toByteArray(), "UTF8");
                Assert.assertEquals("resource", msg);
            } catch (Throwable t) {
                t.printStackTrace();
                Assert.fail();
            }
            m_ensure.step(3);
        }
    }
    
    static class ResourceProvider {
        private volatile BundleContext m_context;
        private final Ensure m_ensure;
        private final Map m_handlers = new HashMap();
        private StaticResource[] m_resources = {
            new StaticResource("test1.txt", "/test", "TestRepository") {
                public InputStream openStream() throws IOException {
                    return new ByteArrayInputStream("resource".getBytes("UTF8"));
                };
            }
        };

        public ResourceProvider(Ensure ensure) {
            m_ensure = ensure;
        }
        
        public void add(ServiceReference ref, ResourceHandler handler) {
            String filterString = (String) ref.getProperty("filter");
            Filter filter;
            try {
                filter = m_context.createFilter(filterString);
            }
            catch (InvalidSyntaxException e) {
                Assert.fail("Could not create filter for resource handler: " + e);
                return;
            }
            synchronized (m_handlers) {
                m_handlers.put(handler, filter);
            }
                for (int i = 0; i < m_resources.length; i++) {
                    if (filter.match(m_resources[i].getProperties())) {
                        handler.added(m_resources[i]);
                    }
                }
            }

        public void remove(ServiceReference ref, ResourceHandler handler) {
            Filter filter;
            synchronized (m_handlers) {
                filter = (Filter) m_handlers.remove(handler);
            }
            removeResources(handler, filter);
        }

        private void removeResources(ResourceHandler handler, Filter filter) {
                for (int i = 0; i < m_resources.length; i++) {
                    if (filter.match(m_resources[i].getProperties())) {
                        handler.removed(m_resources[i]);
                    }
                }
            }

        public void destroy() {
            Entry[] handlers;
            synchronized (m_handlers) {
                handlers = (Entry[]) m_handlers.entrySet().toArray(new Entry[m_handlers.size()]);
            }
            for (int i = 0; i < handlers.length; i++) {
                removeResources((ResourceHandler) handlers[i].getKey(), (Filter) handlers[i].getValue());
            }
            
            System.out.println("DESTROY..." + m_handlers.size());
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
                put(Resource.ID, getID());
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
