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
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import javax.jms.ConnectionFactory;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.net.URI;
import java.util.List;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JmsTest extends KarafTestSupport {

    @Before
    public void installJmsFeatureAndActiveMQBroker() throws Exception {
        installAndAssertFeature("jms");
        featuresService.addRepository(new URI("mvn:org.apache.activemq/activemq-karaf/5.10.0/xml/features"));
        installAndAssertFeature("activemq-broker-noweb");
    }

    @Test
    public void testCommands() throws Exception {
        // jms:create command
        System.out.println(executeCommand("jms:create -t ActiveMQ -u karaf -p karaf --url tcp://localhost:61616 test"));
        // give time to fileinstall to load the blueprint file
        getOsgiService(ConnectionFactory.class, "name=test", 30000);
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

    @Test
    public void testMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=jms,name=root");
            // create operation
            connection.invoke(name, "create", new String[]{ "testMBean", "activemq", "tcp://localhost:61616", "karaf", "karaf" }, new String[]{ "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
            getOsgiService(ConnectionFactory.class, "name=testMBean", 30000);
            List<String> connectionFactories = (List<String>) connection.getAttribute(name, "Connectionfactories");
            assertEquals(true, connectionFactories.size() >= 1);
            // send operation
            connection.invoke(name, "send", new String[]{ "testMBean", "queueMBean", "message", null, "karaf", "karaf" }, new String[]{ "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
            // count operation
            Integer count = (Integer) connection.invoke(name, "count", new String[]{ "testMBean", "queueMBean", "karaf", "karaf" }, new String[]{ "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
            assertEquals(1, count.intValue());
            // queues operation
            List<String> queues = (List<String>) connection.invoke(name, "queues", new String[]{ "testMBean", "karaf", "karaf" }, new String[]{ "java.lang.String", "java.lang.String", "java.lang.String"});
            assertTrue(queues.size() >= 1);
            // delete operation
            connection.invoke(name, "delete", new String[]{ "testMBean" }, new String[]{ "java.lang.String" });
        } finally {
            if (connector != null) {
                connector.close();
            }
        }
    }

}
