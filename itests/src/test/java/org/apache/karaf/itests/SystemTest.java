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

import static org.junit.Assert.assertEquals;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class SystemTest extends KarafTestSupport {

    private static final String KARAF_VERSION = "3";

    @Test
    public void nameCommand() throws Exception {
        String nameOutput = executeCommand("system:name");
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
        assertContains(KARAF_VERSION, executeCommand("system:version"));
    }

    @Test
    public void versionViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=system,name=root");
            assertContains(KARAF_VERSION, (String) connection.getAttribute(name, "Version"));
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void frameworkCommand() throws Exception {
        assertContains("felix", executeCommand("system:framework"));
    }

    @Test
    public void frameworkViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=system,name=root");
            String framework = (String) connection.getAttribute(name, "Framework");
            assertEquals("felix", framework);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void startLevelCommand() throws Exception {
        assertContains("100", executeCommand("system:start-level"));
    }

    @Test
    public void startLevelViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=system,name=root");
            int startLevel = (Integer) connection.getAttribute(name, "StartLevel");
            assertEquals(100, startLevel);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

}
