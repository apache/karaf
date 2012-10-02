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

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.CoreOptions.maven;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@RunWith(JUnit4TestRunner.class)
public class ConditionalFeaturesTest {

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
            KarafDistributionOption.editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", "9080")
        };
    }

    @Test
    public void testScr() throws Exception {
        //Remove management and install scr
        featuresService.uninstallFeature("management");
        featuresService.installFeature("scr");
        Assert.assertFalse(isBundleInstalled("org.apache.karaf.scr.management"));

        //Add management back
        featuresService.installFeature("management");
        Assert.assertTrue(isBundleInstalled("org.apache.karaf.scr.management"));
    }

    @Test
    public void testWebconsole() throws Exception {
        featuresService.installFeature("webconsole");

        Assert.assertTrue(isBundleInstalled("org.apache.karaf.webconsole.features"));
        Assert.assertTrue(isBundleInstalled("org.apache.karaf.webconsole.instance"));
        Assert.assertTrue(isBundleInstalled("org.apache.karaf.webconsole.gogo"));
        Assert.assertTrue(isBundleInstalled("org.apache.karaf.webconsole.http"));

        Assert.assertFalse(isBundleInstalled("org.apache.felix.webconsole.plugins.event"));

        //Add eventadmin
        try {
            featuresService.installFeature("eventadmin");
        } catch (Exception ex) {
          //ignore as the eventadmin activator might throw an error.
        }
        Assert.assertTrue(isBundleInstalled("org.apache.felix.webconsole.plugins.event"));
    }



    private boolean isBundleInstalled(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return true;
            }
        }
        return false;
    }
}
