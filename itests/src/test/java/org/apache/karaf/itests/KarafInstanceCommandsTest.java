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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class KarafInstanceCommandsTest extends KarafTestSupport {

    @Test
    public void createDestroyInstance() throws Exception {
        System.out.println(executeCommand("instance:create itest"));
        String instanceListOutput = executeCommand("instance:list");
        System.out.println(instanceListOutput);
        assertTrue(instanceListOutput.contains("itest"));
        System.out.println(executeCommand("instance:destroy itest"));
        instanceListOutput = executeCommand("instance:list");
        System.out.println(instanceListOutput);
        assertFalse(instanceListOutput.contains("itest"));
    }

    @Test
    public void cloneInstance() throws Exception {
        System.out.println(executeCommand("instance:clone root itest"));
        String instanceListOutput = executeCommand("instance:list");
        System.out.println(instanceListOutput);
        assertTrue(instanceListOutput.contains("itest"));
    }

    @Test
    public void renameInstance() throws Exception {
        System.out.println(executeCommand("instance:create itest"));
        System.out.println(executeCommand("instance:rename itest new_itest"));
        String instanceListOutput = executeCommand("instance:list");
        System.out.println(instanceListOutput);
        assertTrue(instanceListOutput.contains("new_itest"));
    }

}
