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

    // Spring security feature

    @Test
    public void testSpringSecurityFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING SPRING-SECURITY FEATURE =====");
        installAndAssertFeature("spring-security");
    }

}
