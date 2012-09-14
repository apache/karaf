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
public class AdminTest extends KarafTestSupport {

    @Test
    public void createDestroyCommand() throws Exception {
        System.out.println(executeCommand("admin:create itest"));
        String adminListOutput = executeCommand("admin:list");
        System.out.println(adminListOutput);
        assertTrue(adminListOutput.contains("itest"));
        System.out.println(executeCommand("admin:destroy itest"));
        adminListOutput = executeCommand("admin:list");
        System.out.println(adminListOutput);
        assertFalse(adminListOutput.contains("itest"));
    }

    @Test
    public void createDestroyViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=admin,name=root");
            connection.invoke(name, "createInstance", new Object[]{ "itest", 0, 0, 0, null, null, null, null },
                    new String[]{ "java.lang.String", "int", "int", "int", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
            TabularData instances = (TabularData) connection.getAttribute(name, "Instances");
            assertEquals(instances.size(), 2);
            connection.invoke(name, "destroyInstance", new Object[]{ "itest" }, new String[]{ "java.lang.String" });
            instances = (TabularData) connection.getAttribute(name, "Instances");
            assertEquals(instances.size(), 1);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void cloneCommand() throws Exception {
        System.out.println(executeCommand("admin:clone root itest"));
        String adminListOutput = executeCommand("admin:list");
        System.out.println(adminListOutput);
        assertTrue(adminListOutput.contains("itest"));
    }

    @Test
    public void cloneViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=admin,name=root");
            connection.invoke(name, "cloneInstance", new Object[]{ "root", "itest", 0, 0, 0, null, null },
                    new String[]{ "java.lang.String", "java.lang.String", "int", "int", "int", "java.lang.String", "java.lang.String" });
            TabularData instances = (TabularData) connection.getAttribute(name, "Instances");
            assertEquals(instances.size(), 2);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void renameCommand() throws Exception {
        System.out.println(executeCommand("admin:create itest"));
        System.out.println(executeCommand("admin:rename itest new_itest"));
        String instanceListOutput = executeCommand("instance:list");
        System.out.println(instanceListOutput);
        assertTrue(instanceListOutput.contains("new_itest"));
    }

}
