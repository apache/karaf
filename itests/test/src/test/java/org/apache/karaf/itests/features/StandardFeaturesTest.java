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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class StandardFeaturesTest extends BaseTest {

    @Test
    public void checkInstalledFeaturesBoot() throws Exception {
        System.out.println("===>checkInstalledFeaturesBoot");
        assertFeatureInstalled("wrap");
        assertFeatureInstalled("shell");
        assertFeatureInstalled("jaas");
        assertFeatureInstalled("ssh");
        assertFeatureInstalled("management");
        assertFeatureInstalled("bundle");
        assertFeatureInstalled("config");
        assertFeatureInstalled("deployer");
        assertFeatureInstalled("diagnostic");
        assertFeatureInstalled("instance");
        assertFeatureInstalled("kar");
        assertFeatureInstalled("log");
        assertFeatureInstalled("package");
        assertFeatureInstalled("service");
        assertFeatureInstalled("system");
    }

    public void installServiceSecurityFeature() throws Exception {
        System.out.println("===>installServiceSecurityFeature");
        installAssertAndUninstallFeatures("service-security");
    }

    @Test
    public void installAriesBlueprintWebFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http", "blueprint-web");
    }
    
    @Test
    public void installWrapperFeature() throws Exception {
        installAssertAndUninstallFeatures("wrapper");
    }
    
    @Test
    public void installObrFeature() throws Exception {
        installAssertAndUninstallFeatures("obr");
    }

    @Test
    public void installHttpFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http");
    }

    @Test
    public void installHttpWhiteboardFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http-whiteboard");
    }

    @Test
    public void installWarFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-war");
    }

    @Test
    public void installWebConsoleFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http", "webconsole");
    }

    @Test
    public void installFelixHttplisteFeature() throws Exception {
        installAssertAndUninstallFeatures("felix-httplite");
    }

    @Test
    public void installFelixHttpFeature() throws Exception {
        installAssertAndUninstallFeatures("felix-http");
    }
    
    @Test
    public void installSchedulerFeature() throws Exception {
        installAssertAndUninstallFeatures("scheduler");
    }

    @Test
    public void installEventAdminFeature() throws Exception {
        installAssertAndUninstallFeatures("eventadmin");
    }

    @Test
    public void installJasyptEncryptionFeature() throws Exception {
        installAssertAndUninstallFeatures("jasypt-encryption");
    }

    @Test
    public void installScrFeature() throws Exception {
        installAssertAndUninstallFeatures("scr");
    }

    @Test
    public void installJolokiaFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http", "jolokia");
    }

}
