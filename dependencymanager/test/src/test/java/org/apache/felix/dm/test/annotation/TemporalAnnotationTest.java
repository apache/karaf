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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.service.Service;
import org.apache.felix.dm.test.BundleGenerator;
import org.apache.felix.dm.test.Ensure;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Use case: Verify Temporal Service Dependendency Annotations usage.
 */
@RunWith(JUnit4TestRunner.class)
public class TemporalAnnotationTest extends AnnotationBase
{
    @Configuration
    public static Option[] configuration()
    {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager.runtime").versionAsInProject()),
            provision(
                new BundleGenerator()
                    .set(Constants.BUNDLE_SYMBOLICNAME, "TemporalTest")
                    .set("Export-Package", "org.apache.felix.dm.test.bundle.annotation.sequencer")
                    .set("Private-Package", "org.apache.felix.dm.test.bundle.annotation.temporal")
                    .set("Import-Package", "*")
                    .set("-plugin", "org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin")
                    .build()));            
    }

    @Test
    public void testTemporalServiceDependency(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        // Provide the Sequencer service to the TemporalServiceDependencyTest service. 
        m.add(m.createService().setImplementation(this).setInterface(Sequencer.class.getName(), null));
                 
        Runnable r = Ensure.createRunnableStep(m_ensure, 1);
        Dictionary props = new Hashtable() {{ put("test", "temporal"); }};
        Service s = m.createService().setImplementation(r).setInterface(Runnable.class.getName(), props);        
        m.add(s);
        m_ensure.waitForStep(1, 15000);
        m.remove(s);
        m_ensure.step(2);
        sleep(500);
        r = Ensure.createRunnableStep(m_ensure, 3);
        s = m.createService().setImplementation(r).setInterface(Runnable.class.getName(), props);
        m.add(s);
        m_ensure.waitForStep(3, 15000);
        m.remove(s);
        m_ensure.step(4);
        sleep(1500);
        m_ensure.waitForStep(5, 15000);
    }
}
