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

import java.util.ArrayList;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class PackagesTest extends KarafTestSupport {

    @Test
    public void exportsCommand() throws Exception {
        String exportsOutput = executeCommand("packages:exports");
        System.out.println(exportsOutput);
        assertFalse(exportsOutput.isEmpty());
    }

    @Test
    public void exportsViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=packages,name=root");
            ArrayList exports = (ArrayList) connection.invoke(name, "exportedPackages", new Object[]{ }, new String[]{ });
            assertTrue(exports.size() > 0);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

    @Test
    public void importsCommand() throws Exception {
        String importsOutput = executeCommand("packages:imports");
        System.out.println(importsOutput);
        assertFalse(importsOutput.isEmpty());
    }

    @Test
    public void importsViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=packages,name=root");
            ArrayList imports = (ArrayList) connection.invoke(name, "importedPackages", new Object[]{ }, new String[]{ });
            assertTrue(imports.size() > 0);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

}
