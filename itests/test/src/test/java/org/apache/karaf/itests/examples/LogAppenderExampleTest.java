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

import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class LogAppenderExampleTest extends KarafTestSupport {

    @Test
    public void test() throws Exception {
        // adding karaf-log-appender-example features repository
        addFeaturesRepository(
                "mvn:org.apache.karaf.examples/karaf-log-appender-example-features/"
                        + System.getProperty("karaf.version")
                        + "/xml");

        // install the karaf-log-appender-example feature
        installAndAssertFeature("karaf-log-appender-example");

        String output = executeCommand("log:log TEST");
        System.out.println(output);
        assertContains("INFO - TEST", output);
    }
}
