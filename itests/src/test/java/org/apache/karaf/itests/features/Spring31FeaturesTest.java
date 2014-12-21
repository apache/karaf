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
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class Spring31FeaturesTest extends KarafTestSupport {

    // Spring 3.1.x features

    @Test
    public void testSpringFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringAspectsFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-ASPECTS "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-aspects", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringInstrumentFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-INSTRUMENT "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-instrument", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringJdbcFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-JDBC "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-jdbc", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringJmsFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-JMS "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-jms", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringStrutsFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-STRUTS "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-struts", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringTestFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-TEST "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-test", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringOrmFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-ORM "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-orm", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringOxmFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-OXM "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-oxm", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringTxFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-TX "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-tx", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringWebFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-WEB "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-web", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringWebPortletFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-WEB-PORTLET " + System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-web-portlet", System.getProperty("spring31.version"));
    }

}
