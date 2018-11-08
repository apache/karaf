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
package org.apache.karaf.itests.examples;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.container.internal.JavaVersionUtil;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JmsExampleTest extends KarafTestSupport {

    private static final EnumSet<FeaturesService.Option> NO_AUTO_REFRESH =
            EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles);

    @Configuration
    public Option[] config() {
        String version = MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf");
        List<Option> result = new LinkedList<>(Arrays.asList(super.config()));
        result.add(
                editConfigurationFilePut(
                        "etc/org.apache.karaf.features.cfg",
                        "featuresRepositories",
                        "mvn:org.apache.karaf.features/framework/"
                                + version
                                + "/xml/features, "
                                + "mvn:org.apache.karaf.features/enterprise/"
                                + version
                                + "/xml/features, "
                                + "mvn:org.apache.karaf.features/spring-legacy/"
                                + version
                                + "/xml/features, "
                                + "mvn:org.apache.karaf.features/standard/"
                                + version
                                + "/xml/features, "
                                + "mvn:org.apache.activemq/artemis-features/2.6.0/xml/features"));
        result.add(
                replaceConfigurationFile(
                        "etc/org.ops4j.connectionfactory-artemis.cfg",
                        getConfigFile(
                                "/org/apache/karaf/itests/features/org.ops4j.connectionfactory-artemis.cfg")));
        if (JavaVersionUtil.getMajorVersion() >= 9) {
            // need asm 6.x which support java9plus to run this test
            result.add(
                    replaceConfigurationFile(
                            "system/org/apache/karaf/features/standard/"
                                    + version
                                    + "/standard-"
                                    + version
                                    + "-features.xml",
                            getConfigFile("/etc/feature.xml")));
        }
        return result.toArray(new Option[result.size()]);
    }

    @Test
    public void test() throws Exception {
        featureService.installFeature("aries-blueprint");

        System.out.println("== Installing Artemis");
        featureService.installFeature("artemis", NO_AUTO_REFRESH);
        Thread.sleep(15000); // sleep a while ensure the jms broker is up
        featureService.installFeature("jms", NO_AUTO_REFRESH);
        featureService.installFeature("pax-jms-artemis", NO_AUTO_REFRESH);

        String output = executeCommand("jms:info artemis");
        System.out.println(output);
        assertContains("ActiveMQ", output);

        installBundle(
                "mvn:org.apache.karaf.examples/karaf-jms-example-command/"
                        + System.getProperty("karaf.version"),
                true);

        executeCommand("example:send TEST FOO");

        output = executeCommand("example:consume TEST");
        System.out.println(output);
        assertContains("FOO", output);
    }
}
