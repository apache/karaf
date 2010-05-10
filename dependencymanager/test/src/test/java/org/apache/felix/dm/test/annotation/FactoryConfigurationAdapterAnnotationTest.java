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
package org.apache.felix.dm.test.annotation;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.IOException;
import java.util.Hashtable;

import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.test.BundleGenerator;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Use case: Verify that an annotated Configuration Factory Adapter Service is properly created when a factory configuration
 * is created from Config Admin.
 */
@RunWith(JUnit4TestRunner.class)
public class FactoryConfigurationAdapterAnnotationTest extends AnnotationBase
{
    @Configuration
    public static Option[] configuration()
    {
        return options(
            systemProperty("dm.log").value( "true" ),
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.2.4"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager.runtime").versionAsInProject()),
            provision(
                new BundleGenerator()
                    .set(Constants.BUNDLE_SYMBOLICNAME, "FactoryConfigurationAdapterTest")
                    .set("Export-Package", "org.apache.felix.dm.test.bundle.annotation.sequencer")
                    .set("Private-Package", "org.apache.felix.dm.test.bundle.annotation.factoryconfadapter")
                    .set("Import-Package", "*")
                    .set("-plugin", "org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin")
                    .build()));           
    }

    @Test
    public void testFactoryConfigurationAdapterAnnotation(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        // Provide the Sequencer to the adapter bundle service (see main/src/.../factoryconfadapter/*.java). 
        m.add(m.createService().setImplementation(this).setInterface(Sequencer.class.getName(), null));
        ConfigurationAdmin cm = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        try
        {
            // Create a factory configuration in order to instantiate the ServiceProvider
            org.osgi.service.cm.Configuration cf = cm.createFactoryConfiguration("FactoryPidTest", null);
            cf.update(new Hashtable() {{ put("foo", "bar"); }});
            // Wait for the ServiceProvider activation.
            m_ensure.waitForStep(2, 10000);
            // Avoid bug in CM (FELIX-1545), which may miss some updates
            sleep(1); 
            // Update conf
            cf.update(new Hashtable() {{ put("foo", "bar2"); }});            
            // Wait for effective update
            m_ensure.waitForStep(4, 10000);
            // Avoid bug in CM (FELIX-1545), which may miss some updates
            sleep(1);
            // Remove configuration.
            cf.delete();
            // Check if ServiceProvider has been stopped.
            m_ensure.waitForStep(5, 1000);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Assert.fail("can't create factory configuration");
        }
    }
}
