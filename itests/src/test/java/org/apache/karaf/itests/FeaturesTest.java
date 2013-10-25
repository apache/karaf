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
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class FeaturesTest extends KarafTestSupport {

    @Test
    public void listCommand() throws Exception {
        String listOutput = executeCommand("features:list");
        System.out.println(listOutput);
        assertFalse(listOutput.isEmpty());
        listOutput = executeCommand("features:list -i");
        System.out.println(listOutput);
        assertFalse(listOutput.isEmpty());
    }

    @Test
    public void listViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=features,name=root");
            TabularData features = (TabularData) connection.getAttribute(name, "Features");
            assertTrue(features.size() > 0);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void installUninstallCommand() throws Exception {
        String featureInstallOutput = executeCommand("features:install -v eventadmin");
        System.out.println(featureInstallOutput);
        assertFalse(featureInstallOutput.isEmpty());
        String featureListOutput = executeCommand("features:list -i | grep eventadmin");
        System.out.println(featureListOutput);
        assertFalse(featureListOutput.isEmpty());
        System.out.println(executeCommand("features:uninstall eventadmin"));
        featureListOutput = executeCommand("features:list -i | grep eventadmin");
        System.out.println(featureListOutput);
        assertTrue(featureListOutput.isEmpty());
    }

    @Test
    public void installUninstallViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=features,name=root");
            connection.invoke(name, "installFeature", new Object[] { "eventadmin" }, new String[]{ "java.lang.String" });
            connection.invoke(name, "uninstallFeature", new Object[] { "eventadmin" }, new String[]{ "java.lang.String" });
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void repoAddRemoveCommand() throws Exception {
        System.out.println(executeCommand("features:addurl mvn:org.apache.karaf.cellar/apache-karaf-cellar/2.2.4/xml/features"));
        String repoListOutput = executeCommand("features:listurl");
        System.out.println(repoListOutput);
        assertTrue(repoListOutput.contains("apache-karaf-cellar"));
        System.out.println(executeCommand("features:removeurl mvn:org.apache.karaf.cellar/apache-karaf-cellar/2.2.4/xml/features"));
        repoListOutput = executeCommand("features:listurl");
        System.out.println(repoListOutput);
        assertFalse(repoListOutput.contains("apache-karaf-cellar"));
    }

    @Test
    public void repoAddRemoveViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=features,name=root");
            connection.invoke(name, "addRepository", new Object[] { "mvn:org.apache.karaf.cellar/apache-karaf-cellar/2.2.4/xml/features" }, new String[]{ "java.lang.String" });
            connection.invoke(name, "removeRepository", new Object[] { "mvn:org.apache.karaf.cellar/apache-karaf-cellar/2.2.4/xml/features" }, new String[]{ "java.lang.String" });
        } finally {
            if (connector != null)
                connector.close();
        }
    }

}
