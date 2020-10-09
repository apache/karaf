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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class FeatureTest extends BaseTest {


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
    public void bootFeatures() throws Exception {
        assertFeaturesInstalled("jaas", "ssh", "management", "bundle", "config", "deployer", "diagnostic",
                                "instance", "kar", "log", "package", "service", "system");
    }

    @Test
    public void listCommand() throws Exception {
        String featureListOutput = executeCommand("feature:list");
        System.out.println(featureListOutput);
        assertFalse(featureListOutput.isEmpty());
        featureListOutput = executeCommand("feature:list -i");
        System.out.println(featureListOutput);
        assertFalse(featureListOutput.isEmpty());
    }

    @Test
    public void listViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=feature,name=root");
        TabularData features = (TabularData) mbeanServer.getAttribute(name, "Features");
        assertTrue(features.size() > 0);
    }

    @Test
    public void installUninstallCommand() throws Exception {
        System.out.println(executeCommand("feature:install -v -r wrapper", new RolePrincipal("admin")));
        assertFeatureInstalled("wrapper");
        System.out.println(executeCommand("feature:uninstall -r wrapper", new RolePrincipal("admin")));
        assertFeatureNotInstalled("wrapper");
    }

    @Test
    public void installUninstallViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=feature,name=root");
        mbeanServer.invoke(name, "installFeature", new Object[]{"wrapper", true}, new String[]{"java.lang.String", "boolean"});
        assertFeatureInstalled("wrapper");
        mbeanServer.invoke(name, "uninstallFeature", new Object[]{"wrapper", true}, new String[]{"java.lang.String", "boolean"});
        assertFeatureNotInstalled("wrapper");
    }

    @Test
    public void repoAddRemoveCommand() throws Exception {
        System.out.println(executeCommand("feature:repo-add mvn:org.apache.karaf.cellar/apache-karaf-cellar/3.0.0/xml/features"));
        assertContains("apache-karaf-cellar", executeCommand("feature:repo-list"));
        System.out.println(executeCommand("feature:repo-remove mvn:org.apache.karaf.cellar/apache-karaf-cellar/3.0.0/xml/features"));
        assertContainsNot("apache-karaf-cellar", executeCommand("feature:repo-list"));
    }

    @Test
    public void repoAddRemoveCommandWithRegex() throws Exception {
        System.out.println(executeCommand("feature:repo-add mvn:org.apache.karaf.cellar/apache-karaf-cellar/3.0.0/xml/features"));
        assertContains("apache-karaf-cellar", executeCommand("feature:repo-list"));
        System.out.println(executeCommand("feature:repo-remove '.*apache-karaf-cellar.*'"));
        assertContainsNot("apache-karaf-cellar", executeCommand("feature:repo-list"));
    }

    @Test
    public void repoAddRemoveViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=feature,name=root");
        mbeanServer.invoke(name, "addRepository", new Object[] { "mvn:org.apache.karaf.cellar/apache-karaf-cellar/3.0.0/xml/features" }, new String[]{ "java.lang.String" });
        mbeanServer.invoke(name, "removeRepository", new Object[] { "mvn:org.apache.karaf.cellar/apache-karaf-cellar/3.0.0/xml/features" }, new String[]{ "java.lang.String" });
    }

    @Test
    public void repoAddRemoveWithRegexViaMBean() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=feature,name=root");
        mBeanServer.invoke(name, "addRepository", new Object[] { "mvn:org.apache.karaf.cellar/apache-karaf-cellar/3.0.0/xml/features" }, new String[]{ "java.lang.String" });
        mBeanServer.invoke(name, "removeRepository", new Object[] { ".*apache-karaf-cellar.*" }, new String[]{ "java.lang.String" });
    }

    @Test
    public void repoRefreshCommand() throws Exception {
        String refreshedRepo = executeCommand("feature:repo-refresh '.*org.ops4j.pax.[wc].*'");
        assertContains("pax-cdi", refreshedRepo);
        assertContains("pax-web", refreshedRepo);
    }

    @Test
    public void repoRefreshViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=feature,name=root");
        mbeanServer.invoke(name, "refreshRepository", new Object[] { ".*pax-web.*" }, new String[]{ "java.lang.String" });
    }

    @Test
    public void configRegularLifecycle() throws Exception {
        System.out.println(executeCommand("feature:install http", new RolePrincipal("admin")));
        String output = executeCommand("config:exists org.ops4j.pax.web");
        assertContains("true", output);
        File jetty = new File(System.getProperty("karaf.etc"), "jetty.xml");
        assertTrue("jetty.xml should exist", jetty.exists());

        System.out.println(executeCommand("feature:uninstall http", new RolePrincipal("admin")));
        output = executeCommand("config:exists org.ops4j.pax.web");
        assertContains("true", output);
        jetty = new File(System.getProperty("karaf.etc"), "jetty.xml");
        assertTrue("jetty.xml should still exist", jetty.exists());
    }

    @Test
    public void configDelete() throws Exception {
        System.out.println(executeCommand("feature:install http", new RolePrincipal("admin")));
        String output = executeCommand("config:exists org.ops4j.pax.web");
        assertContains("true", output);
        File jetty = new File(System.getProperty("karaf.etc"), "jetty.xml");
        assertTrue("etc/jetty.xml should exist", jetty.exists());

        System.out.println(executeCommand("feature:uninstall -c http", new RolePrincipal("admin")));
        output = executeCommand("config:exists org.ops4j.pax.web");
        assertContains("false", output);
        jetty = new File(System.getProperty("karaf.etc"), "jetty.xml");
        assertFalse("jetty.xml should not still exist", jetty.exists());
    }

    @Test
    public void configRegularLifecycleViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName featureMBean = new ObjectName("org.apache.karaf:type=feature,name=root");
        ObjectName configMBean = new ObjectName("org.apache.karaf:type=config,name=root");
        mbeanServer.invoke(featureMBean, "installFeature", new Object[]{ "http" }, new String[]{ "java.lang.String" });

        boolean exist = (boolean) mbeanServer.invoke(configMBean, "exists", new Object[]{ "org.ops4j.pax.web" }, new String[]{ "java.lang.String" });
        assertTrue("true", exist);
        File jetty = new File(System.getProperty("karaf.etc", "jetty.xml"));
        assertTrue("jetty.xml should exist", jetty.exists());

        mbeanServer.invoke(featureMBean, "uninstallFeature", new Object[]{ "http" }, new String[]{ "java.lang.String" });
        exist = (boolean) mbeanServer.invoke(configMBean, "exists", new Object[]{ "org.ops4j.pax.web" }, new String[]{ "java.lang.String" });
        assertTrue("true", exist);
        jetty = new File(System.getProperty("karaf.etc", "jetty.xml"));
        assertTrue("jetty.xml should exist", jetty.exists());
    }

    @Test
    public void configDeleteViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName featureMBean = new ObjectName("org.apache.karaf:type=feature,name=root");
        ObjectName configMBean = new ObjectName("org.apache.karaf:type=config,name=root");
        mbeanServer.invoke(featureMBean, "installFeature", new Object[]{ "http" }, new String[]{ "java.lang.String" });

        boolean exist = (boolean) mbeanServer.invoke(configMBean, "exists", new Object[]{ "org.ops4j.pax.web" }, new String[]{ "java.lang.String" });
        assertTrue("true", exist);
        File jetty = new File(System.getProperty("karaf.etc", "jetty.xml"));
        assertTrue("jetty.xml should exist", jetty.exists());

        mbeanServer.invoke(featureMBean, "uninstallFeature", new Object[]{ "http", false, true }, new String[]{ "java.lang.String", "boolean", "boolean" });
        exist = (boolean) mbeanServer.invoke(configMBean, "exists", new Object[]{ "org.ops4j.pax.web" }, new String[]{ "java.lang.String" });
        assertFalse("false", exist);
    }

}
