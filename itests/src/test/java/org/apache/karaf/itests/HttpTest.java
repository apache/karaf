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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class HttpTest extends KarafTestSupport {

    @Before
    public void installHttpFeature() {
        System.out.println(executeCommand("feature:install http"));
        System.out.println(executeCommand("feature:install webconsole"));
    }

    @Test
    public void list() throws Exception {
        String listOutput = executeCommand("http:list");
        System.out.println(listOutput);
        assertTrue(listOutput.contains("/system/console"));
    }

    @Ignore
    @Test
    // TODO remove ignore flag when the HttpMBean is fixed
    public void listViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=http,name=root");
            TabularData servlets = (TabularData) connection.getAttribute(name, "Servlets");
            assertTrue(servlets.size() > 0);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

}
