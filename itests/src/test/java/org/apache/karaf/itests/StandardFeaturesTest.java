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
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class StandardFeaturesTest extends KarafTestSupport {

    @Test
    public void testBootFeatures() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING BOOT FEATURES =====");
        assertFeatureInstalled("karaf-framework");
        assertFeatureInstalled("shell");
        assertFeatureInstalled("features");
        assertFeatureInstalled("service-security");
        assertFeatureInstalled("admin");
        assertFeatureInstalled("config");
        assertFeatureInstalled("ssh");
        assertFeatureInstalled("management");
        assertFeatureInstalled("kar");
        assertFeatureInstalled("deployer");
        assertFeatureInstalled("diagnostic");
    }

    @Test
    public void testWrapperFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING WRAPPER FEATURE =====");
        installAndAssertFeature("wrapper");
    }

    @Test
    public void testObrFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING OBR FEATURE =====");
        installAndAssertFeature("obr");
    }

    @Test
    public void testJettyFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING JETTY FEATURE =====");
        installAndAssertFeature("jetty");
    }

    @Test
    public void testHttpFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING HTTP FEATURE =====");
        installAndAssertFeature("http");
    }

    @Test
    public void testHttpWhiteboardFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING HTTP-WHITEBOARD FEATURE =====");
        installAndAssertFeature("http-whiteboard");
    }

    @Test
    public void testWarFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING WAR FEATURE =====");
        installAndAssertFeature("war");
    }

    @Test
    public void testWebconsoleFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING WEBCONSOLE FEATURE =====");
        installAndAssertFeature("webconsole");
    }

    @Test
    public void testEventadminFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING EVENTADMIN FEATURE =====");
        installAndAssertFeature("eventadmin");
    }

    @Test
    public void testJasyptEncryptionFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING JASYPT-ENCRYPTION FEATURE =====");
        installAndAssertFeature("jasypt-encryption");
    }

    @Test
    public void testBlueprintWebFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING BLUEPRINT-WEB FEATURE =====");
        installAndAssertFeature("blueprint-web");
    }

    @Test
    public void testScrFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SCR FEATURE =====");
        installAndAssertFeature("scr");
    }

}
