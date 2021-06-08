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
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class LogTest extends BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogTest.class);

    @Test
    public void setDebugAndDisplay() throws Exception {
        assertSetLevel("DEBUG");
        LOGGER.debug("Making sure there is DEBUG level output");
        assertContains("DEBUG", executeCommand("log:display -n 200"));
    }

    @Test
    public void setDebugAndDisplayAlias() throws Exception {
        assertSetLevel("DEBUG");
        LOGGER.debug("Making sure there is DEBUG level output");
        assertContains("DEBUG", executeAlias("ld -n 200"));
    }

    @Test
    public void setDebugViaMBean() throws Exception {
        assertSetLevel("INFO");
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=log,name=root");
        Attribute attribute = new Attribute("Level", "DEBUG");
        mbeanServer.setAttribute(name, attribute);
        String logLevel = (String) mbeanServer.getAttribute(name, "Level");
        assertEquals("DEBUG", logLevel);
    }

    @Test
    public void setGetDebugAndClear() throws Exception {
        assertSetLevel("DEBUG");
        assertSetLevel("INFO");
        System.out.println(executeCommand("log:clear"));
        String displayOutput = executeCommand("log:display").trim();
        assertTrue("Should be empty but was: " + displayOutput, displayOutput.trim().isEmpty());
    }

    @Test
    public void setGetDebugAndClearAlias() throws Exception {
        assertSetLevel("DEBUG");
        assertSetLevel("INFO");
        System.out.println(executeCommand("log:clear"));
        String displayOutput = executeAlias("ld").trim();
        assertTrue("Should be empty but was: " + displayOutput, displayOutput.trim().isEmpty());
    }
    
    public void assertSetLevel(String level) throws InterruptedException {
        System.out.println(executeCommand("log:set " + level));
        assertContains(level, executeCommand("log:get"));
        assertContains(level, executeAlias("log:list"));
        Thread.sleep(100);
    }

}
