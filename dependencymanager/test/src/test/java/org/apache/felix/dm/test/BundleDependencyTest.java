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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
public class BundleDependencyTest {
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
    public void testBundleDependencies(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // create a service provider and consumer
        Consumer c = new Consumer();
        Service consumer = m.createService().setImplementation(c).add(m.createBundleDependency().setCallbacks("add", "remove"));
        // add the service consumer
        m.add(consumer);
        // check if at least one bundle was found
        c.check();
        // remove the consumer again
        m.remove(consumer);
        // check if all bundles were removed correctly
        c.doubleCheck();
        
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        Service consumerWithFilter = m.createService().setImplementation(new FilteredConsumer(e)).add(m.createBundleDependency().setFilter("(Bundle-SymbolicName=org.apache.felix.dependencymanager)").setCallbacks("add", "remove"));
        // add a consumer with a filter
        m.add(consumerWithFilter);
        e.step(2);
        // remove the consumer again
        m.remove(consumerWithFilter);
        e.step(4);
    }
    
    static class Consumer {
        private volatile int m_count = 0;

        public void add(Bundle b) {
            Assert.assertNotNull("bundle instance must not be null", b);
            m_count++;
        }
        
        public void check() {
            Assert.assertTrue("we should have found at least one bundle", m_count > 0);
        }
        
        public void remove(Bundle b) {
            m_count--;
        }
        
        public void doubleCheck() {
            Assert.assertEquals("all bundles we found should have been removed again", 0, m_count);
        }
    }
    
    static class FilteredConsumer {
        private final Ensure m_ensure;

        public FilteredConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void add(Bundle b) {
            m_ensure.step(1);
        }
        
        public void remove(Bundle b) {
            m_ensure.step(3);
        }
    }
}
