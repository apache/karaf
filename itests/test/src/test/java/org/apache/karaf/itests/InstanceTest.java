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

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.lang.management.ManagementFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class InstanceTest extends BaseTest {

    @Test
    public void createDestroyCommand() throws Exception {
        System.out.println(executeCommand("instance:create itest1"));
        assertContains("itest1" ,executeCommand("instance:list"));
        System.out.println(executeCommand("instance:destroy itest1"));
        assertContainsNot("itest1" ,executeCommand("instance:list"));
    }

    @Test
    public void createDestroyViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=instance,name=root");
        int oldNum = getInstancesNum(mbeanServer, name);
        mbeanServer.invoke(name, "createInstance", new Object[]{"itest2", 0, 0, 0, null, null, null, null},
                new String[]{"java.lang.String", "int", "int", "int", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
        Assert.assertEquals(oldNum + 1, getInstancesNum(mbeanServer, name));
        mbeanServer.invoke(name, "destroyInstance", new Object[]{"itest2"}, new String[]{"java.lang.String"});
        Assert.assertEquals(oldNum, getInstancesNum(mbeanServer, name));
    }

    @Test
    public void createStartStopDestroyCommand() throws Exception {
        System.out.println(executeCommand("instance:create itest666"));
        assertContains("itest", executeCommand("instance:list"));
        System.out.println(executeCommand("instance:start itest666"));
        String output = executeCommand("instance:status itest666");
        int i = 0;
        while (!output.contains("Started")) {
            if (i >= 10) {
                break;
            }
            i = i + 1;
            Thread.sleep(5000);
            output = executeCommand("instance:status itest666");
        }
        System.out.println("itest instance status: " + output);
        assertContains("Started", output);
        System.out.println(executeCommand("instance:stop itest666"));
        output = executeCommand("instance:status itest666");
        i = 0;
        while (!output.contains("Stopped")) {
            if (i >= 10) {
                break;
            }
            i = i + 1;
            Thread.sleep(5000);
            output = executeCommand("instance:status itest666");
        }
        System.out.println("itest instance status: " + output);
        assertContains("Stopped", output);
        executeCommand("instance:destroy itest666");
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
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=instance,name=root");
        int oldNum = getInstancesNum(mbeanServer, name);
        mbeanServer.invoke(name, "cloneInstance", new Object[]{"root", "itest4", 0, 0, 0, null, null},
                new String[]{"java.lang.String", "java.lang.String", "int", "int", "int", "java.lang.String", "java.lang.String"});
        Assert.assertEquals(oldNum + 1, getInstancesNum(mbeanServer, name));
    }

    @Test
    public void renameCommand() throws Exception {
        executeCommand("instance:create itest777");
        executeCommand("instance:rename itest777 new_itest");
        assertContains("new_itest", executeCommand("instance:list"));
    }

    @Test
    public void renameViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=instance,name=root");
        mbeanServer.invoke(name, "createInstance", new Object[]{"itest5", 0, 0, 0, null, null, null, null},
                new String[]{"java.lang.String", "int", "int", "int", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
        mbeanServer.invoke(name, "renameInstance", new Object[]{"itest5", "new_itest5"}, new String[]{"java.lang.String", "java.lang.String"});
    }

}
