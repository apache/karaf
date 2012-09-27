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
public class EnterpriseFeaturesTest extends KarafTestSupport {

    @Test
    public void testTransactionFeature() throws Exception {
        System.out.println(executeCommand("features:install transaction"));
        String transactionFeatureStatus = executeCommand("features:list | grep transaction");
        System.out.println(transactionFeatureStatus);
        assertFalse("transaction feature is not installed", transactionFeatureStatus.isEmpty());
    }

    @Test
    public void testJpaFeature() throws Exception {
        System.out.println(executeCommand("features:install jpa"));
        String jpaFeatureStatus = executeCommand("features:list | grep jpa");
        System.out.println(jpaFeatureStatus);
        assertFalse("jpa feature is not installed", jpaFeatureStatus.isEmpty());
    }

    @Test
    public void testJndiFeature() throws Exception {
        System.out.println(executeCommand("features:install jndi"));
        String jndiFeatureStatus = executeCommand("features:list | grep jndi");
        System.out.println(jndiFeatureStatus);
        assertFalse("jndi feature is not installed", jndiFeatureStatus.isEmpty());
    }

    @Test
    public void testApplicationWithoutIsolationFeature() throws Exception {
        System.out.println(executeCommand("features:install application-without-isolation"));
        String applicationWithoutIsolationFeatureStatus = executeCommand("features:list | grep application-without-isolation");
        System.out.println(applicationWithoutIsolationFeatureStatus);
        assertFalse("application-without-isolation feature is not installed", applicationWithoutIsolationFeatureStatus.isEmpty());
    }

}
