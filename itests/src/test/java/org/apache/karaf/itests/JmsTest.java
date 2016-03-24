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
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.net.URI;
import java.util.List;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JmsTest extends KarafTestSupport {
    
    @Before
    public void installJmsFeatureAndActiveMQBroker() throws Exception {
        installAndAssertFeature("jms");
        featureService
            .addRepository(new URI("mvn:org.apache.activemq/activemq-karaf/5.10.0/xml/features"));
        installAndAssertFeature("activemq-broker-noweb");
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
        System.out.println("===>testCommands");
        // jms:create command
        System.out.println(executeCommand("jms:create -t ActiveMQ -u karaf -p karaf --url tcp://localhost:61616 test"));
        // give time to fileinstall to load the blueprint file by looking for the connection factory OSGi service
        getOsgiService(ConnectionFactory.class, "name=test" , 30000);
        // jms:connectionfactories command
        String connectionFactories = executeCommand("jms:connectionfactories");
        System.out.println(connectionFactories);
        assertContains("jms/test", connectionFactories);
        // jms:info command
        String info = executeCommand("jms:info test");
        System.out.println(info);
        assertContains("ActiveMQ", info);
        assertContains("5.10.0", info);
        // jms:send command
        System.out.println(executeCommand("jms:send test queue message"));
        // jms:count command
        String count = executeCommand("jms:count test queue");
        System.out.println(count);
        assertContains("1", count);
        // jms:consume command
        String consumed = executeCommand("jms:consume test queue");
        System.out.println(consumed);
        assertContains("1 message", consumed);
        // jms:send & jms:move commands
        System.out.print(executeCommand("jms:send test queue message"));
        String move = executeCommand("jms:move test queue other");
        System.out.println(move);
        assertContains("1 message", move);
        // jms:queues command
        String queues = executeCommand("jms:queues test");
        System.out.println(queues);
        assertContains("queue", queues);
        assertContains("other", queues);
        // jms:browse command
        String browse = executeCommand("jms:browse test other");
        System.out.println(browse);
        assertContains("message", browse);
        assertContains("queue://other", browse);
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
        System.out.println("===>testMBean");
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=jms,name=root");
        // create operation
        System.out.println("JMS MBean create operation invocation");
        mbeanServer.invoke(name, "create", new String[]{"testMBean", "activemq", "tcp://localhost:61616", "karaf", "karaf"}, new String[]{"java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
        // give time to fileinstall to load the blueprint file by looking for the connection factory OSGi service
        getOsgiService(ConnectionFactory.class, "name=testMBean", 30000);
        List<String> connectionFactories = (List<String>) mbeanServer.getAttribute(name, "Connectionfactories");
        assertEquals(true, connectionFactories.size() >= 1);
        // send operation
        System.out.println("JMS MBean send operation invocation");
        mbeanServer.invoke(name, "send", new String[]{"testMBean", "queueMBean", "message", null, "karaf", "karaf"}, new String[]{"java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
        // count operation
        System.out.println("JMS MBean count operation invocation");
        Integer count = (Integer) mbeanServer.invoke(name, "count", new String[]{"testMBean", "queueMBean", "karaf", "karaf"}, new String[]{"java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});
        assertEquals(1, count.intValue());
        // queues operation
        System.out.print("JMS MBean queues operation invocation: ");
        List<String> queues = (List<String>) mbeanServer.invoke(name, "queues", new String[]{"testMBean", "karaf", "karaf"}, new String[]{"java.lang.String", "java.lang.String", "java.lang.String"});
        System.out.println(queues);
        assertTrue(queues.size() >= 1);
        // delete operation
        System.out.println("JMS MBean delete operation invocation");
        mbeanServer.invoke(name, "delete", new String[]{"testMBean"}, new String[]{"java.lang.String"});
    }

}
