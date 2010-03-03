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

import java.util.Properties;

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

/**
 * Use case: Verify Bundle Dependency annotations usage.
 */
@RunWith(JUnit4TestRunner.class)
public class BundleDependencyAnnotationTest extends AnnotationBase
{
    @Configuration
    public static Option[] configuration()
    {
        return options(
            systemProperty("dm.log").value( "true" ),
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager.runtime").versionAsInProject()),
            provision(
                new BundleGenerator()
                    .set(Constants.BUNDLE_SYMBOLICNAME, "BundleDependencyTest")
                    .set("Export-Package", "org.apache.felix.dm.test.bundle.annotation.sequencer")
                    .set("Private-Package", "org.apache.felix.dm.test.bundle.annotation.bundledependency")
                    .set("Import-Package", "*")
                    .set("-plugin", "org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin")
                    .build()));           
    }

    /**
     * Tests a simple Consumer, which has a BundleDependency over the dependency manager bundle.
     * @param context
     */
    @Test
    public void testBundleDependencyAnnotation(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        Properties props = new Properties() {{ put("test", "consumer"); }};
        m.add(m.createService().setImplementation(this).setInterface(Sequencer.class.getName(), props));
        m_ensure.waitForStep(1, 10000);
        super.stopBundle("BundleDependencyTest", context);
        m_ensure.waitForStep(2, 10000);
    }
    
    /**
     * Tests a Bundle Adapter, which adapts the dependency manager bundle to a "ServiceInterface" service.
     * @param context
     */
    @Test
    public void testBundleAdapterServiceAnnotation(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        Properties props = new Properties() {{ put("test", "adapter"); }};
        m.add(m.createService().setImplementation(this).setInterface(Sequencer.class.getName(), props));
        m_ensure.waitForStep(3, 10000);
    }    
}
