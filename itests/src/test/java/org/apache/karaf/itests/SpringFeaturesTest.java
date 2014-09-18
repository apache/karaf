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
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SpringFeaturesTest extends KarafTestSupport {

    // Spring DM features

    @Test
    public void testSpringDmFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-DM FEATURE =====");
        installAndAssertFeature("spring-dm");
    }

    @Test
    public void testSpringDmWebFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-DM-WEB FEATURE =====");
        installAndAssertFeature("spring-dm-web");
    }

    // Spring 3.1.x features

    @Test
    public void testSpring31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringAspects31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-ASPECTS "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-aspects", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringInstrument31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-INSTRUMENT "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-instrument", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringJdbc31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-JDBC "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-jdbc", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringJms31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-JMS "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-jms", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringStruts31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-STRUTS "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-struts", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringTest31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-TEST "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-test", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringOrm31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-ORM "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-orm", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringOxm31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-OXM "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-oxm", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringTx31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-TX "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-tx", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringWeb31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-WEB "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-web", System.getProperty("spring31.version"));
    }

    @Test
    public void testSpringWebPortlet31Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-WEB-PORTLET "+ System.getProperty("spring31.version") + " FEATURE =====");
        installAndAssertFeature("spring-web-portlet", System.getProperty("spring31.version"));
    }

    // Spring 3.2.x features

    @Test
    public void testSpring32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringAspects32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-ASPECTS "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-aspects", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringInstrument32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-INSTRUMENT "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-instrument", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringJdbc32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-JDBC "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-jdbc", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringJms32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-JMS "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-jms", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringStruts32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-STRUTS "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-struts", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringTest32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-TEST "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-test", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringOrm32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-ORM "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-orm", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringOxm32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-OXM "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-oxm", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringTx32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-TX "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-tx", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringWeb32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-WEB "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-web", System.getProperty("spring32.version"));
    }

    @Test
    public void testSpringWebPortlet32Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-WEB-PORTLET "+ System.getProperty("spring32.version") + " FEATURE =====");
        installAndAssertFeature("spring-web-portlet", System.getProperty("spring32.version"));
    }

    // Spring 4.0.x features

    @Test
    public void testSpring40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringAspects40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-ASPECTS "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-aspects", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringInstrument40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-INSTRUMENT "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-instrument", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringJdbc40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-JDBC "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-jdbc", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringJms40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-JMS "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-jms", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringTest40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-TEST "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-test", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringOrm40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-ORM "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-orm", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringOxm40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-OXM "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-oxm", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringTx40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-TX "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-tx", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringWeb40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-WEB "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-web", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringWebPortlet40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-WEB-PORTLET "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-web-portlet", System.getProperty("spring40.version"));
    }

    @Test
    public void testSpringWebsocket40Feature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-WEBSOCKET "+ System.getProperty("spring40.version") + " FEATURE =====");
        installAndAssertFeature("spring-websocket", System.getProperty("spring40.version"));
    }

    // Spring security feature

    @Test
    public void testSpringSecurityFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-SECURITY FEATURE =====");
        installAndAssertFeature("spring-security");
    }

}
