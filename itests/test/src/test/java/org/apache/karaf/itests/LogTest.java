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
import java.util.function.Supplier;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class LogTest extends BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogTest.class);

    private static final long LEVEL_APPLIED_TIMEOUT = 30000L;
    private static final long ALIAS_COMMAND_TIMEOUT = 360000L;
    // log:list and ld are aliases defined in shell.init.script, not command services: don't wait for a service
    // that is never registered (the default would block 30 seconds on every call)
    private static final long NO_WAIT = 0L;

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
        assertContains("DEBUG", executeAlias("ld -n 200", ALIAS_COMMAND_TIMEOUT, NO_WAIT, false));
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
        String displayOutput = executeAlias("ld", ALIAS_COMMAND_TIMEOUT, NO_WAIT, false).trim();
        assertTrue("Should be empty but was: " + displayOutput, displayOutput.trim().isEmpty());
    }
    
    public void assertSetLevel(String level) throws InterruptedException {
        System.out.println(executeCommand("log:set " + level));
        // log:set updates the pax-logging configuration asynchronously (ConfigAdmin update, then pax-logging
        // reconfiguration): until it is applied, the configuration can be transiently unreadable ("Unrecognized
        // configuration") or still hold the previous level. Wait for the new level instead of failing right away.
        assertEventuallyContains(level, () -> executeCommand("log:get"));
        assertEventuallyContains(level, () -> executeAlias("log:list", ALIAS_COMMAND_TIMEOUT, NO_WAIT, false));
    }

    private void assertEventuallyContains(String expected, Supplier<String> command) throws InterruptedException {
        long deadline = System.currentTimeMillis() + LEVEL_APPLIED_TIMEOUT;
        String last;
        while (true) {
            try {
                last = command.get();
                if (last.contains(expected)) {
                    return;
                }
            } catch (RuntimeException e) {
                // transient configuration state while pax-logging reconfigures
                last = "Exception: " + e.getMessage();
            }
            if (System.currentTimeMillis() > deadline) {
                break;
            }
            Thread.sleep(100);
        }
        assertContains(expected, last);
    }

}
