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

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.lang.management.ManagementFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class DiagnosticTest extends BaseTest {

    @Test
    public void dumpCreateCommand() throws Exception {
        assertContains("Created dump zip", executeCommand("dev:dump-create"));
    }
  
    @Test
    public void dumpCreateCommandNoHeapDump() throws Exception {
        assertContains("Created dump zip", executeCommand("dev:dump-create --no-heap-dump"));
    }
    
    @Test
    public void dumpCreateCommandNoThreadDump() throws Exception {
        assertContains("Created dump zip", executeCommand("dev:dump-create --no-thread-dump"));
    }
   
    @Test
    public void dumpCreateCommandNoHeapAndThreadDump() throws Exception {
        assertContains("Created dump zip", executeCommand("dev:dump-create --no-heap-dump --no-thread-dump"));
    }
    
    @Test
    public void createDumpViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=diagnostic,name=root");
        mbeanServer.invoke(name, "createDump", new Object[]{ "itest" }, new String[]{ "java.lang.String" });
    }

}
