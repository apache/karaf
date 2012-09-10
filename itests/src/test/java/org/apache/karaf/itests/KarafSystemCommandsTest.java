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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class KarafSystemCommandsTest extends KarafTestSupport {

    @Test
    public void name() throws Exception {
        String nameOutput = executeCommand("system:name");
        System.out.println(nameOutput);
        assertEquals("root", nameOutput.trim());
    }

    @Test
    public void framework() throws Exception {
        String frameworkOutput = executeCommand("system:framework");
        System.out.println(frameworkOutput);
        assertTrue(frameworkOutput.contains("felix"));
    }

    @Test
    public void startLevel() throws Exception {
        String startLevelOutput = executeCommand("system:start-level");
        System.out.println(startLevelOutput);
        assertTrue(startLevelOutput.contains("100"));
    }

    @Test
    public void shutdown() throws Exception {
        System.out.println(executeCommand("system:shutdown -f"));
    }

}
