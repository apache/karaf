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
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class GroovyTest extends BaseTest {

    @Configuration
    public Option[] config() {
        List<Option> result = new LinkedList<>(Arrays.asList(super.config()));
        result.add(replaceConfigurationFile("script.groovy",
                getConfigFile("/org/apache/karaf/itests/groovy/script.groovy")));
        return result.toArray(new Option[result.size()]);
    }

    @Before
    public void setUp() throws Exception {
        installAndAssertFeature("groovy");
    }

    @Test
    public void execGroovyCodeCommand() throws Exception {
        String resultNoArgs = executeCommand("groovy:exec \""
                + "def add(x, y) {\n"
                + "  return x + y \n"
                + "}\n"
                + "println add(1, 2)\"");
        assertContains("3", resultNoArgs);

        String resultTwoArgs = executeCommand("groovy:exec \"(x as int) + (y as int)\" x=1 y=2");
        assertContains("3", resultTwoArgs);
    }

    @Test
    public void execGroovyFileCommand() throws Exception {
        String result = executeCommand("groovy:exec-file script.groovy");
        assertContains("3", result);
    }
}
