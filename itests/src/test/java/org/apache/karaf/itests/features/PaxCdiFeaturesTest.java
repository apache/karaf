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
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class PaxCdiFeaturesTest extends KarafTestSupport {

    @Test
    public void installPaxCdiFeature() throws Exception {
        installAndAssertFeature("pax-cdi");
    }

    @Test
    public void installPaxCdi11Feature() throws Exception {
        installAndAssertFeature("pax-cdi-1.1");
    }

    @Test
    public void installPaxCdi12Feature() throws Exception {
        installAndAssertFeature("pax-cdi-1.2");
    }

    @Test
    public void installPaxCdiWeldFeature() throws Exception {
        installAndAssertFeature("pax-cdi-weld");
    }

    @Test
    public void installPaxCdiWeld11Feature() throws Exception {
        installAndAssertFeature("pax-cdi-1.1-weld");
    }

    @Test
    public void installPaxCdiWeld12Feature() throws Exception {
        installAndAssertFeature("pax-cdi-1.2-weld");
    }

    @Test
    public void installPaxCdiOpenwebbeansFeature() throws Exception {
        installAndAssertFeature("pax-cdi-openwebbeans");
    }

    @Test
    public void installPaxCdiWebFeature() throws Exception {
        installAndAssertFeature("pax-cdi-web");
    }

    @Test
    public void installPaxCdi11WebFeature() throws Exception {
        installAndAssertFeature("pax-cdi-1.1-web");
    }

    @Test
    @Ignore("PAXCDI-146 Require Pax Web 4.x/Karaf 4")
    public void installPaxCdi12WebFeature() throws Exception {
        installAndAssertFeature("pax-cdi-1.2-web");
    }

    @Test
    @Ignore("PAXCDI-146: Require Pax Web 4.x/Karaf 4")
    public void installPaxCdiWebWeldFeature() throws Exception {
        installAndAssertFeature("pax-cdi-web-weld");
    }

    @Test
    @Ignore("PAXCDI-146: Require Pax Web 4.x/Karaf 4")
    public void installPaxCdi11WebWeldFeature() throws Exception {
        installAndAssertFeature("pax-cdi-1.1-web-weld");
    }

    @Test
    @Ignore("PAXCDI-146: Require Pax Web 4.x/Karaf 4")
    public void installPaxCdi12WebWeldFeature() throws Exception {
        installAndAssertFeature("pax-cdi-1.2-web-weld");
    }

    @Test
    @Ignore("PAXCDI-146: Require Pax Web 4.x/Karaf 4")
    public void installPaxCdiWebOpenwebbeansFeature() throws Exception {
        installAndAssertFeature("pax-cdi-web-openwebbeans");
    }

    @Test
    public void installDeltaspikeCoreFeature() throws Exception {
        installAndAssertFeature("deltaspike-core");
    }

    @Test
    public void installDeltaspikeJpaFeature() throws Exception {
        installAndAssertFeature("deltaspike-jpa");
    }

}
