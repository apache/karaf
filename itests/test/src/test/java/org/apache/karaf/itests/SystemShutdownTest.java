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
import javax.management.ObjectName;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import java.lang.management.ManagementFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SystemShutdownTest extends BaseTest {

    @Test
    public void shutdownCommand() throws Exception {
        System.out.println(executeCommand("system:shutdown -f", new RolePrincipal("admin")));
    }

    @Test
    public void haltAlias() throws Exception {
        System.out.println(executeAlias("halt", new RolePrincipal("admin")));
    }

    @Test
    public void shutdownViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=system,name=root");
        mbeanServer.invoke(name, "halt", new Object[]{}, new String[]{});
    }

}
