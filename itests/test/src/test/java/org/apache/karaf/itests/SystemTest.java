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

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.lang.management.ManagementFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SystemTest extends BaseTest {

    private static final String KARAF_VERSION = "4";

    @Test
    public void nameCommand() throws Exception {
        String nameOutput = executeCommand("system:name");
        System.out.println(nameOutput);
        assertEquals("root", nameOutput.trim());
    }

    @Test
    public void nameViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=system,name=root");
        String currentName = (String) mbeanServer.getAttribute(name, "Name");
        assertEquals("root", currentName);
    }

    @Test
    public void versionCommand() throws Exception {
        assertContains(KARAF_VERSION, executeCommand("system:version"));
    }

    @Test
    public void versionViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=system,name=root");
        assertContains(KARAF_VERSION, (String) mbeanServer.getAttribute(name, "Version"));
    }

    @Test
    public void frameworkCommand() throws Exception {
        assertContains("felix", executeCommand("system:framework"));
    }

    @Test
    public void frameworkViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=system,name=root");
        String framework = (String) mbeanServer.getAttribute(name, "Framework");
        assertEquals("felix", framework);
    }

    @Test
    public void startLevelCommand() throws Exception {
        assertContains("100", executeCommand("system:start-level",
                new RolePrincipal("admin"), new RolePrincipal("manager"), new RolePrincipal("viewer")));
    }

    @Test
    public void startLevelViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=system,name=root");
        int startLevel = (Integer) mbeanServer.getAttribute(name, "StartLevel");
        assertEquals(100, startLevel);
    }

}
