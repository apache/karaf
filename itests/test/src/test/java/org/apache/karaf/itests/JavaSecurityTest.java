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

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JavaSecurityTest extends BaseTest {

    @Configuration
    public Option[] config() {
        String version = MavenUtils.getArtifactVersion("org.apache.felix", "org.apache.felix.framework.security");
        String url = maven("org.apache.felix", "org.apache.felix.framework.security", version).getURL();
        Path temp;
        try {
            temp = Files.createTempFile("org.apache.felix.framework.security-" + version + "-", ".jar");
            System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
            try (InputStream is = new URL(url).openStream()) {
                Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        List<Option> options = new ArrayList<>(Arrays.asList(super.config()));
        // Add some extra options used by this test...
        options.addAll(Arrays.asList(
            editConfigurationFilePut("etc/system.properties", "java.security.policy", "${karaf.etc}/all.policy"),
            editConfigurationFilePut("etc/system.properties", "org.osgi.framework.security", "osgi"),
            editConfigurationFilePut("etc/system.properties", "org.osgi.framework.trust.repositories", "${karaf.etc}/trustStore.ks"),
            editConfigurationFilePut("etc/startup.properties", "mvn:org.apache.felix/org.apache.felix.framework.security/" + version, "1"),
            replaceConfigurationFile("system/org/apache/felix/org.apache.felix.framework.security/" + version + "/org.apache.felix.framework.security-" + version + ".jar", temp.toFile())));
        return options.toArray(new Option[] {});
    }

    @Test
    public void testJavaSecurity() throws Exception {
        assertNotNull("Karaf should run under a security manager", System.getSecurityManager());

        BundleService service = getOsgiService(BundleService.class);
        long tried = 0;
        while (true) {
            Map<Bundle, BundleState> incorrect = new HashMap<>();
            for (Bundle bundle : bundleContext.getBundles()) {
                BundleInfo info = service.getInfo(bundle);
                BundleState state = info.getState();
                if (state != BundleState.Active && state != BundleState.Resolved) {
                    incorrect.put(bundle, state);
                }
            }
            if (incorrect.isEmpty()) {
                break;
            } else {
                if (++tried >= 10) {
                    fail("Unable to start bundles correctly: " + incorrect);
                }
                Thread.sleep(100);
            }
        }
    }

}
