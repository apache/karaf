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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.junit.Assert.assertFalse;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class SpringFeaturesTest extends KarafTestSupport {

    private void installAndAssertFeature(String feature) throws Exception {
        featureService.installFeature(feature);
        assertFeatureInstalled(feature);
    }

    @Test
    public void installSpringFeature() throws Exception {
        installAndAssertFeature("spring");
    }

    @Test
    public void installSpringAspectsFeature() throws Exception {
        installAndAssertFeature("spring-aspects");
    }

    @Test
    public void installSpringDmFeature() throws Exception {
        installAndAssertFeature("spring-dm");
    }

    @Test
    public void installSpringDmWebFeature() throws Exception {
        installAndAssertFeature("spring-dm-web");
    }

    @Test
    public void installSpringInstrumentFeature() throws Exception {
        installAndAssertFeature("spring-instrument");
    }

    @Test
    public void installSpringJdbcFeature() throws Exception {
        installAndAssertFeature("spring-jdbc");
    }

    @Test
    public void installSpringJmsFeature() throws Exception {
        installAndAssertFeature("spring-jms");
    }

    @Test
    public void installSpringStrutsFeature() throws Exception {
        installAndAssertFeature("spring-struts");
    }

    @Test
    public void installSpringTestFeature() throws Exception {
        installAndAssertFeature("spring-test");
    }

    @Test
    public void installSpringOrmFeature() throws Exception {
        installAndAssertFeature("spring-orm");
    }

    @Test
    public void installSpringOxmFeature() throws Exception {
        installAndAssertFeature("spring-oxm");
    }

    @Test
    public void installSpringTxFeature() throws Exception {
        installAndAssertFeature("spring-tx");
    }

    @Test
    public void installSpringWebFeature() throws Exception {
        installAndAssertFeature("spring-web");
    }

    @Test
    public void installSpringWebPortletFeature() throws Exception {
        installAndAssertFeature("spring-web-portlet");
    }

    @Test
    public void installGeminiBlueprintFeature() throws Exception {
        installAndAssertFeature("gemini-blueprint");
    }

}
