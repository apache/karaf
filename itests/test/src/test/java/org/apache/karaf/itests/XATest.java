/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.util.RunIfRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class XATest extends BaseTest {

    private static final EnumSet<FeaturesService.Option> NO_AUTO_REFRESH = EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles);

    @Rule
    public RunIfRule rule = new RunIfRule();

    @Configuration
    public Option[] config() {
        String version = MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf");
        List<Option> result = new LinkedList<>(Arrays.asList(super.config()));
        result.add(editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresRepositories",
                "mvn:org.apache.karaf.features/framework/" + version + "/xml/features, " +
                "mvn:org.apache.karaf.features/enterprise/" + version + "/xml/features, " +
                "mvn:org.apache.karaf.features/standard/" + version + "/xml/features, " +
                "mvn:org.apache.activemq/artemis-features/2.6.0/xml/features, " +
                "mvn:org.apache.camel.karaf/apache-camel/2.20.1/xml/features"
            ));
        result.add(editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresBoot",
                "instance,package,log,ssh,framework,system,eventadmin,feature,shell,management,service,jaas,deployer,diagnostic,wrap,bundle,config,kar,aries-blueprint,artemis,jms,pax-jms-artemis"));
        result.add(replaceConfigurationFile("etc/org.ops4j.connectionfactory-artemis.cfg", getConfigFile("/org/apache/karaf/itests/features/org.ops4j.connectionfactory-artemis.cfg")));
        result.add(replaceConfigurationFile("etc/org.ops4j.datasource-h2.cfg", getConfigFile("/org/apache/karaf/itests/features/org.ops4j.datasource-h2.cfg")));
        result.add(replaceConfigurationFile("etc/xa-test-camel.xml", getConfigFile("/org/apache/karaf/itests/features/xa-test-camel.xml")));
        return result.toArray(new Option[result.size()]);
    }

    private static final long TIMEOUT_MS = 120_000;

    private String awaitCondition(String command, String expected, String description) throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        String output = executeCommand(command);
        while (!output.contains(expected)) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Timeout waiting for " + description
                        + ". Last output: " + output);
            }
            Thread.sleep(500);
            output = executeCommand(command);
        }
        return output;
    }

    @Ignore("Camel 2.20.1 requires spring-tx [4.1.0,5.0.0) which is not available after spring-legacy removal")
    @Test
    public void test() throws Exception {
        System.out.println("== Starting Artemis broker == ");
        awaitCondition("log:display", "AMQ221007: Server is now live", "Artemis broker to start");
        System.out.println("AMQ221007: Server is now live");
        System.out.println(executeCommand("jms:info artemis"));

        System.out.println("== Installing H2 database == ");
        featureService.installFeature("jdbc", NO_AUTO_REFRESH);
        featureService.installFeature("pax-jdbc-h2", NO_AUTO_REFRESH);
        featureService.installFeature("pax-jdbc-pool-transx", NO_AUTO_REFRESH);

        System.out.println(" ");
        String dsList = awaitCondition("jdbc:ds-list", "OK", "H2 datasource to become available");
        System.out.println(dsList);

        System.out.println("== Creating table in H2 ==");
        System.out.println(executeCommand("jdbc:execute h2 CREATE TABLE IF NOT EXISTS messages (id INTEGER GENERATED BY DEFAULT AS IDENTITY NOT NULL, message VARCHAR(1024) NOT NULL, CONSTRAINT primary_key PRIMARY KEY (id))"));

        awaitCondition("jdbc:query h2 select * from messages", "MESSAGE", "table creation");
        System.out.println("== Table created ==");

        System.out.println("== Installing Camel route ==");
        featureService.installFeature("camel-blueprint", NO_AUTO_REFRESH);
        featureService.installFeature("camel-sql", NO_AUTO_REFRESH);
        featureService.installFeature("camel-jms", NO_AUTO_REFRESH);

        System.out.println("== Starting Narayana TX Manager == ");
        featureService.installFeature("transaction-manager-narayana");
        assertNotNull(getOsgiService("org.jboss.narayana.osgi.jta.ObjStoreBrowserService", null, 30000l));

        Bundle bundle = bundleContext.installBundle("blueprint:file:etc/xa-test-camel.xml");
        bundle.start();

        String routeList = awaitCondition("camel:route-list", "Started", "Camel route to start");
        System.out.println(routeList);

        System.out.println("== Sending a message in Artemis broker that should be consumed by Camel route and inserted into the H2 database");
        System.out.println(executeCommand("jms:send artemis MyQueue 'the-message'"));

        String output = awaitCondition("jdbc:query h2 select * from messages", "the-message",
                "message to be inserted into H2");

        System.out.println(output);

        assertContains("the-message", output);

    }

}
