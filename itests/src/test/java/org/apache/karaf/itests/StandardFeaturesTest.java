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
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.junit.Assert.assertFalse;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class StandardFeaturesTest extends KarafTestSupport {

    @Test
    public void testBootFeatures() throws Exception {
        // config
        String configFeatureStatus = executeCommand("feature:list -i | grep config");
        System.out.println(configFeatureStatus);
        assertFalse("config feature is not installed", configFeatureStatus.isEmpty());
        // ssh
        String sshFeatureStatus = executeCommand("feature:list -i | grep ssh");
        System.out.println(sshFeatureStatus);
        assertFalse("ssh feature is not installed", sshFeatureStatus.isEmpty());
        // management
        String managementFeatureStatus = executeCommand("feature:list -i | grep management");
        System.out.println(managementFeatureStatus);
        assertFalse("management feature is not installed", managementFeatureStatus.isEmpty());
        // kar
        String karFeatureStatus = executeCommand("feature:list -i | grep i kar");
        System.out.println(karFeatureStatus);
        assertFalse("kar feature is not installed", karFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring"));
        String springFeatureStatus = executeCommand("feature:list -i | grep spring");
        System.out.println(springFeatureStatus);
        assertFalse("spring feature is not installed", springFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringAspectsFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-aspects"));
        String springAspectsFeatureStatus = executeCommand("feature:list -i | grep spring-aspects");
        System.out.println(springAspectsFeatureStatus);
        assertFalse("spring-aspects feature is not installed", springAspectsFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringDmFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-dm"));
        String springDmFeatureStatus = executeCommand("feature:list -i | grep spring-dm");
        System.out.println(springDmFeatureStatus);
        assertFalse("spring-dm feature is not installed", springDmFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringDmWebFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-dm-web"));
        String springDmWebFeatureStatus = executeCommand("feature:list -i | grep spring-dm-web");
        System.out.println(springDmWebFeatureStatus);
        assertFalse("spring-dm-web feature is not installed", springDmWebFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringInstrumentFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-instrument"));
        String springInstrumentFeatureStatus = executeCommand("feature:list -i | grep spring-instrument");
        System.out.println(springInstrumentFeatureStatus);
        assertFalse("spring-instrument feature is not installed", springInstrumentFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringJdbcFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-jdbc"));
        String springJdbcFeatureStatus = executeCommand("feature:list -i | grep spring-jdbc");
        System.out.println(springJdbcFeatureStatus);
        assertFalse("spring-jdbc feature is not installed", springJdbcFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringJmsFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-jms"));
        String springJmsFeatureStatus = executeCommand("feature:list -i | grep spring-jms");
        System.out.println(springJmsFeatureStatus);
        assertFalse("spring-jms feature is not installed", springJmsFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringStrutsFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-struts"));
        String springStrutsFeatureStatus = executeCommand("feature:list -i | grep spring-struts");
        System.out.println(springStrutsFeatureStatus);
        assertFalse("spring-struts feature is not installed", springStrutsFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringTestFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-test"));
        String springTestFeatureStatus = executeCommand("feature:list -i | grep spring-test");
        System.out.println(springTestFeatureStatus);
        assertFalse("spring-test feature is not installed", springTestFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringOrmFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-orm"));
        String springOrmFeatureStatus = executeCommand("feature:list -i | grep spring-orm");
        System.out.println(springOrmFeatureStatus);
        assertFalse("spring-orm feature is not installed", springOrmFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringOxmFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-oxm"));
        String springOxmFeatureStatus = executeCommand("feature:list -i | grep spring-oxm");
        System.out.println(springOxmFeatureStatus);
        assertFalse("spring-oxm feature is not installed", springOxmFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringTxFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-tx"));
        String springTxFeatureStatus = executeCommand("feature:list -i | grep spring-tx");
        System.out.println(springTxFeatureStatus);
        assertFalse("spring-tx feature is not installed", springTxFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringWebFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-web"));
        String springWebFeatureStatus = executeCommand("feature:list -i | grep spring-web");
        System.out.println(springWebFeatureStatus);
        assertFalse("spring-web feature is not installed", springWebFeatureStatus.isEmpty());
    }

    @Test
    public void testSpringWebPortletFeature() throws Exception {
        System.out.println(executeCommand("feature:install spring-web-portlet"));
        String springWebPortletFeatureStatus = executeCommand("feature:list -i | grep spring-web-portlet");
        System.out.println(springWebPortletFeatureStatus);
        assertFalse("spring-web-portlet feature is not installed", springWebPortletFeatureStatus.isEmpty());
    }

    @Test
    public void testWrapperFeature() throws Exception {
        System.out.println(executeCommand("feature:install wrapper"));
        String wrapperFeatureStatus = executeCommand("feature:list -i | grep wrapper");
        System.out.println(wrapperFeatureStatus);
        assertFalse("wrapper feature is not installed", wrapperFeatureStatus.isEmpty());
    }

    @Test
    public void testObrFeature() throws Exception {
        System.out.println(executeCommand("feature:install obr"));
        String obrFeatureStatus = executeCommand("feature:list -i | grep obr");
        System.out.println(obrFeatureStatus);
        assertFalse("obr feature is not installed", obrFeatureStatus.isEmpty());
    }

    @Test
    public void testJettyFeature() throws Exception {
        System.out.println(executeCommand("feature:install jetty"));
        String jettyFeatureStatus = executeCommand("feature:list -i | grep jetty");
        System.out.println(jettyFeatureStatus);
        assertFalse("jetty feature is not installed", jettyFeatureStatus.isEmpty());
    }

    @Test
    public void testHttpFeature() throws Exception {
        System.out.println(executeCommand("feature:install http"));
        String httpFeatureStatus = executeCommand("feature:list -i | grep http");
        System.out.println(httpFeatureStatus);
        assertFalse("http feature is not installed", httpFeatureStatus.isEmpty());
    }

    @Test
    public void testHttpWhiteboardFeature() throws Exception {
        System.out.println(executeCommand("feature:install http-whiteboard"));
        String httpWhiteboardFeatureStatus = executeCommand("feature:list -i | grep http-whiteboard");
        System.out.println(httpWhiteboardFeatureStatus);
        assertFalse("http-whiteboard feature is not installed", httpWhiteboardFeatureStatus.isEmpty());
    }

    @Test
    public void testWarFeature() throws Exception {
        System.out.println(executeCommand("feature:install war"));
        String warFeatureStatus = executeCommand("feature:list -i | grep war");
        System.out.println(warFeatureStatus);
        assertFalse("war feature is not installed", warFeatureStatus.isEmpty());
    }

    @Test
    public void testWebconsoleFeature() throws Exception {
        System.out.println(executeCommand("feature:install webconsole"));
        String webconsoleFeatureStatus = executeCommand("feature:list -i | grep webconsole");
        System.out.println(webconsoleFeatureStatus);
        assertFalse("webconsole feature is not installed", webconsoleFeatureStatus.isEmpty());
    }

    @Test
    public void testEventadminFeature() throws Exception {
        System.out.println(executeCommand("feature:install eventadmin"));
        String eventadminFeatureStatus = executeCommand("feature:list -i | grep eventadmin");
        System.out.println(eventadminFeatureStatus);
        assertFalse("eventadmin feature is not installed", eventadminFeatureStatus.isEmpty());
    }

    @Test
    public void testJasyptEncryptionFeature() throws Exception {
        System.out.println(executeCommand("feature:install jasypt-encryption"));
        String jasyptEncryptionFeatureStatus = executeCommand("feature:list -i | grep jasypt-encryption");
        System.out.println(jasyptEncryptionFeatureStatus);
        assertFalse("jasypt-encryption feature is not installed", jasyptEncryptionFeatureStatus.isEmpty());
    }

}
