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
public class StandardFeaturesTest extends KarafTestSupport {

    @Test
    public void testBootFeatures() throws Exception {
        assertFeatureInstalled("config");
        assertFeatureInstalled("ssh");
        assertFeatureInstalled("management");
        assertFeatureInstalled("kar");
    }

    private void installAndAssertFeature(String feature) throws Exception {
        featuresService.installFeature(feature);
        assertFeatureInstalled(feature);
    }

    @Test
    public void testSpringFeature() throws Exception {
        installAndAssertFeature("spring");
    }

    @Test
    public void testSpringAspectsFeature() throws Exception {
        installAndAssertFeature("spring-aspects");
    }

    @Test
    public void testSpringDmFeature() throws Exception {
        installAndAssertFeature("spring-dm");
    }

    @Test
    public void testSpringDmWebFeature() throws Exception {
        installAndAssertFeature("spring-dm-web");
    }

    @Test
    public void testSpringInstrumentFeature() throws Exception {
        installAndAssertFeature("spring-instrument");
    }

    @Test
    public void testSpringJdbcFeature() throws Exception {
        installAndAssertFeature("spring-jdbc");
    }

    @Test
    public void testSpringJmsFeature() throws Exception {
        installAndAssertFeature("spring-jms");
    }

    @Test
    public void testSpringStrutsFeature() throws Exception {
        installAndAssertFeature("spring-struts");
    }

    @Test
    public void testSpringTestFeature() throws Exception {
        installAndAssertFeature("spring-test");
    }

    @Test
    public void testSpringOrmFeature() throws Exception {
        installAndAssertFeature("spring-orm");
    }

    @Test
    public void testSpringOxmFeature() throws Exception {
        installAndAssertFeature("spring-oxm");
    }

    @Test
    public void testSpringTxFeature() throws Exception {
        installAndAssertFeature("spring-tx");
    }

    @Test
    public void testSpringWebFeature() throws Exception {
        installAndAssertFeature("spring-web");
    }

    @Test
    public void testSpringWebPortletFeature() throws Exception {
        installAndAssertFeature("spring-web-portlet");
    }

    @Test
    public void testWrapperFeature() throws Exception {
        installAndAssertFeature("wrapper");
    }

    @Test
    public void testObrFeature() throws Exception {
        installAndAssertFeature("obr");
    }

    @Test
    public void testJettyFeature() throws Exception {
        installAndAssertFeature("jetty");
    }

    @Test
    public void testHttpFeature() throws Exception {
        installAndAssertFeature("http");
    }

    @Test
    public void testHttpWhiteboardFeature() throws Exception {
        installAndAssertFeature("http-whiteboard");
    }

    @Test
    public void testWarFeature() throws Exception {
        installAndAssertFeature("war");
    }

    @Test
    public void testWebconsoleFeature() throws Exception {
        installAndAssertFeature("webconsole");
    }

    @Test
    public void testEventadminFeature() throws Exception {
        installAndAssertFeature("eventadmin");
    }

    @Test
    public void testJasyptEncryptionFeature() throws Exception {
        installAndAssertFeature("jasypt-encryption");
    }

}
