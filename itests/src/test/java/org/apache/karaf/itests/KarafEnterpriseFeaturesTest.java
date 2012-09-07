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
public class KarafEnterpriseFeaturesTest extends KarafTestSupport {

    @Test
    public void installTransactionFeature() throws Exception {
        executeCommand("feature:install transaction");
        String transactionFeatureStatus = executeCommand("feature:list -i | grep transaction");
        assertFalse("transaction feature is not installed", transactionFeatureStatus.isEmpty());
    }

    @Test
    public void installJpaFeature() throws Exception {
        executeCommand("feature:install jpa");
        String jpaFeatureStatus = executeCommand("feature:list -i | grep jpa");
        assertFalse("jpa feature is not installed", jpaFeatureStatus.isEmpty());
    }

    @Test
    public void installJndiFeature() throws Exception {
        executeCommand("feature:install jndi");
        String jndiFeatureStatus = executeCommand("feature:list -i | grep jndi");
        assertFalse("jndi feature is not installed", jndiFeatureStatus.isEmpty());
    }

    @Test
    public void installApplicationWithoutIsolationFeature() throws Exception {
        executeCommand("feature:install application-without-isolation");
        String applicationWithoutIsolationFeatureStatus = executeCommand("feature:list -i | grep application-without-isolation");
        assertFalse("application-without-isolation feature is not installed", applicationWithoutIsolationFeatureStatus.isEmpty());
    }

}
