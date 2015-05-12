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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.inject.Inject;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;

import org.apache.karaf.packages.core.PackageService;
import org.apache.karaf.packages.core.PackageVersion;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class PackageTest extends KarafTestSupport {
    @Inject
    PackageService packageService;

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
            TabularData exports = (TabularData)connection.getAttribute(name, "Exports");
            assertTrue(exports.size() > 0);
        } finally {
            close(connector);
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
            TabularData imports = (TabularData)connection.getAttribute(name, "Imports");
            assertTrue(imports.size() > 0);
        } finally {
            close(connector);
        }
    }

    @Test
    public void duplicatePackageTest() throws Exception {
        // Leaving out version to make test easier to manage
        // We currently expect no duplicate package exports
        Map<String, Integer> expectedDups = new HashMap<String, Integer>();
        List<PackageVersion> packageVersionMap = packageService.getExports();
       
        for (PackageVersion pVer : packageVersionMap) {
            if (pVer.getBundles().size() > 1) {
                String packageName = pVer.getPackageName();
                int expectedNum = expectedDups.containsKey(packageName) ? expectedDups.get(packageName) : 0;
                Assert.assertEquals("Expecting number of duplicates for package " + packageName, expectedNum, pVer.getBundles().size());
            }
        }
    }

}
