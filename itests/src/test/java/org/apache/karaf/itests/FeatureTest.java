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

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FeatureTest extends KarafTestSupport {

    @Test
    public void bootFeatures() throws Exception {
        String featureListOutput = executeCommand("feature:list -i");
        System.out.println(featureListOutput);
        assertTrue(featureListOutput.contains("standard"));
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
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=feature,name=root");
            TabularData features = (TabularData) connection.getAttribute(name, "Features");
            assertTrue(features.size() > 0);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void installUninstallCommand() throws Exception {
        String featureInstallOutput = executeCommand("feature:install -v eventadmin");
        System.out.println(featureInstallOutput);
        assertFalse(featureInstallOutput.isEmpty());
        String featureListOutput = executeCommand("feature:list -i | grep eventadmin");
        System.out.println(featureListOutput);
        assertFalse(featureListOutput.isEmpty());
        System.out.println(executeCommand("feature:uninstall eventadmin"));
        featureListOutput = executeCommand("feature:list -i | grep eventadmin");
        System.out.println(featureListOutput);
        assertTrue(featureListOutput.isEmpty());
    }

    @Test
    public void installUninstallViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=feature,name=root");
            connection.invoke(name, "installFeature", new Object[] { "eventadmin" }, new String[]{ "java.lang.String" });
            connection.invoke(name, "uninstallFeature", new Object[] { "eventadmin" }, new String[]{ "java.lang.String" });
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void repoAddRemoveCommand() throws Exception {
        System.out.println(executeCommand("feature:repo-add mvn:org.apache.karaf.cellar/apache-karaf-cellar/2.2.4/xml/features"));
        String repoListOutput = executeCommand("feature:repo-list");
        System.out.println(repoListOutput);
        assertTrue(repoListOutput.contains("apache-karaf-cellar"));
        System.out.println(executeCommand("feature:repo-remove mvn:org.apache.karaf.cellar/apache-karaf-cellar/2.2.4/xml/features"));
        repoListOutput = executeCommand("feature:repo-list");
        System.out.println(repoListOutput);
        assertFalse(repoListOutput.contains("apache-karaf-cellar"));
    }

    @Test
    public void repoAddRemoveViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=feature,name=root");
            connection.invoke(name, "addRepository", new Object[] { "mvn:org.apache.karaf.cellar/apache-karaf-cellar/2.2.4/xml/features" }, new String[]{ "java.lang.String" });
            connection.invoke(name, "removeRepository", new Object[] { "mvn:org.apache.karaf.cellar/apache-karaf-cellar/2.2.4/xml/features" }, new String[]{ "java.lang.String" });
        } finally {
            if (connector != null)
                connector.close();
        }
    }

}
