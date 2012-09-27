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
import static org.junit.Assert.assertTrue;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class LogTest extends KarafTestSupport {

    @Test
    public void setDebugAndDisplay() throws Exception {
        System.out.println(executeCommand("log:set DEBUG"));
        String displayOutput = executeCommand("log:display");
        System.out.println(displayOutput);
        assertTrue(displayOutput.contains("DEBUG"));
    }

    @Test
    public void setDebugViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=log,name=root");
            Attribute attribute = new Attribute("Level", "DEBUG");
            connection.setAttribute(name, attribute);
            String logLevel = (String) connection.getAttribute(name, "Level");
            assertEquals("DEBUG", logLevel);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void setGetDebugAndClear() throws Exception {
        System.out.println(executeCommand("log:set DEBUG"));
        String getOutput = executeCommand("log:get");
        System.out.println(getOutput);
        assertTrue(getOutput.contains("DEBUG"));
        System.out.println(executeCommand("log:set INFO"));
        System.out.println(executeCommand("log:clear"));
        String displayOutput = executeCommand("log:display");
        System.out.println(displayOutput.trim());
        assertTrue(displayOutput.trim().isEmpty());
    }

}
