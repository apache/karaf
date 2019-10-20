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
package org.apache.karaf.itests.examples;

import org.apache.karaf.itests.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class MBeanExampleTest extends BaseTest {

    private void setup() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-mbean-example-features/" + System.getProperty("karaf.version") + "/xml");
        installAndAssertFeature("karaf-mbean-example-provider");
    }

    private void checkMBean() throws Exception {
        MBeanServerConnection connection = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf.examples:type=booking,name=default");
        MBeanInfo info = connection.getMBeanInfo(name);
        Assert.assertNotNull(info);
    }

    @Test
    public void testSimple() throws Exception {
        setup();
        installAndAssertFeature("karaf-mbean-example-simple");
        checkMBean();
    }

    @Test
    public void testBlueprint() throws Exception {
        setup();
        installAndAssertFeature("karaf-mbean-example-blueprint");
        checkMBean();
    }

    @Test
    public void testScr() throws Exception {
        setup();
        installAndAssertFeature("karaf-mbean-example-scr");
        checkMBean();
    }

}
