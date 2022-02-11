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

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;



import java.lang.management.ManagementFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class HttpTest extends BaseTest {

    @Before
    public void installHttpFeature() throws Exception {
        installAndAssertFeature("http");
        installAndAssertFeature("pax-web-karaf");
        installAndAssertFeature("webconsole");
    }
    
    @Test
    public void list() throws Exception {
        waitForService("(objectClass=javax.servlet.ServletContext)", 5000);
        assertContains("/system/console", executeCommand("web:servlet-list"));
    }

    @Test
    public void listViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=http,name=root");
        TabularData servlets = (TabularData) mbeanServer.getAttribute(name, "Servlets");
        assertTrue(servlets.size() > 0);
    }

    @Test
    public void testProxy() throws Exception {
        executeCommand("http:proxy-add /test1 http://karaf.apache.org");

        String output = executeCommand("http:proxy-balancing-list");
        System.out.println(output);
        assertContains("random", output);
        assertContains("round-robin", output);

        output = executeCommand("http:proxy-list");
        System.out.println(output);
        assertContains("/test1", output);
    }

}
