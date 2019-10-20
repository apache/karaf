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

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ObrTest extends BaseTest {

    @Before
    public void installObrFeature() throws Exception {
        installAndAssertFeature("obr");
    }

       
    @Test
    public void listCommands() throws Exception {
        System.out.println(executeCommand("obr:url-list"));
        System.out.println(executeCommand("obr:list"));
    }

    @Test
    public void listsViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=obr,name=root");
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) mbeanServer.getAttribute(name, "Urls");
        assertEquals(0, urls.size());
        TabularData bundles = (TabularData) mbeanServer.getAttribute(name, "Bundles");
        assertEquals(0, bundles.size());
    }

}
