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

    private void installAndAssertFeature(String feature) throws Exception {
        featureService.installFeature(feature);
        assertFeatureInstalled(feature);
    }

    @Test
    public void installWrapperFeature() throws Exception {
        installAndAssertFeature("wrapper");
    }

    @Test
    public void installObrFeature() throws Exception {
        installAndAssertFeature("obr");
    }

    @Test
    public void installJettyFeature() throws Exception {
        installAndAssertFeature("jetty");
    }

    @Test
    public void installHttpFeature() throws Exception {
        installAndAssertFeature("http");
    }

    @Test
    public void installHttpWhiteboardFeature() throws Exception {
        installAndAssertFeature("http-whiteboard");
    }

    @Test
    public void installWarFeature() throws Exception {
        installAndAssertFeature("war");
    }

    @Test
    public void installWebConsoleFeature() throws Exception {
        installAndAssertFeature("webconsole");
    }

    @Test
    public void installSchedulerFeature() throws Exception {
        installAndAssertFeature("scheduler");
    }

    @Test
    public void installEventAdminFeature() throws Exception {
        installAndAssertFeature("eventadmin");
    }

    @Test
    public void installJasyptEncryptionFeature() throws Exception {
        installAndAssertFeature("jasypt-encryption");
    }

    @Test
    public void installScrFeature() throws Exception {
        installAndAssertFeature("scr");
    }

}
