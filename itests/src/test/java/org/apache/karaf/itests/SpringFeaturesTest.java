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

import static org.junit.Assert.assertFalse;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class SpringFeaturesTest extends KarafTestSupport {

    @Test
    public void installSpringFeature() throws Exception {
        executeCommand("feature:install spring");
        String springFeatureStatus = executeCommand("feature:list -i | grep spring");
        assertFalse("spring feature is not installed", springFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringAspectsFeature() throws Exception {
        executeCommand("feature:install spring-aspects");
        String springAspectsFeatureStatus = executeCommand("feature:list -i | grep spring-aspects");
        assertFalse("spring-aspects feature is not installed", springAspectsFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringDmFeature() throws Exception {
        executeCommand("feature:install spring-dm");
        String springDmFeatureStatus = executeCommand("feature:list -i | grep spring-dm");
        assertFalse("spring-dm feature is not installed", springDmFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringDmWebFeature() throws Exception {
        executeCommand("feature:install spring-dm-web");
        String springDmWebFeatureStatus = executeCommand("feature:list -i | grep spring-dm-web");
        assertFalse("spring-dm-web feature is not installed", springDmWebFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringInstrumentFeature() throws Exception {
        executeCommand("feature:install spring-instrument");
        String springInstrumentFeatureStatus = executeCommand("feature:list -i | grep spring-instrument");
        assertFalse("spring-instrument feature is not installed", springInstrumentFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringJdbcFeature() throws Exception {
        executeCommand("feature:install spring-jdbc");
        String springJdbcFeatureStatus = executeCommand("feature:list -i | grep spring-jdbc");
        assertFalse("spring-jdbc feature is not installed", springJdbcFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringJmsFeature() throws Exception {
        executeCommand("feature:install spring-jms");
        String springJmsFeatureStatus = executeCommand("feature:list -i | grep spring-jms");
        assertFalse("spring-jms feature is not installed", springJmsFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringStrutsFeature() throws Exception {
        executeCommand("feature:install spring-struts");
        String springStrutsFeatureStatus = executeCommand("feature:list -i | grep spring-struts");
        assertFalse("spring-struts feature is not installed", springStrutsFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringTestFeature() throws Exception {
        executeCommand("feature:install spring-test");
        String springTestFeatureStatus = executeCommand("feature:list -i | grep spring-test");
        assertFalse("spring-test feature is not installed", springTestFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringOrmFeature() throws Exception {
        executeCommand("feature:install spring-orm");
        String springOrmFeatureStatus = executeCommand("feature:list -i | grep spring-orm");
        assertFalse("spring-orm feature is not installed", springOrmFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringOxmFeature() throws Exception {
        executeCommand("feature:install spring-oxm");
        String springOxmFeatureStatus = executeCommand("feature:list -i | grep spring-oxm");
        assertFalse("spring-oxm feature is not installed", springOxmFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringTxFeature() throws Exception {
        executeCommand("feature:install spring-tx");
        String springTxFeatureStatus = executeCommand("feature:list -i | grep spring-tx");
        assertFalse("spring-tx feature is not installed", springTxFeatureStatus.isEmpty());
    }

    @Test
    public void installSpringWebFeature() throws Exception {
        executeCommand("feature:install spring-web");
        String springWebFeatureStatus = executeCommand("feature:list -i | grep spring-web");
        assertFalse("spring-web feature is not installed", springWebFeatureStatus.isEmpty());
    }

    @Test
    @Ignore
    // TODO fix the spring-web-portlet feature
    public void installSpringWebPortletFeature() throws Exception {
        executeCommand("feature:install spring-web-portlet");
        String springWebPortletFeatureStatus = executeCommand("feature:list -i | grep spring-web-portlet");
        assertFalse("spring-web-portlet feature is not installed", springWebPortletFeatureStatus.isEmpty());
    }

    @Test
    public void installGeminiBlueprintFeature() throws Exception {
        executeCommand("feature:install gemini-blueprint");
        String geminiBlueprintFeatureStatus = executeCommand("feature:list -i | grep gemini-blueprint");
        assertFalse("gemini-blueprint feature is not installed", geminiBlueprintFeatureStatus.isEmpty());
    }

}
