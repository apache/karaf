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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class JaasExampleTest extends BaseTest {

    @Test(expected = RuntimeException.class)
    public void testCommand() throws Exception {
        // add jaas example repository
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-jaas-example-features/" + System.getProperty("karaf.version") + "/xml");

        // install karaf-jaas-example-app feature
        installAndAssertFeature("karaf-jaas-example-app");

        // execute example:jaas command
        String output = executeCommand("example:jaas karaf karaf");
        assertContains("Authentication successful", output);
        output = executeCommand("example:jaas foo bar");
    }

}
