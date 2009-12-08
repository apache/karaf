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
public class ComponentLifeCycleTest {
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
    public void testComponentLifeCycleCallbacks(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a simple service component
        Service s = m.createService().setImplementation(new ComponentInstance(e));
        // add it, and since it has no dependencies, it should be activated immediately
        m.add(s);
        // remove it so it gets destroyed
        m.remove(s);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
    
    @Test
    public void testCustomComponentLifeCycleCallbacks(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a simple service component
        Service s = m.createService().setImplementation(new CustomComponentInstance(e)).setCallbacks("a", "b", "c", "d");
        // add it, and since it has no dependencies, it should be activated immediately
        m.add(s);
        // remove it so it gets destroyed
        m.remove(s);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }

    static class ComponentInstance {
        private final Ensure m_ensure;
        public ComponentInstance(Ensure e) {
            m_ensure = e;
            m_ensure.step(1);
        }
        public void init() {
            m_ensure.step(2);
        }
        public void start() {
            m_ensure.step(3);
        }
        public void stop() {
            m_ensure.step(4);
        }
        public void destroy() {
            m_ensure.step(5);
        }
    }

    static class CustomComponentInstance {
        private final Ensure m_ensure;
        public CustomComponentInstance(Ensure e) {
            m_ensure = e;
            m_ensure.step(1);
        }
        public void a() {
            m_ensure.step(2);
        }
        public void b() {
            m_ensure.step(3);
        }
        public void c() {
            m_ensure.step(4);
        }
        public void d() {
            m_ensure.step(5);
        }
    }
}
