/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class ConditionalFeaturesTest extends KarafTestSupport {
    /*
    @Inject
    private FeaturesService featuresService;

    @Inject
    private BundleContext bundleContext;
    
    @Inject
    BootFinished bootFinished;


    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }


    @Configuration
    public Option[] config() {
        
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("zip").versionAsInProject();
        return new Option[]{
            karafDistributionConfiguration().frameworkUrl(karafUrl),
            KarafDistributionOption.editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", KarafTestSupport.HTTP_PORT)
        };
    }
    */

    @Test
    public void testScr() throws Exception {
        //Remove management and install scr
        featureService.uninstallFeature("management");
        featureService.installFeature("scr");
        assertBundleNotInstalled("org.apache.karaf.scr.management");

        //Add management back
        featureService.installFeature("management");
        assertBundleInstalled("org.apache.karaf.scr.management");
    }

    @Test
    public void testWebconsole() throws Exception {
        try {
            featureService.uninstallFeature("eventadmin");
        } catch (Exception e) {
        }
        featureService.installFeature("webconsole");

        assertBundleInstalled("org.apache.karaf.webconsole.features");
        assertBundleInstalled("org.apache.karaf.webconsole.instance");
        assertBundleInstalled("org.apache.karaf.webconsole.gogo");
        assertBundleInstalled("org.apache.karaf.webconsole.http");

        assertBundleNotInstalled("org.apache.felix.webconsole.plugins.event");

        //Add eventadmin
        try {
            featureService.installFeature("eventadmin");
        } catch (Exception ex) {
          //ignore as the eventadmin activator might throw an error.
        }
        assertBundleInstalled("org.apache.felix.webconsole.plugins.event");
    }
}
