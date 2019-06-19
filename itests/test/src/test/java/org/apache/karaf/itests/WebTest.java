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
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class WebTest extends KarafTestSupport {

    @Before
    public void installWarFeature() throws Exception {
        installAndAssertFeature("war");
    }
    
    @Test
    public void listCommand() throws Exception {
        String listOutput = executeCommand("web:list");
        System.out.println(listOutput);
        assertFalse(listOutput.isEmpty());
    }

    @Test
    public void listViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=web,name=root");
        TabularData webBundles = (TabularData) mbeanServer.getAttribute(name, "WebBundles");
        assertEquals(0, webBundles.size());
    }

    @Test
    public void installUninstallCommands() throws Exception {
        System.out.println(executeCommand("web:install mvn:org.apache.karaf.examples/karaf-war-example-webapp/" + System.getProperty("karaf.version") + "/war test"));
        String listOutput = executeCommand("web:list");
        System.out.println(listOutput);
        assertContains("/test", listOutput);
        while (!listOutput.contains("Deployed")) {
            Thread.sleep(500);
            listOutput = executeCommand("web:list");
        }
        URL url = new URL("http://localhost:" + getHttpPort() + "/test");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setRequestMethod("GET");
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
        }
        System.out.println(buffer.toString());
        assertContains("Hello World!", buffer.toString());

        System.out.println(executeCommand("web:uninstall 125"));
        listOutput = executeCommand("web:list");
        System.out.println(listOutput);
        while (listOutput.contains("/test")) {
            Thread.sleep(500);
            listOutput = executeCommand("web:list");
        }
        assertContainsNot("/test", listOutput);
    }

    @Test
    public void installViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=web,name=root");
        mbeanServer.invoke(name, "install", new Object[]{ "mvn:org.apache.karaf.examples/karaf-war-example-webapp/" + System.getProperty("karaf.version") + "/war", "test" }, new String[]{ String.class.getName(), String.class.getName() });
        TabularData webBundles = (TabularData) mbeanServer.getAttribute(name, "WebBundles");
        assertEquals(1, webBundles.size());

        Thread.sleep(2000);

        URL url = new URL("http://localhost:" + getHttpPort() + "/test");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setRequestMethod("GET");
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
        }
        System.out.println(buffer.toString());
        assertContains("Hello World!", buffer.toString());
    }

}
