/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularDataSupport;
import javax.management.remote.JMXConnector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BundleTests extends KarafTestSupport {

    @Test
    public void listCommand() throws Exception {
        String listOutput = executeCommand("bundle:list -t 0");
        System.out.println(listOutput);
        assertFalse(listOutput.isEmpty());
    }

    @Test
    public void listViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=bundle,name=root");
            TabularDataSupport value = (TabularDataSupport) connection.getAttribute(name, "Bundles");
            assertTrue(value.size() > 0);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void capabilitiesCommand() throws Exception {
        String allCapabilitiesOutput = executeCommand("bundle:capabilities");
        System.out.println(allCapabilitiesOutput);
        assertFalse(allCapabilitiesOutput.isEmpty());
        String jmxWhiteboardBundleCapabilitiesOutput = executeCommand("bundle:capabilities org.apache.aries.jmx.whiteboard");
        System.out.println(jmxWhiteboardBundleCapabilitiesOutput);
        assertTrue(jmxWhiteboardBundleCapabilitiesOutput.contains("osgi.wiring.bundle; org.apache.aries.jmx.whiteboard 1.0.0 [UNUSED]"));
    }

    @Test
    public void classesCommand() throws Exception {
        String allClassesOutput = executeCommand("bundle:classes");
        assertFalse(allClassesOutput.isEmpty());
        String jmxWhiteboardBundleClassesOutput = executeCommand("bundle:classes org.apache.aries.jmx.whiteboard");
        System.out.println(jmxWhiteboardBundleClassesOutput);
        assertTrue(jmxWhiteboardBundleClassesOutput.contains("org/apache/aries/jmx/whiteboard/Activator$MBeanTracker.class"));
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
        String headersOutput = executeCommand("bundle:headers org.apache.aries.jmx.whiteboard");
        System.out.println(headersOutput);
        assertTrue(headersOutput.contains("Bundle-Activator = org.apache.aries.jmx.whiteboard.Activator"));
    }

    @Test
    public void infoCommand() throws Exception {
        String infoOutput = executeCommand("bundle:info org.apache.karaf.management.server");
        System.out.println(infoOutput);
        assertTrue(infoOutput.contains("This bundle starts the Karaf embedded MBean server"));
    }

    @Test
    public void installUninstallCommand() throws Exception {
        executeCommand("bundle:install mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.commons-lang/2.4_6");
        assertBundleInstalled("org.apache.servicemix.bundles.commons-lang");
        executeCommand("bundle:uninstall org.apache.servicemix.bundles.commons-lang");
        assertBundleNotInstalled("org.apache.servicemix.bundles.commons-lang");
    }

    @Test
    public void showTreeCommand() throws Exception {
        String bundleTreeOutput = executeCommand("bundle:tree-show org.apache.karaf.management.server");
        System.out.println(bundleTreeOutput);
        assertFalse(bundleTreeOutput.isEmpty());
    }

}
