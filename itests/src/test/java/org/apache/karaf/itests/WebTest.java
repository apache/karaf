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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class WebTest extends KarafTestSupport {

    @Before
    public void installWarFeature() throws Exception {
    	featureService.installFeature("war");
    	assertFeatureInstalled("war");
    	assertBundleInstalled("org.apache.karaf.web.command");
    	//just sleep for a while so blueprint is able to pick up the new commands. 
    	Thread.sleep(2000);
    }

    @Test
    public void listCommand() throws Exception {
        String listOutput = executeCommand("web:list");
        System.out.println(listOutput);
        assertFalse(listOutput.isEmpty());
    }

    @Test
    public void listViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=web,name=root");
            TabularData webBundles = (TabularData) connection.getAttribute(name, "WebBundles");
            assertEquals(0, webBundles.size());
        } finally {
            if (connector != null)
                connector.close();
        }
    }

}
