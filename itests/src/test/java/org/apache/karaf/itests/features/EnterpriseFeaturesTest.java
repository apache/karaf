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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class EnterpriseFeaturesTest extends KarafTestSupport {

    @Test
    public void installTransactionFeature() throws Exception {
        installAssertAndUninstallFeatures("transaction");
    }

    @Test
    public void installJpaFeature() throws Exception {
    	installAssertAndUninstallFeatures("jpa");
    }

    @Test
    public void installOpenJpaFeature() throws Exception {
        installAssertAndUninstallFeatures("openjpa");
    }

    @Test
    public void installHibernateFeature() throws Exception {
        installAssertAndUninstallFeatures("hibernate");
    }

    @Test
    public void installHibernateEnversFeature() throws Exception {
        installAssertAndUninstallFeatures("hibernate-envers");
    }

    @Test
    public void installHibernateValidatorFeature() throws Exception {
        installAssertAndUninstallFeatures("hibernate-validator");
    }

    @Test
    public void installJndiFeature() throws Exception {
    	installAssertAndUninstallFeatures("jndi");
    }

    @Test
    public void installJdbcFeature() throws Exception {
        installAssertAndUninstallFeatures("jdbc");
    }

    @Test
    public void installJmsFeature() throws Exception {
        installAssertAndUninstallFeatures("jms");
    }

    @Test
    @Ignore("Pax-cdi depends on scr feature [2.3,3.5) so it does not work with 4.0")
    public void installOpenWebBeansFeature() throws Exception {
        installAssertAndUninstallFeatures("openwebbeans");
    }

    @Test
    @Ignore("Pax-cdi depends on scr feature [2.3,3.5) so it does not work with 4.0")
    public void installWeldFeature() throws Exception {
        installAssertAndUninstallFeatures("weld");
    }

    @Test
    public void installApplicationWithoutIsolationFeature() throws Exception {
    	installAssertAndUninstallFeatures("application-without-isolation");
    }

    @Test
    public void installSubsystems() throws Exception {
        installAssertAndUninstallFeatures("subsystems");
    }

}
