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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import javax.jms.ConnectionFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JmsTest extends KarafTestSupport {
    private static final String JMX_CF_NAME = "testMBean";
    private static final String JMX_QUEUE_NAME = "queueMBean";
    private MBeanServer mbeanServer;
    private ObjectName objName;
    private String activemqVersion;

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference activeMqUrl = maven().groupId("org.apache.activemq")
            .artifactId("activemq-karaf").versionAsInProject().type("xml").classifier("features");
        return new Option[] //
        {
         composite(super.config()), //
         features(activeMqUrl, "jms", "activemq-broker-noweb")
        };
    }

    @Before
    public void setup() throws Exception {
        await("ActiveMQ transport up").atMost(30, SECONDS).until(this::jmsTransportPresent);
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
        objName = new ObjectName("org.apache.karaf:type=jms,name=root");
        activemqVersion = System.getProperty("activemq.version");
    }

    @Test(timeout = 60000)
    public void testCommands() throws Exception {
        execute("jms:create -t activemq -u karaf -p karaf --url tcp://localhost:61616 test");
        waitForConnectionFactory("name=test");

        assertThat(execute("jms:connectionfactories"), containsString("jms/test"));
        assertThat(execute("jms:info test"), both(containsString("ActiveMQ")).and(containsString(activemqVersion)));

        execute("jms:send test queue message");
        assertThat(execute("jms:count test queue"), containsString("1"));
        assertThat(execute("jms:consume test queue"), containsString("1 message"));

        execute("jms:send test queue message");
        assertThat(execute("jms:move test queue other"), containsString("1 message"));

        assertThat(execute("jms:queues test"), both(containsString("queue")).and(containsString("other")));
        assertThat(execute("jms:browse test other"),
                   both(containsString("queue")).and(containsString("queue://other")));
        execute("jms:consume test other");
        execute("jms:delete test");
    }

    @Test(timeout = 60000)
    public void testMBean() throws Exception {
        checkJMXCreateConnectionFactory();

        invoke("send", JMX_CF_NAME, JMX_QUEUE_NAME, "message", null, "karaf", "karaf");
        Integer count = invoke("count", JMX_CF_NAME, JMX_QUEUE_NAME, "karaf", "karaf");
        assertTrue("Queue size > 0", count > 0);

        List<String> queues = invoke("queues", JMX_CF_NAME, "karaf", "karaf");
        assertThat(queues, hasItem(JMX_QUEUE_NAME));

        invoke("delete", JMX_CF_NAME);
    }

    public boolean jmsTransportPresent() throws IOException {
        try (Socket socket = new Socket("localhost", 61616)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String execute(String command) {
        String output = executeCommand(command);
        System.out.println(output);
        return output;
    }

    private void checkJMXCreateConnectionFactory() throws Exception {
        invoke("create", JMX_CF_NAME, "activemq", "tcp://localhost:61616", "karaf", "karaf");
        waitForConnectionFactory("name=" + JMX_CF_NAME);
        @SuppressWarnings("unchecked")
        List<String> connectionFactories = (List<String>)mbeanServer.getAttribute(objName,
                                                                                  "Connectionfactories");
        assertTrue(connectionFactories.size() > 0);
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(String operationName, String... parameters) throws Exception {
        String[] types = new String[parameters.length];
        Arrays.fill(types, String.class.getName());
        System.out.println("Invoking jmx call " + operationName);
        return (T)mbeanServer.invoke(objName, operationName, parameters, types);
    }

    /**
     * Give fileinstall some time to load the blueprint file by looking for the connection factory OSGi
     * service
     */
    private ConnectionFactory waitForConnectionFactory(String filter) {
        return getOsgiService(ConnectionFactory.class, filter, 30000);
    }

}
