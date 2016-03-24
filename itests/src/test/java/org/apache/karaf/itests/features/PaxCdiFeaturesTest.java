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
import org.apache.karaf.itests.util.RunIfRule;
import org.apache.karaf.itests.util.RunIfRules.RunIfNotOnJdk8;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class PaxCdiFeaturesTest extends KarafTestSupport {

    @Rule
    public RunIfRule rule = new RunIfRule();

    
    @Test
    public void installPaxCdiFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi");
    }

    @Test
    public void installPaxCdi11Feature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-1.1");
    }

    @Test
    public void installPaxCdi12Feature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-1.2");
    }

    @Test
    public void installPaxCdiWeldFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-weld");
    }

    @Test
    public void installPaxCdi11WeldFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-1.1-weld");
    }

    @Test
    public void installPaxCdi12WeldFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-1.2-weld");
    }

    @Test
    @Ignore("openwebbeans-spi requires javax.servlet.http in version 2.5.0, this fails")
    public void installPaxCdiOpenwebbeansFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-openwebbeans");
    }

    @Test
    @RunIfNotOnJdk8
    public void installPaxCdiWebFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-web");
    }

    @Test
    @RunIfNotOnJdk8
    public void installPaxCdi11WebFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-1.1-web");
    }

    @Test
    @RunIfNotOnJdk8
    public void installPaxCdi12WebFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-1.2-web");
    }

    @Test
    @RunIfNotOnJdk8
    public void installPaxCdiWebWeldFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-web-weld");
    }

    @Test
    @RunIfNotOnJdk8
    public void installPaxCdi11WebWeldFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-1.1-web-weld");
    }

    @Test
    @Ignore
    @RunIfNotOnJdk8
    public void installPaxCdi12WebWeldFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-1.2-web-weld");
    }

    @Test
    @RunIfNotOnJdk8
    public void installPaxCdiWebOpenwebbeansFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-web-openwebbeans");
    }

}
