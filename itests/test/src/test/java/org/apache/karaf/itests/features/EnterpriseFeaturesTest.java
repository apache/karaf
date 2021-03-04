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
import org.apache.karaf.itests.util.RunIfRules.RunIfNotOnJdk8;
import org.apache.karaf.itests.util.RunIfRule;

import org.junit.Ignore;
import org.junit.Rule;
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
public class EnterpriseFeaturesTest extends BaseTest {

    @Rule
    public RunIfRule rule = new RunIfRule();

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
    public void installTransaction20Feature() throws Exception {
        installAssertAndUninstallFeature("transaction", "2.0.0");
    }

    @Test
    public void installConnector311Feature() throws Exception {
        installAssertAndUninstallFeature("connector", "3.1.1");
    }

    @Test
    @Ignore("jpa feature is installed two times causing error. Test disabled to investigate.")
    public void installJpaFeature() throws Exception {
    	installAssertAndUninstallFeatures("jpa");
    }

    @Test
    public void installOpenJpaFeature() throws Exception {
        installAssertAndUninstallFeatures("openjpa");
    }

    @Test
    public void installHibernateValidatorFeatures() throws Exception {
        installAssertAndUninstallFeatures("hibernate-validator");
        installAssertAndUninstallFeatures("hibernate-validator-joda-time");
        installAssertAndUninstallFeatures("hibernate-validator-javax-money");
        installAssertAndUninstallFeatures("hibernate-validator-groovy");
        installAssertAndUninstallFeatures("hibernate-validator-paranamer");
    }

    @Test
    public void installJndiFeature() throws Exception {
    	installAssertAndUninstallFeatures("jndi");
    }

    @Test
    public void installJdbcFeature() throws Exception {
        installAssertAndUninstallFeatures("jdbc");
    }

    @Test
    public void installJmsFeature() throws Exception {
        installAssertAndUninstallFeatures("jms");
    }

    @Test
    public void installApplicationWithoutIsolationFeature() throws Exception {
    	installAssertAndUninstallFeatures("application-without-isolation");
    }

    @Test
    public void installSubsystems() throws Exception {
        installAssertAndUninstallFeatures("subsystems");
    }

    @Test
    public void installDocker() throws Exception {
        installAssertAndUninstallFeatures("docker");
    }

}
