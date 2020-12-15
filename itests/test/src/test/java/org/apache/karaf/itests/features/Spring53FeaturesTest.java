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

import org.apache.karaf.itests.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class Spring53FeaturesTest extends BaseTest {

    @Test
    public void installSpringFeature() throws Exception {
        installAssertAndUninstallFeature("spring", System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringAspectsFeature() throws Exception {
        installAssertAndUninstallFeature("spring-aspects", System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringInstrumentFeature() throws Exception {
        installAssertAndUninstallFeature("spring-instrument", System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringJdbcFeature() throws Exception {
        installAssertAndUninstallFeature("spring-jdbc", System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringJmsFeature() throws Exception {
        installAssertAndUninstallFeature("spring-jms", System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringMessagingFeature() throws Exception {
        installAssertAndUninstallFeature("spring-messaging", System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringTestFeature() throws Exception {
        installAssertAndUninstallFeature("spring-test", System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringOrmFeature() throws Exception {
        installAssertAndUninstallFeature("spring-orm", System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringOxmFeature() throws Exception {
        installAssertAndUninstallFeature("spring-oxm", System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringTxFeature() throws Exception {
        installAssertAndUninstallFeature("spring-tx", System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringWebFeature() throws Exception {
        installAssertAndUninstallFeatures("pax-web-http", "spring-web/" + System.getProperty("spring53.version"));
    }

    @Test
    public void installSpringWebSocketFeature() throws Exception {
        installAndAssertFeature("pax-web-http");
        installAssertAndUninstallFeatures("pax-web-http", "spring-websocket/" + System.getProperty("spring53.version"));
    }

}
