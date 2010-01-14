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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.test.annotation.Sequencer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

@SuppressWarnings({ "unchecked", "serial" })
@RunWith(JUnit4TestRunner.class)
public class AnnotationTest implements Sequencer
{
    Ensure m_ensure;

    @Configuration
    public static Option[] configuration()
    {
        return options(provision(
            mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject(),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager.runtime").versionAsInProject(),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager.test.annotation").versionAsInProject()));
    }

    @Test
    public void testSimpleAnnotations(BundleContext context)
    {
        m_ensure = new Ensure();
        DependencyManager m = new DependencyManager(context);
        // We provide ourself as a "Sequencer" service: this will active the "org.apache.felix.dependencymanager.test.annotation" bundle
        Dictionary props = new Hashtable() {{ put("test", "simple"); }};
        m.add(m.createService().setImplementation(this).setInterface(Sequencer.class.getName(), props));
        // Check if the test.annotation components have been initialized orderly
        m_ensure.waitForStep(7, 10000);
        // Stop the test.annotation bundle
        boolean found = false;
        for (Bundle b : context.getBundles())
        {
            if (b.getSymbolicName().equals("org.apache.felix.dependencymanager.test.annotation"))
            {
                try
                {
                    found = true;
                    b.stop();
                }
                catch (BundleException e)
                {
                    e.printStackTrace();
                }
            }
        }
        if (! found) 
        {
            throw new IllegalStateException("org.apache.felix.dependencymanager.test.annotation bundle not found");
        }
        // And check if the test.annotation bundle has been deactivated orderly
        m_ensure.waitForStep(11, 10000);
    }

    @Test
    public void testFactoryAnnotation(BundleContext context)
    {
        m_ensure = new Ensure();
        DependencyManager m = new DependencyManager(context);
        // We provide ourself as a "Sequencer" service: this will active the "org.apache.felix.dependencymanager.test.annotation" bundle
        Dictionary props = new Hashtable() {{ put("test", "factory"); }};
        m.add(m.createService().setImplementation(this).setInterface(Sequencer.class.getName(), props));
        // Check if the test.annotation components have been initialized orderly
        m_ensure.waitForStep(1, 10000);
    }
        
    // The test.annotation bundle will call back us here
    public void next(int step)
    {
        m_ensure.step(step);
    }
}
