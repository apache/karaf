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
package org.apache.karaf.examples.itests;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ExampleITest extends KarafTestSupport {

    @Test
    public void listBundleCommand() throws Exception {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class);

        // installing a feature and verifying that it's correctly installed
        installAndAssertFeature("scr");

        // testing a command execution
        String bundles = executeCommand("bundle:list -t 0");
        System.out.println(bundles);
        assertContains("junit", bundles);

        String features = executeCommand("feature:list -i");
        System.out.print(features);
        assertContains("scr", features);

        // using a service and assert state or result
        FeaturesService featuresService = getOsgiService(FeaturesService.class);
        Feature scr = featuresService.getFeature("scr");
        Assert.assertEquals("scr", scr.getName());
    }

}
