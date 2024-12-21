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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class InstanceTest extends BaseTest {

    private String generateRandomInstanceName() {
        return "instance-" + UUID.randomUUID();
    }

    @Test
    public void createDestroyCommand() throws Exception {
        String instanceName = generateRandomInstanceName();
        System.out.println(executeCommand("instance:create " + instanceName));
        assertContains(instanceName, executeCommand("instance:list"));
        System.out.println(executeCommand("instance:destroy " + instanceName));
        assertContainsNot(instanceName, executeCommand("instance:list"));
    }

    @Test
    public void createDestroyViaMBean() throws Exception {
        String instanceName = generateRandomInstanceName();
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=instance,name=root");
        int oldNum = getInstancesNum(mbeanServer, name);
        mbeanServer.invoke(name, "createInstance", new Object[]{instanceName, 0, 0, 0, null, null, null, null},
                new String[]{"java.lang.String", "int", "int", "int", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
        Assert.assertEquals(oldNum + 1, getInstancesNum(mbeanServer, name));
        mbeanServer.invoke(name, "destroyInstance", new Object[]{instanceName}, new String[]{"java.lang.String"});
        Assert.assertEquals(oldNum, getInstancesNum(mbeanServer, name));
    }

    @Test
    public void createStartStopDestroyCommand() throws Exception {
        String instanceName = generateRandomInstanceName();
        System.out.println(executeCommand("instance:create " + instanceName));
        assertContains(instanceName, executeCommand("instance:list"));
        System.out.println(executeCommand("instance:start " + instanceName));
        String output = executeCommand("instance:status " + instanceName);
        int i = 0;
        while (!output.contains("Started")) {
            if (i >= 10) {
                break;
            }
            i = i + 1;
            Thread.sleep(5000);
            output = executeCommand("instance:status " + instanceName);
        }
        System.out.println("itest instance status: " + output);
        assertContains("Started", output);
        System.out.println(executeCommand("instance:stop " + instanceName));
        output = executeCommand("instance:status " + instanceName);
        i = 0;
        while (!output.contains("Stopped")) {
            if (i >= 10) {
                break;
            }
            i = i + 1;
            Thread.sleep(5000);
            output = executeCommand("instance:status " + instanceName);
        }
        System.out.println("itest instance status: " + output);
        assertContains("Stopped", output);
        executeCommand("instance:destroy " + instanceName);
    }

    @Test
    public void packageCommand() throws Exception {
        String instanceName = generateRandomInstanceName();
        executeCommand("instance:create " + instanceName);
        executeCommand("instance:package " + instanceName + " archive.zip");
        String zipPath = Paths.get(System.getProperty("karaf.home"), "archive.zip").toString();
        ZipFile zipFile = new ZipFile(zipPath);

        List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
        assertFalse(entries.isEmpty());
        assertTrue(entries.stream().anyMatch(e -> e.getName().equals("bin/karaf")));
        assertTrue(entries.stream().anyMatch(e -> e.getName().equals("etc/system.properties")));

        executeCommand("instance:destroy " + instanceName);
    }

    private int getInstancesNum(MBeanServerConnection connection, ObjectName name) throws Exception {
        TabularData instances = (TabularData) connection.getAttribute(name, "Instances");
        return instances.size();
    }

    @Test
    public void cloneCommand() throws Exception {
        String instanceName = generateRandomInstanceName();
        System.out.println(executeCommand("instance:clone root " + instanceName));
        assertContains(instanceName, executeCommand("instance:list"));
    }

    @Test
    public void cloneViaMBean() throws Exception {
        String instanceName = generateRandomInstanceName();
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=instance,name=root");
        int oldNum = getInstancesNum(mbeanServer, name);
        mbeanServer.invoke(name, "cloneInstance", new Object[]{"root", instanceName, 0, 0, 0, null, null},
                new String[]{"java.lang.String", "java.lang.String", "int", "int", "int", "java.lang.String", "java.lang.String"});
        Assert.assertEquals(oldNum + 1, getInstancesNum(mbeanServer, name));
    }

    @Test
    public void renameCommand() throws Exception {
        String instanceName = generateRandomInstanceName();
        executeCommand("instance:create " + instanceName);
        executeCommand("instance:rename " + instanceName + " new_" + instanceName);
        assertContains("new_" + instanceName, executeCommand("instance:list"));
    }

    @Test
    public void renameViaMBean() throws Exception {
        String instanceName = generateRandomInstanceName();
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=instance,name=root");
        mbeanServer.invoke(name, "createInstance", new Object[]{instanceName, 0, 0, 0, null, null, null, null},
                new String[]{"java.lang.String", "int", "int", "int", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
        mbeanServer.invoke(name, "renameInstance", new Object[]{instanceName, "new_" + instanceName}, new String[]{"java.lang.String", "java.lang.String"});
    }
}
