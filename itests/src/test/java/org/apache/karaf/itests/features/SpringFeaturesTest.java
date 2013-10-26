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
package org.apache.karaf.itests.features;

import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SpringFeaturesTest extends KarafTestSupport {

    @Test
    public void installSpringFeature() throws Exception {
        installAssertAndUninstallFeature("spring");
    }

    @Test
    public void installSpringAspectsFeature() throws Exception {
        installAssertAndUninstallFeature("spring-aspects");
    }

    @Test
    public void installSpringDmFeature() throws Exception {
        installAssertAndUninstallFeature("spring-dm");
    }

    @Test
    public void installSpringDmWebFeature() throws Exception {
        installAssertAndUninstallFeature("spring-dm-web");
    }

    @Test
    public void installSpringInstrumentFeature() throws Exception {
        installAssertAndUninstallFeature("spring-instrument");
    }

    @Test
    public void installSpringJdbcFeature() throws Exception {
        installAssertAndUninstallFeature("spring-jdbc");
    }

    @Test
    public void installSpringJmsFeature() throws Exception {
        installAssertAndUninstallFeature("spring-jms");
    }

    @Test
    public void installSpringStrutsFeature() throws Exception {
        installAssertAndUninstallFeature("spring-struts");
    }

    @Test
    public void installSpringTestFeature() throws Exception {
        installAssertAndUninstallFeature("spring-test");
    }

    @Test
    public void installSpringOrmFeature() throws Exception {
        installAssertAndUninstallFeature("spring-orm");
    }

    @Test
    public void installSpringOxmFeature() throws Exception {
        installAssertAndUninstallFeature("spring-oxm");
    }

    @Test
    public void installSpringTxFeature() throws Exception {
        installAssertAndUninstallFeature("spring-tx");
    }

    @Test
    public void installSpringWebFeature() throws Exception {
        installAssertAndUninstallFeature("spring-web");
    }

    @Test
    @Ignore
    public void installSpringWebPortletFeature() throws Exception {
        installAssertAndUninstallFeature("spring-web-portlet");
    }

    @Test
    @Ignore
    public void installGeminiBlueprintFeature() throws Exception {
        installAssertAndUninstallFeature("gemini-blueprint");
    }

}
