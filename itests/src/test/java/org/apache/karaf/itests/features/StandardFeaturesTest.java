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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class StandardFeaturesTest extends KarafTestSupport {

    @Test
    public void installAriesAnnotationFeature() throws Exception {
        installAssertAndUninstallFeature("aries-annotation");
    }
    
    @Test
    public void installWrapperFeature() throws Exception {
        installAssertAndUninstallFeature("wrapper");
    }
    
    @Test
    public void installObrFeature() throws Exception {
        installAssertAndUninstallFeature("obr");
    }

    @Test
    public void installConfigFeature() throws Exception {
        installAssertAndUninstallFeature("config");
    }
    
    @Test
    public void installRegionFeature() throws Exception {
        installAssertAndUninstallFeature("region");
    }
    
    @Test
    public void installPackageFeature() throws Exception {
        installAssertAndUninstallFeature("package");
    }

    @Test
    public void installHttpFeature() throws Exception {
        installAssertAndUninstallFeature("http");
    }

    @Test
    public void installHttpWhiteboardFeature() throws Exception {
        installAssertAndUninstallFeature("http-whiteboard");
    }

    @Test
    public void installWarFeature() throws Exception {
        installAssertAndUninstallFeature("war");
    }
    
    @Test
    public void installKarFeature() throws Exception {
        installAssertAndUninstallFeature("kar");
    }

    @Test
    public void installWebConsoleFeature() throws Exception {
        installAssertAndUninstallFeature("webconsole");
    }

    @Test
    public void installSSHFeature() throws Exception {
        installAssertAndUninstallFeature("ssh");
    }
    
    @Test
    public void installManagementFeature() throws Exception {
        installAssertAndUninstallFeature("management");
    }
    
    @Test
    public void installSchedulerFeature() throws Exception {
        installAssertAndUninstallFeature("scheduler");
    }

    @Test
    public void installEventAdminFeature() throws Exception {
        installAssertAndUninstallFeature("eventadmin");
    }

    @Test
    public void installJasyptEncryptionFeature() throws Exception {
        installAssertAndUninstallFeature("jasypt-encryption");
    }

    @Test
    public void installScrFeature() throws Exception {
        installAssertAndUninstallFeature("scr");
    }

}
