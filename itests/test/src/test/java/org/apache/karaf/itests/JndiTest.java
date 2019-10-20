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

import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.apache.karaf.jndi.JndiService;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)

public class JndiTest extends BaseTest {

    @Before
    public void installJndiFeature() throws Exception {
        installAndAssertFeature("jndi");
        getOsgiService(JndiService.class, 30000);
    }
    
    
    @Test
    public void testCommand() throws Exception {
        String output = executeCommand("jndi:names");
        System.out.println(output);
        assertContains("osgi:service/jndi", output);
        output = executeCommand("jndi:alias osgi:service/jndi /test/foo");
        System.out.println(output);
        output = executeCommand("jndi:names");
        System.out.println(output);
        assertContains("/test/foo", output);
        output = executeCommand("jndi:bind 40 /test/bar");
        System.out.println(output);
        output = executeCommand("jndi:names");
        System.out.println(output);
        assertContains("/test/bar", output);
        output = executeCommand("jndi:unbind /test/bar");
        System.out.println(output);
        assertContainsNot("/test/bar", output);
    }

}
