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

import org.apache.karaf.itests.BaseTest;
import org.apache.karaf.itests.util.RunIfRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class PaxCdiFeaturesTest extends BaseTest {

    @Rule
    public RunIfRule rule = new RunIfRule();

    
    @Test
    public void installPaxCdiFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi");
    }

    @Test
    public void installPaxCdiWeldFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-weld");
    }

    @Test
    public void installPaxCdiOpenwebbeansFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-openwebbeans");
    }

    @Test
    public void installPaxCdiWebFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-web");
    }

    @Test
    public void installPaxCdiWebWeldFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-weld", "pax-cdi-web");
    }

    @Test
    public void installPaxCdiWebOpenwebbeansFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-cdi-openwebbeans", "pax-cdi-web");
    }

    @Configuration
    public Option[] config() {
        return options(composite(
                super.config()),
                features(maven().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-features").versionAsInProject().type("xml").classifier("features"))
        );
    }
}
