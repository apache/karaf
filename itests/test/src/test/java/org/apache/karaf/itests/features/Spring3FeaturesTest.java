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

import org.junit.Ignore;
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
public class Spring3FeaturesTest extends BaseTest {

    // Spring DM

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
    public void installSpringDmFeature() throws Exception {
        installAssertAndUninstallFeatures("spring-dm");
    }

    @Test
    public void installSpringDmWebFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http", "spring-dm-web");
    }

    // Spring 3.1.x

    @Test
    public void installSpring31Feature() throws Exception {
        installAssertAndUninstallFeature("spring", System.getProperty("spring31.version"));
    }

    @Test
    public void installSpringAspects31Feature() throws Exception {
        installAssertAndUninstallFeature("spring-aspects", System.getProperty("spring31.version"));
    }

    @Test
    public void installSpringInstrument31Feature() throws Exception {
        installAssertAndUninstallFeature("spring-instrument", System.getProperty("spring31.version"));
    }

    @Test
    public void installSpringJdbc31Feature() throws Exception {
        installAssertAndUninstallFeature("spring-jdbc", System.getProperty("spring31.version"));
    }

    @Test
    public void installSpringJms31Feature() throws Exception {
        installAssertAndUninstallFeature("spring-jms", System.getProperty("spring31.version"));
    }

    @Test
    @Ignore("Skipped due to version range not matching to Servlet 3.1")
    public void installSpringStruts31Feature() throws Exception {
        installAssertAndUninstallFeature("spring-struts", System.getProperty("spring31.version"));
    }

    @Test
    public void installSpringTest31Feature() throws Exception {
        installAssertAndUninstallFeature("spring-test", System.getProperty("spring31.version"));
    }

    @Test
    public void installSpringOrm31Feature() throws Exception {
        installAssertAndUninstallFeature("spring-orm", System.getProperty("spring31.version"));
    }

    @Test
    public void installSpringOxm31Feature() throws Exception {
        installAssertAndUninstallFeature("spring-oxm", System.getProperty("spring31.version"));
    }

    @Test
    public void installSpringTx31Feature() throws Exception {
        installAssertAndUninstallFeature("spring-tx", System.getProperty("spring31.version"));
    }

    @Test
    public void installSpringWeb31Feature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http", "spring-web/" + System.getProperty("spring31.version"));
    }

    @Test
    public void installSpringWebPortlet31Feature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http", "spring-web-portlet/" + System.getProperty("spring31.version"));
    }

    // Spring Security

    @Test
    public void installSpringSecurityFeature() throws Exception {
        installAssertAndUninstallFeature("spring-security", System.getProperty("spring.security31.version"));
    }

}
