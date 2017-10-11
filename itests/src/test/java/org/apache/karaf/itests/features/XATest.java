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
package org.apache.karaf.itests.features;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class XATest extends KarafTestSupport {

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
                "mvn:org.apache.karaf.features/spring/" + version + "/xml/features, " +
                "mvn:org.apache.karaf.features/standard/" + version + "/xml/features, " +
                "mvn:org.apache.activemq/artemis-features/2.2.0/xml/features, " +
                "mvn:org.apache.camel.karaf/apache-camel/2.19.2/xml/features"
            ));
        result.add(replaceConfigurationFile("etc/org.ops4j.connectionfactory-artemis.cfg", getConfigFile("/org/apache/karaf/itests/features/org.ops4j.connectionfactory-artemis.cfg")));
        result.add(replaceConfigurationFile("etc/org.ops4j.datasource-derby.cfg", getConfigFile("/org/apache/karaf/itests/features/org.ops4j.datasource-derby.cfg")));
        result.add(replaceConfigurationFile("etc/xa-test-camel.xml", getConfigFile("/org/apache/karaf/itests/features/xa-test-camel.xml")));
        if (System.getProperty("java.version").startsWith("9")) {
            //need asm 6.x which support java9 to run this test
            result.add(replaceConfigurationFile("system/org/apache/karaf/features/standard/" 
                + version + "/standard-" + version + "-features.xml", 
                getConfigFile("/etc/feature.xml")));
        }
        return result.toArray(new Option[result.size()]);
    }

    @Test
    public void installFeature() throws Exception {
        featureService.installFeatures(asSet(
                "transaction",
                "transaction-manager-narayana",
                "artemis",
                "pax-jms-pool",
                "jms",
                "pax-jdbc-derby",
                "pax-jdbc-pool-transx",
                "jdbc",
                "shell-compat",
                "camel-blueprint",
                "camel-spring",
                "camel-sql",
                "camel-jms"), NO_AUTO_REFRESH);

        Bundle bundle = bundleContext.installBundle("blueprint:file:etc/xa-test-camel.xml");
        bundle.start();

        executeCommand("jdbc:execute derby CREATE TABLE messages (id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY, message VARCHAR(1024) NOT NULL, CONSTRAINT primary_key PRIMARY KEY (id))");
        executeCommand("jms:send artemis MyQueue 'the-message'");

        Thread.sleep(1000);

        String output = executeCommand("jdbc:query derby select * from messages");
        System.err.println(output);

        assertContains("the-message", output);

    }

    private static Set<String> asSet(String... strings) {
        return new HashSet<>(Arrays.asList(strings));
    }

}
