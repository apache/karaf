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

import static org.junit.Assert.assertTrue;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class InstanceTest extends KarafTestSupport {

    @Test
    public void createDestroyCommand() throws Exception {
        System.out.println(executeCommand("instance:create itest1"));
        assertContains("itest1" ,executeCommand("instance:list"));
        System.out.println(executeCommand("instance:destroy itest1"));
        assertContainsNot("itest1" ,executeCommand("instance:list"));
    }

    @Test
    public void createDestroyViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=instance,name=root");
            int oldNum = getInstancesNum(connection, name);  
            connection.invoke(name, "createInstance", new Object[]{ "itest2", 0, 0, 0, null, null, null, null },
                    new String[]{ "java.lang.String", "int", "int", "int", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
            Assert.assertEquals(oldNum + 1, getInstancesNum(connection, name));
            connection.invoke(name, "destroyInstance", new Object[]{ "itest2" }, new String[]{ "java.lang.String" });
            Assert.assertEquals(oldNum, getInstancesNum(connection, name));
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    private int getInstancesNum(MBeanServerConnection connection, ObjectName name) throws Exception {
        TabularData instances = (TabularData) connection.getAttribute(name, "Instances");
        return instances.size();
    }

    @Test
    public void cloneCommand() throws Exception {
        System.out.println(executeCommand("instance:clone root itest3"));
        assertContains("itest3", executeCommand("instance:list"));
    }

    @Test
    public void cloneViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=instance,name=root");
            int oldNum = getInstancesNum(connection, name);
            connection.invoke(name, "cloneInstance", new Object[]{ "root", "itest4", 0, 0, 0, null, null },
                    new String[]{ "java.lang.String", "java.lang.String", "int", "int", "int", "java.lang.String", "java.lang.String" });
            Assert.assertEquals(oldNum + 1, getInstancesNum(connection, name));
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void renameCommand() throws Exception {
        System.out.println(executeCommand("instance:create itest"));
        System.out.println(executeCommand("instance:rename itest new_itest"));
        String instanceListOutput = executeCommand("instance:list");
        System.out.println(instanceListOutput);
        assertTrue(instanceListOutput.contains("new_itest"));
    }

}
