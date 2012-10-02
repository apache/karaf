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

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class RegionTest extends KarafTestSupport {

    @Test
    public void infoCommand() throws Exception {
        Thread.sleep(1000);
        String infoOutput = executeCommand("region:info");
        System.out.println(infoOutput);
        assertTrue("Region org.eclipse.equinox.region.kernel should be present", infoOutput.contains("org.eclipse.equinox.region.kernel"));
        assertTrue("Region org.apache.karaf.region.application should be present", infoOutput.contains("org.apache.karaf.region.application"));
    }

    @Test
    public void addRegionCommand() throws Exception {
        Thread.sleep(2000);
        System.out.println(executeCommand("region:addregion itest"));
        String infoOutput = executeCommand("region:info");
        System.out.println(infoOutput);
        assertTrue("Region itest should be present", infoOutput.contains("itest"));
    }

}
