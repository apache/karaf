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

import org.apache.karaf.examples.command.api.BookingService;
import org.apache.karaf.itests.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CommandExampleTest extends BaseTest {

    @Test
    public void test() throws Exception {
        // add command example features repository
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-command-example-features/" + System.getProperty("karaf.version") + "/xml");

        // install karaf-command-example-provider feature
        installAndAssertFeature("karaf-command-example-provider");
        assertServiceAvailable(BookingService.class);

        // install karaf-command-example feature
        installAndAssertFeature("karaf-command-example");

        // execute booking commands
        executeCommand("booking:add AF520 Foo");
        String output = executeCommand("booking:list");
        System.out.println(output);
        assertContains("AF520", output);
    }

}
