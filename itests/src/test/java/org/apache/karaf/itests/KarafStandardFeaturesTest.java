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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class KarafStandardFeaturesTest extends KarafTestSupport {

    @Test
    public void testBootFeatures() throws Exception {
        // standard feature
        String standardFeatureStatus = executeCommand("feature:list -i | grep standard");
        assertTrue("standard feature is not installed", !standardFeatureStatus.isEmpty());
        // config feature
        String configFeatureStatus = executeCommand("feature:list -i | grep config");
        assertTrue("config feature is not installed", !configFeatureStatus.isEmpty());
        // region feature
        String regionFeatureStatus = executeCommand("feature:list -i | grep region");
        assertTrue("region feature is not installed", !regionFeatureStatus.isEmpty());
        // package feature
        String packageFeatureStatus = executeCommand("feature:list -i | grep package");
        assertTrue("package feature is not installed", !packageFeatureStatus.isEmpty());
        // kar feature
        String karFeatureStatus = executeCommand("feature:list -i | grep kar");
        assertTrue("kar feature is not installed", !karFeatureStatus.isEmpty());
        // ssh feature
        String sshFeatureStatus = executeCommand("feature:list -i | grep ssh");
        assertTrue("ssh feature is not installed", !sshFeatureStatus.isEmpty());
        // management feature
        String managementFeatureStatus = executeCommand("feature:list -i | grep management");
        assertTrue("management feature is not installed", !managementFeatureStatus.isEmpty());
    }

    @Test
    public void installWrapperFeature() throws Exception {
        System.out.println(executeCommand("feature:install wrapper"));
        String wrapperFeatureStatus = executeCommand("feature:list -i | grep wrapper");
        assertTrue("wrapper feature is not installed", !wrapperFeatureStatus.isEmpty());
    }

    @Test
    public void installObrFeature() throws Exception {
        System.out.println(executeCommand("feature:install obr"));
        String obrFeatureStatus = executeCommand("feature:list -i | grep obr");
        assertTrue("obr feature is not installed", !obrFeatureStatus.isEmpty());
    }

    @Test
    public void installJettyFeature() throws Exception {
        System.out.println(executeCommand("feature:install jetty"));
        String jettyFeatureStatus = executeCommand("feature:list -i | grep jetty");
        assertTrue("jetty feature is not installed", !jettyFeatureStatus.isEmpty());
    }

    @Test
    public void installHttpFeature() throws Exception {
        System.out.println(executeCommand("feature:install http"));
        String httpFeatureStatus = executeCommand("feature:list -i | grep http");
        assertTrue("http feature is not installed", !httpFeatureStatus.isEmpty());
    }

    @Test
    public void installHttpWhiteboardFeature() throws Exception {
        System.out.println(executeCommand("feature:install http-whiteboard"));
        String httpWhiteboardFeatureStatus = executeCommand("feature:list -i | grep http-whiteboard");
        assertTrue("http-whiteboard feature is not installed", !httpWhiteboardFeatureStatus.isEmpty());
    }

    @Test
    public void installWarFeature() throws Exception {
        System.out.println(executeCommand("feature:install war"));
        String warFeatureStatus = executeCommand("feature:list -i | grep war");
        assertTrue("war feature is not installed", !warFeatureStatus.isEmpty());
    }

    @Test
    public void installWebConsoleFeature() throws Exception {
        System.out.println(executeCommand("feature:install webconsole"));
        String webConsoleFeatureStatus = executeCommand("feature:list -i | grep webconsole");
        assertTrue("webconsole feature is not installed", !webConsoleFeatureStatus.isEmpty());
    }

    @Test
    public void installSchedulerFeature() throws Exception {
        System.out.println(executeCommand("feature:install scheduler"));
        String schedulerFeatureStatus = executeCommand("feature:list -i | grep scheduler");
        assertTrue("scheduler feature is not installed", !schedulerFeatureStatus.isEmpty());
    }

    @Test
    public void installEventAdminFeature() throws Exception {
        System.out.println(executeCommand("feature:install eventadmin"));
        String eventAdminFeatureStatus = executeCommand("feature:list -i | grep eventadmin");
        assertTrue("eventadmin feature is not installed", !eventAdminFeatureStatus.isEmpty());
    }

    @Test
    public void installJasyptEncryptionFeature() throws Exception {
        System.out.println(executeCommand("feature:install jasypt-encryption"));
        String jasyptEncryptionFeatureStatus = executeCommand("feature:list -i | grep jasypt-encryption");
        assertTrue("jasypt-encryption feature is not installed", !jasyptEncryptionFeatureStatus.isEmpty());
    }

    @Test
    public void installScrFeature() throws Exception {
        System.out.println(executeCommand("feature:install scr"));
        String scrFeatureStatus = executeCommand("feature:list -i | grep scr");
        assertTrue("scr feature is not installed", !scrFeatureStatus.isEmpty());
    }

}
