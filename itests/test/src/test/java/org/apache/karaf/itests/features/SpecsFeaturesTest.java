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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SpecsFeaturesTest extends BaseTest {

    @Test
    public void installAsmFeature() throws Exception {
        installAssertAndUninstallFeatures("asm");
    }

    @Test
    public void installSpiflyFeature() throws Exception {
        installAssertAndUninstallFeatures("spifly");
    }

    @Test
    public void installJakartaAnnotationFeature() throws Exception {
        installAssertAndUninstallFeatures("jakarta.annotation");
    }

    @Test
    public void installActivationFeature() throws Exception {
        installAssertAndUninstallFeatures("activation");
    }

    @Test
    public void installJaxbFeature() throws Exception {
        installAssertAndUninstallFeatures("jaxb");
    }

    @Test
    public void installJwsFeature() throws Exception {
        installAssertAndUninstallFeatures("jws");
    }

    @Test
    public void installJaxrsFeature() throws Exception {
        installAssertAndUninstallFeatures("jaxrs");
    }

    @Test
    public void installJaxwsFeature() throws Exception {
        installAssertAndUninstallFeatures("jaxws");
    }

    @Test
    public void installMailFeature() throws Exception {
        installAssertAndUninstallFeatures("mail");
    }

    @Test
    public void installJacksonFeature() throws Exception {
        installAssertAndUninstallFeatures("jackson");
    }

    @Test
    public void installJacksonJaxrsFeature() throws Exception {
        installAssertAndUninstallFeatures("jackson-jaxrs");
    }

    @Test
    public void installStaxFeature() throws Exception {
        installAssertAndUninstallFeatures("stax");
    }

    @Test
    public void installStax2Feature() throws Exception {
        installAssertAndUninstallFeatures("stax2");
    }

    @Test
    public void installSaajFeature() throws Exception {
        installAssertAndUninstallFeatures("saaj");
    }

    @Test
    public void installNamespaceFeature() throws Exception {
        installAssertAndUninstallFeatures("namespace");
    }

    @Test
    public void installUtilFeature() throws Exception {
        installAssertAndUninstallFeatures("util");
    }

    @Test
    public void installCdiFeature() throws Exception {
        installAssertAndUninstallFeatures("cdi");
    }

    @Test
    public void installConverterFeature() throws Exception {
        installAssertAndUninstallFeatures("converter");
    }

    @Test
    public void installXBeanFeature() throws Exception {
        installAssertAndUninstallFeatures("xbean");
    }

    @Test
    public void installGroovyFeature() throws Exception {
        installAssertAndUninstallFeatures("groovy");
    }

}
