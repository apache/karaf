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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class PackageTest extends KarafTestSupport {

    @Test
    public void exportsCommand() throws Exception {
        String exportsOutput = executeCommand("package:exports");
        System.out.println(exportsOutput);
        assertFalse(exportsOutput.isEmpty());
    }

    @Test
    public void exportsViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=package,name=root");
            TabularData exports = (TabularData) connection.getAttribute(name, "Exports");
            assertTrue(exports.size() > 0);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void importsCommand() throws Exception {
        String importsOutput = executeCommand("package:imports");
        System.out.println(importsOutput);
        assertFalse(importsOutput.isEmpty());
    }

    @Test
    public void importsViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=package,name=root");
            TabularData imports = (TabularData) connection.getAttribute(name, "Imports");
            assertTrue(imports.size() > 0);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

}
