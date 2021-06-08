/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.TabularDataSupport;

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.lang.management.ManagementFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BundleTest extends BaseTest {

    private static final RolePrincipal[] ADMIN_ROLES = {
            new RolePrincipal(BundleService.SYSTEM_BUNDLES_ROLE),
            new RolePrincipal("admin"),
            new RolePrincipal("manager")
    };

    @Test
    public void listCommand() throws Exception {
        String listOutput = executeCommand("bundle:list -t 0", ADMIN_ROLES);
        System.out.println(listOutput);
        assertFalse(listOutput.isEmpty());
    }

    @Test
    public void laAlias() throws Exception {
        String laOutput = executeAlias("la", ADMIN_ROLES);
        System.out.println(laOutput);
        assertFalse(laOutput.isEmpty());
    }

    @Test
    public void listViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("org.apache.karaf:type=bundle,name=root");
            TabularDataSupport value = (TabularDataSupport) mbeanServer.getAttribute(name, "Bundles");
            assertTrue(value.size() > 0);
    }

    @Test
    public void capabilitiesCommand() throws Exception {
        String allCapabilitiesOutput = executeCommand("bundle:capabilities", ADMIN_ROLES);
        System.out.println(allCapabilitiesOutput);
        assertFalse(allCapabilitiesOutput.isEmpty());
        String jmxWhiteboardBundleCapabilitiesOutput = executeCommand("bundle:capabilities org.apache.aries.jmx.whiteboard", ADMIN_ROLES);
        System.out.println(jmxWhiteboardBundleCapabilitiesOutput);
        assertContains("osgi.wiring.bundle; org.apache.aries.jmx.whiteboard 1.2.0 [UNUSED]", jmxWhiteboardBundleCapabilitiesOutput);
    }

    @Test
    public void classesCommand() throws Exception {
        String allClassesOutput = executeCommand("bundle:classes", ADMIN_ROLES);
        assertFalse(allClassesOutput.isEmpty());
        String jmxWhiteboardBundleClassesOutput = executeCommand("bundle:classes org.apache.aries.jmx.whiteboard", ADMIN_ROLES);
        System.out.println(jmxWhiteboardBundleClassesOutput);
        assertContains("org/apache/aries/jmx/whiteboard/Activator$MBeanTracker.class", jmxWhiteboardBundleClassesOutput);
    }

    /**
     * TODO We need some more thorough tests for diag
     */
    @Test
    public void diagCommand() throws Exception {
        String allDiagOutput = executeCommand("bundle:diag");
        assertTrue(allDiagOutput.isEmpty());
    }

    @Test
    public void findClassCommand() throws Exception {
        String findClassOutput = executeCommand("bundle:find-class jmx");
        System.out.println(findClassOutput);
        assertFalse(findClassOutput.isEmpty());
    }

    @Test
    public void headersCommand() throws Exception {
        String headersOutput = executeCommand("bundle:headers org.apache.aries.jmx.whiteboard", ADMIN_ROLES);
        System.out.println(headersOutput);
        assertContains("Bundle-Activator = org.apache.aries.jmx.whiteboard.Activator", headersOutput);
    }

    @Test
    public void infoCommand() throws Exception {
        String infoOutput = executeCommand("bundle:info org.apache.karaf.management.server", ADMIN_ROLES);
        System.out.println(infoOutput);
        assertContains("This bundle starts the Karaf embedded MBean server", infoOutput);
    }

    @Test
    public void installUninstallCommand() throws Exception {
        executeCommand("bundle:install mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.commons-lang/2.4_6", ADMIN_ROLES);
        assertBundleInstalled("org.apache.servicemix.bundles.commons-lang");
        executeCommand("bundle:uninstall org.apache.servicemix.bundles.commons-lang", ADMIN_ROLES);
        assertBundleNotInstalled("org.apache.servicemix.bundles.commons-lang");
    }

    @Test
    public void showTreeCommand() throws Exception {
        String bundleTreeOutput = executeCommand("bundle:tree-show org.apache.karaf.management.server", ADMIN_ROLES);
        System.out.println(bundleTreeOutput);
        assertFalse(bundleTreeOutput.isEmpty());
    }

    @Test
    public void statusCommand() throws Exception {
        String statusOutput = executeCommand("bundle:status org.apache.karaf.management.server", ADMIN_ROLES);
        System.out.println(statusOutput);
        assertFalse(statusOutput.isEmpty());
    }

}
