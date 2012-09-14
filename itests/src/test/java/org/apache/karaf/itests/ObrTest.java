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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ObrTest extends KarafTestSupport {

    @Before
    public void installObrFeature() throws Exception {
        System.out.println(executeCommand("features:install obr"));
    }

    @Test
    public void listCommands() throws Exception {
        System.out.println(executeCommand("obr:listUrl"));
        System.out.println(executeCommand("obr:list"));
    }

    @Test
    public void listsViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=obr,name=root");
            List<String> urls = (List<String>) connection.invoke(name, "listUrls", new Object[]{ }, new String[]{ });
            assertEquals(0, urls.size());
            TabularData bundles = (TabularData) connection.getAttribute(name, "Bundles");
            assertEquals(0, bundles.size());
        } finally {
            if (connector != null)
                connector.close();
        }
    }

}
