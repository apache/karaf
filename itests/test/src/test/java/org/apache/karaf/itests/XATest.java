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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
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
                "mvn:org.apache.karaf.features/spring-legacy/" + version + "/xml/features, " +
                "mvn:org.apache.karaf.features/standard/" + version + "/xml/features, " +
                "mvn:org.apache.activemq/artemis-features/2.6.0/xml/features, " +
                "mvn:org.apache.camel.karaf/apache-camel/2.20.1/xml/features"
            ));
        result.add(editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresBoot",
                "instance,package,log,ssh,framework,system,eventadmin,feature,shell,management,service,jaas,deployer,diagnostic,wrap,bundle,config,kar,aries-blueprint,artemis,jms,pax-jms-artemis"));
        result.add(replaceConfigurationFile("etc/org.ops4j.connectionfactory-artemis.cfg", getConfigFile("/org/apache/karaf/itests/features/org.ops4j.connectionfactory-artemis.cfg")));
        result.add(replaceConfigurationFile("etc/org.ops4j.datasource-derby.cfg", getConfigFile("/org/apache/karaf/itests/features/org.ops4j.datasource-derby.cfg")));
        result.add(replaceConfigurationFile("etc/xa-test-camel.xml", getConfigFile("/org/apache/karaf/itests/features/xa-test-camel.xml")));
        return result.toArray(new Option[result.size()]);
    }

    @Test
    public void test() throws Exception {
        System.out.println("== Starting Artemis broker == ");
        String logDisplay = executeCommand("log:display");
        while (!logDisplay.contains("AMQ221007: Server is now live")) {
            Thread.sleep(500);
            logDisplay = executeCommand("log:display");
        }
        System.out.println("AMQ221007: Server is now live");
        System.out.println(executeCommand("jms:info artemis"));

        System.out.println("== Installing Derby database == ");
        featureService.installFeature("jdbc", NO_AUTO_REFRESH);
        featureService.installFeature("pax-jdbc-derby", NO_AUTO_REFRESH);
        featureService.installFeature("pax-jdbc-pool-transx", NO_AUTO_REFRESH);

        System.out.println(" ");
        String dsList = executeCommand("jdbc:ds-list");
        while (!dsList.contains("OK")) {
            Thread.sleep(500);
            dsList = executeCommand("jdbc:ds-list");
        }
        System.out.println(dsList);
        
        System.out.println("== Creating table in Derby ==");
        System.out.println(executeCommand("jdbc:execute derby CREATE TABLE messages (id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY, message VARCHAR(1024) NOT NULL, CONSTRAINT primary_key PRIMARY KEY (id))"));

        String tableOutput = executeCommand("jdbc:query derby select * from messages");
        while (!tableOutput.contains("MESSAGE")) {
            Thread.sleep(500);
            tableOutput = executeCommand("jdbc:query derby select * from messages");;
        }
        System.out.println("== Table created ==");

        System.out.println("== Installing Camel route ==");
        featureService.installFeature("camel-blueprint", NO_AUTO_REFRESH);
        featureService.installFeature("camel-sql", NO_AUTO_REFRESH);
        featureService.installFeature("camel-jms", NO_AUTO_REFRESH);

        featureService.installFeature("transaction-manager-narayana");

        Bundle bundle = bundleContext.installBundle("blueprint:file:etc/xa-test-camel.xml");
        bundle.start();

        String routeList = executeCommand("camel:route-list");
        while (!routeList.contains("Started")) {
            Thread.sleep(500);
            routeList = executeCommand("camel:route-list");
        }
        System.out.println(routeList);

        System.out.println("== Sending a message in Artemis broker that should be consumed by Camel route and inserted into the Derby database");
        System.out.println(executeCommand("jms:send artemis MyQueue 'the-message'"));

        String output = executeCommand("jdbc:query derby select * from messages");

        while (!output.contains("the-message")) {
            Thread.sleep(500);
            output = executeCommand("jdbc:query derby select * from messages");
        }

        System.out.println(output);

        assertContains("the-message", output);

    }

}
