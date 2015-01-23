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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.net.Socket;
import java.net.URI;
import java.util.List;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class JmsTest extends KarafTestSupport {

    @Before
    public void installJmsFeatureAndActiveMQBroker() throws Exception {
        featuresService.installFeature("jms");
        assertFeatureInstalled("jms");
        featuresService.addRepository(new URI("mvn:org.apache.activemq/activemq-karaf/5.10.0/xml/features"));
        featuresService.installFeature("activemq-broker-noweb");
        assertFeatureInstalled("activemq-broker-noweb");
        // check if ActiveMQ is completely started
        System.out.println("Waiting for the ActiveMQ transport connector on 61616 ...");
        boolean bound = false;
        while (!bound) {
            try {
                Thread.sleep(2000);
                Socket socket = new Socket("localhost", 61616);
                bound = true;
            } catch (Exception e) {
                // wait the connection
            }
        }
    }

    @Test(timeout = 120000)
    public void testCommands() throws Exception {
        // jms:create command
        System.out.println(executeCommand("jms:create -t ActiveMQ -u karaf -p karaf --url tcp://localhost:61616 test"));
        // give time to fileinstall to load the blueprint file
        Thread.sleep(5000);
        // jms:connectionfactories command
        String connectionFactories = executeCommand("jms:connectionfactories");
        System.out.println(connectionFactories);
        assertTrue(connectionFactories.contains("jms/test"));
        // jms:info command
        String info = executeCommand("jms:info test");
        System.out.println(info);
        assertTrue(info.contains("ActiveMQ"));
        assertTrue(info.contains("5.10.0"));
        // jms:send command
        System.out.println(executeCommand("jms:send test queue message"));
        // jms:count command
        String count = executeCommand("jms:count test queue");
        System.out.println(count);
        assertTrue(count.contains("1"));
        // jms:consume command
        String consumed = executeCommand("jms:consume test queue");
        System.out.println(consumed);
        assertTrue(consumed.contains("1 message"));
        // jms:send & jms:move commands
        System.out.print(executeCommand("jms:send test queue message"));
        String move = executeCommand("jms:move test queue other");
        System.out.println(move);
        assertTrue(move.contains("1 message"));
        // jms:queues command
        String queues = executeCommand("jms:queues test");
        System.out.println(queues);
        assertTrue(queues.contains("queue"));
        assertTrue(queues.contains("other"));
        // jms:browse command
        String browse = executeCommand("jms:browse test other");
        System.out.println(browse);
        assertTrue(browse.contains("message"));
        assertTrue(browse.contains("queue://other"));
        // jms:consume command
        System.out.println(executeCommand("jms:consume test other"));
        // jms:delete command
        System.out.println(executeCommand("jms:delete test"));
        // jms:connectionfactories command
        connectionFactories = executeCommand("jms:connectionfactories");
        System.out.println(connectionFactories);
    }

    @Test(timeout = 120000)
    public void testMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=jms,name=root");
            // create operation
            System.out.println("JMS MBean create operation invocation");
            connection.invoke(name, "create", new String[]{ "testMBean", "activemq", "tcp://localhost:61616", "karaf", "karaf" }, new String[]{ "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
            Thread.sleep(5000);
            List<String> connectionFactories = (List<String>) connection.getAttribute(name, "Connectionfactories");
            assertEquals(true, connectionFactories.size() >= 1);
            // send operation
            System.out.println("JMS MBean send operation invocation");
            connection.invoke(name, "send", new String[]{ "testMBean", "queueMBean", "message", null, "karaf", "karaf" }, new String[]{ "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
            // count operation
            System.out.println("JMS MBean count operation invocation");
            Integer count = (Integer) connection.invoke(name, "count", new String[]{ "testMBean", "queueMBean", "karaf", "karaf" }, new String[]{ "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
            assertEquals(1, count.intValue());
            // queues operation
            System.out.print("JMS MBean queues operation invocation: ");
            List<String> queues = (List<String>) connection.invoke(name, "queues", new String[]{ "testMBean", "karaf", "karaf" }, new String[]{ "java.lang.String", "java.lang.String", "java.lang.String"});
            System.out.println(queues);
            assertTrue(queues.size() >= 1);
            // delete operation
            System.out.println("JMS MBean delete operation invocation");
            connection.invoke(name, "delete", new String[]{"testMBean"}, new String[]{"java.lang.String"});
        } finally {
            if (connector != null) {
                connector.close();
            }
        }
    }

}
