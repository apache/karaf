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

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import javax.management.MBeanServer;
import javax.management.ObjectName;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class JmsTest extends BaseTest {

    @Configuration
    public Option[] config() {
        String version = MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf");
        List<Option> options = new LinkedList<>(Arrays.asList(super.config()));
        options.add(editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresRepositories",
                "mvn:org.apache.karaf.features/framework/" + version + "/xml/features, " +
                        "mvn:org.apache.karaf.features/spring/" + version + "/xml/features, " +
                        "mvn:org.apache.karaf.features/spring-legacy/" + version + "/xml/features, " +
                        "mvn:org.apache.karaf.features/enterprise/" + version + "/xml/features, " +
                        "mvn:org.apache.karaf.features/standard/" + version + "/xml/features, " +
                        "mvn:org.apache.activemq/activemq-karaf/" + System.getProperty("activemq.version") + "/xml/features"));
        return options.toArray(new Option[options.size()]);
    }

    @Test(timeout = 60000)
    public void testCommands() throws Exception {
        System.out.println("== Installing ActiveMQ");
        featureService.installFeature("aries-blueprint");
        featureService.installFeature("activemq-broker-noweb");

        System.out.println("== Installing JMS feature");
        featureService.installFeature("jms");
        featureService.installFeature("pax-jms-activemq");

        System.out.println("== Creating JMS ConnectionFactory");
        executeCommand("jms:create test");
        Thread.sleep(2000);
        String output = executeCommand("jms:connectionfactories");
        System.out.println(output);
        assertContains("jms/test", output);

        output = executeCommand("jms:info jms/test");
        System.out.println(output);
        assertContains("ActiveMQ", output);

        executeCommand("jms:send jms/test queue message");
        output = executeCommand("jms:count jms/test queue");
        System.out.println(output);
        assertContains("1", output);

        output = executeCommand("jms:consume jms/test queue");
        System.out.println(output);
        assertContains("1 message", output);

        executeCommand("jms:send test queue message");
        output = executeCommand("jms:move test queue other");
        System.out.println(output);
        assertContains("1 message", output);

        output = executeCommand("jms:queues test");
        System.out.println(output);
        assertContains("queue", output);
        assertContains("other", output);

        output = executeCommand("jms:browse test other");
        System.out.println(output);
        assertContains("queue", output);
        assertContains("queue://other", output);

        System.out.println(executeCommand("jms:consume test other"));
        System.out.println(executeCommand("jms:delete test"));
    }

    @Test
    public void testMBean() throws Exception {
        System.out.println("== Installing ActiveMQ");
        featureService.installFeature("aries-blueprint");
        featureService.installFeature("activemq-broker-noweb");

        System.out.println("== Installing JMS feature");
        featureService.installFeature("jms");
        featureService.installFeature("pax-jms-activemq");

        Thread.sleep(2000);

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName = new ObjectName("org.apache.karaf:type=jms,name=root");

        mBeanServer.invoke(objectName, "create",
                new String[]{ "testMBean", "activemq", "tcp://localhost:61616", "karaf", "karaf", "transx" },
                new String[]{ "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });

        Thread.sleep(2000);

        mBeanServer.invoke(objectName, "send",
                new String[]{ "jms/testMBean", "queueMBean", "message", null, "karaf", "karaf"},
                new String[]{ "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });

        Integer count = (Integer) mBeanServer.invoke(objectName, "count",
                new String[]{ "jms/testMBean", "queueMBean", "karaf", "karaf"},
                new String[]{ "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
        Assert.assertEquals((Integer) 1, count);

        List<String> queues = (List<String>) mBeanServer.invoke(objectName, "queues",
                new String[]{ "jms/testMBean", "karaf", "karaf"},
                new String[]{ "java.lang.String", "java.lang.String", "java.lang.String" });
        Assert.assertTrue(queues.contains("queueMBean"));

        mBeanServer.invoke(objectName, "delete",
                new String[]{ "testMBean"},
                new String[]{ "java.lang.String"});
    }

}
