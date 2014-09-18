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
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class EnterpriseFeaturesTest extends KarafTestSupport {

    @Test
    public void testTransactionFeatures() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING TRANSACTION 1.0.0 FEATURE =====");
        installAndAssertFeature("transaction", "1.0.0");
        System.out.println("");
        System.out.println("===== TESTING TRANSACTION 1.0.1 FEATURE =====");
        installAndAssertFeature("transaction", "1.0.1");
        System.out.println("");
        System.out.println("===== TESTING TRANSACTION 1.1.0 FEATURE =====");
        installAndAssertFeature("transaction", "1.1.0");
    }

    @Test
    public void testConnectorFeatures() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING CONNECTOR 2.2.2 FEATURE =====");
        installAndAssertFeature("connector", "2.2.2");
        System.out.println("");
        System.out.println("===== TESTING CONNECTOR 3.1.1 FEATURE =====");
        installAndAssertFeature("connector", "3.1.1");
    }

    @Test
    public void testJpaFeatures() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING JPA 1.0.1 FEATURE =====");
        installAndAssertFeature("jpa", "1.0.1");
        System.out.println("");
        System.out.println("===== TESTING JPA 1.0.2 FEATURE =====");
        installAndAssertFeature("jpa", "1.0.2");
    }

    @Test
    public void testOpenJpaFeatures() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING OPENJPA 2.2.2 FEATURE =====");
        installAndAssertFeature("openjpa", "2.2.2");
        System.out.println("");
        System.out.println("===== TESTING OPENJPA 2.3.0 FEATURE =====");
        installAndAssertFeature("openjpa", "2.3.0");
    }

    @Test
    public void testHibernateFeatures() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING HIBERNATE " + System.getProperty("hibernate3.version") + " FEATURE =====");
        installAndAssertFeature("hibernate", System.getProperty("hibernate3.version"));
        System.out.println("");
        System.out.println("===== TESTING HIBERNATE " + System.getProperty("hibernate42.version") + " FEATURE =====");
        installAndAssertFeature("hibernate", System.getProperty("hibernate42.version"));
        System.out.println("");
        System.out.println("===== TESTING HIBERNATE " + System.getProperty("hibernate43.version") + " FEATURE =====");
        installAndAssertFeature("hibernate", System.getProperty("hibernate43.version"));
    }

    @Test
    public void testHibernateEnversFeatures() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING HIBERNATE-ENVERS " + System.getProperty("hibernate42.version") + " FEATURE =====");
        installAndAssertFeature("hibernate-envers", System.getProperty("hibernate42.version"));
        System.out.println("");
        System.out.println("===== TESTING HIBERNATE-ENVERS " + System.getProperty("hibernate43.version") + " FEATURE =====");
        installAndAssertFeature("hibernate-envers", System.getProperty("hibernate43.version"));
    }

    @Test
    public void testHibernateValidatorFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING HIBERNATE-VALIDATOR FEATURE =====");
        installAndAssertFeature("hibernate-validator");
    }

    @Test
    public void testJndiFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING JNDI FEATURE =====");
        installAndAssertFeature("jndi");
    }

    @Test
    public void testJdbcFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING JDBC FEATURE =====");
        installAndAssertFeature("jdbc");
    }

    @Test
    public void testJmsFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING JMS FEATURE =====");
        installAndAssertFeature("jms");
    }

    @Test
    public void testOpenwebbeansFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING OPENWEBBEANS FEATURE =====");
        installAndAssertFeature("openwebbeans");
    }

    @Test
    public void testWeldFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING WELD FEATURE =====");
        installAndAssertFeature("weld");
    }

    @Test
    public void testApplicationWithoutIsolationFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING APPLICATION-WITHOUT-ISOLATION FEATURE =====");
        installAndAssertFeature("application-without-isolation");
    }

}
