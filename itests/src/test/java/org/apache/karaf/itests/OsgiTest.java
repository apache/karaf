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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class OsgiTest extends KarafTestSupport {

    @Test
    public void listCommand() throws Exception {
        String listOutput = executeCommand("osgi:list -t 0");
        System.out.println(listOutput);
        assertFalse(listOutput.isEmpty());
    }

    @Test
    public void listViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=bundles,name=root");
            TabularData bundles = (TabularData) connection.invoke(name, "list", new Object[]{ }, new String[]{ });
            assertTrue(bundles.size() > 0);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void classesCommand() throws Exception {
        String classesOutput = executeCommand("osgi:classes --force 10");
        System.out.println(classesOutput);
        assertFalse(classesOutput.isEmpty());
        classesOutput = executeCommand("osgi:classes --force 10");
        System.out.println(classesOutput);
        assertTrue(classesOutput.contains("org/apache/aries/proxy"));
    }

    @Test
    public void findClassCommand() throws Exception {
        String findClassOutput = executeCommand("osgi:find-class jmx");
        System.out.println(findClassOutput);
        assertFalse(findClassOutput.isEmpty());
    }

    @Test
    public void headersCommand() throws Exception {
        String headersOutput = executeCommand("osgi:headers --force 10");
        System.out.println(headersOutput);
        assertTrue(headersOutput.contains("Bundle-SymbolicName = org.apache.aries.proxy.api"));
    }

    @Test
    public void infoCommand() throws Exception {
        String infoOutput = executeCommand("osgi:info --force 10");
        System.out.println(infoOutput);
        assertTrue(infoOutput.contains("Apache Aries Proxy API"));
    }

    @Test
    public void installCommand() throws Exception {
        String installOutput = executeCommand("osgi:install mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.commons-lang/2.4_6");
        System.out.println(installOutput);
        String listOutput = executeCommand("osgi:list | grep -i commons-lang");
        assertFalse(listOutput.isEmpty());
    }

    @Test
    public void nameCommand() throws Exception {
        String nameOutput = executeCommand("osgi:name");
        System.out.println(nameOutput);
        assertEquals("root", nameOutput.trim());
    }

    @Test
    public void nameViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=system,name=root");
            String currentName = (String) connection.getAttribute(name, "Name");
            assertEquals("root", currentName);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void versionCommand() throws Exception {
        String versionOutput = executeCommand("osgi:version");
        System.out.println(versionOutput);
        assertTrue(versionOutput.contains("2.3"));
    }

    @Test
    public void versionViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=system,name=root");
            String version = (String) connection.getAttribute(name, "Version");
            assertTrue(version.contains("2.3"));
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void startLevelCommand() throws Exception {
        String startLevelOutput = executeCommand("osgi:start-level");
        System.out.println(startLevelOutput);
        assertTrue(startLevelOutput.contains("100"));
    }

    @Test
    public void shutdownCommand() throws Exception {
        System.out.println(executeCommand("osgi:shutdown -f"));
    }

}
