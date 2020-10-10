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

import org.apache.karaf.itests.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class Spring43FeaturesTest extends BaseTest {

    @Configuration
    public Option[] config() {
        String version = MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf");
        List<Option> result = new LinkedList<>(Arrays.asList(super.config()));
        result.add(editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresRepositories",
                "mvn:org.apache.karaf.features/framework/" + version + "/xml/features, " +
                        "mvn:org.apache.karaf.features/spring/" + version + "/xml/features, " +
                        "mvn:org.apache.karaf.features/spring-legacy/" + version + "/xml/features, " +
                        "mvn:org.apache.karaf.features/enterprise/" + version + "/xml/features, " +
                        "mvn:org.apache.karaf.features/standard/" + version + "/xml/features"));
        return result.toArray(new Option[result.size()]);
    }

    @Test
    public void installSpringFeature() throws Exception {
        installAssertAndUninstallFeature("spring", System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringAspectsFeature() throws Exception {
        installAssertAndUninstallFeature("spring-aspects", System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringInstrumentFeature() throws Exception {
        installAssertAndUninstallFeature("spring-instrument", System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringJdbcFeature() throws Exception {
        installAssertAndUninstallFeature("spring-jdbc", System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringJmsFeature() throws Exception {
        installAssertAndUninstallFeature("spring-jms", System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringMessagingFeature() throws Exception {
        installAssertAndUninstallFeature("spring-messaging", System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringTestFeature() throws Exception {
        installAssertAndUninstallFeature("spring-test", System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringOrmFeature() throws Exception {
        installAssertAndUninstallFeature("spring-orm", System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringOxmFeature() throws Exception {
        installAssertAndUninstallFeature("spring-oxm", System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringTxFeature() throws Exception {
        installAssertAndUninstallFeature("spring-tx", System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringWebFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http", "spring-web/" + System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringWebPortletFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http", "spring-web-portlet/" + System.getProperty("spring43.version"));
    }

    @Test
    public void installSpringWebSocketFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http", "spring-websocket/" + System.getProperty("spring43.version"));
    }

    // Spring Security

    @Test
    public void installSpringSecurityFeature() throws Exception {
        installAssertAndUninstallFeature("spring-security", System.getProperty("spring.security42.version"));
    }

}
