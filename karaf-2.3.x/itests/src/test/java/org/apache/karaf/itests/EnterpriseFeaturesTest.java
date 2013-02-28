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

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EnterpriseFeaturesTest extends KarafTestSupport {

    private void installAndAssertFeature(String feature) throws Exception {
        featuresService.installFeature(feature);
        assertFeatureInstalled(feature);
    }

    @Test
    public void testTransactionFeature() throws Exception {
        installAndAssertFeature("transaction");
    }

    @Test
    public void testJpaFeature() throws Exception {
        installAndAssertFeature("jpa");
    }

    @Test
    public void testJndiFeature() throws Exception {
        installAndAssertFeature("jndi");
    }

    @Test
    public void testApplicationWithoutIsolationFeature() throws Exception {
        installAndAssertFeature("application-without-isolation");
    }

}
