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
package org.apache.karaf.itests.examples;

import org.apache.karaf.examples.blueprint.client.ClientService;
import org.apache.karaf.examples.blueprint.common.Booking;
import org.apache.karaf.examples.blueprint.common.BookingService;
import org.apache.karaf.itests.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.util.List;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BlueprintExampleTest extends BaseTest {

    @Test
    public void test() throws Exception {
        // add blueprint example features repository
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-blueprint-example-features/" + System.getProperty("karaf.version") + "/xml");

        // install the karaf-blueprint-example-provider feature
        installAndAssertFeature("karaf-blueprint-example-provider");
        // check the provider service
        assertServiceAvailable(BookingService.class);

        // install the karaf-blueprint-example-client feature
        installAndAssertFeature("karaf-blueprint-example-client");

        // get the client service
        assertServiceAvailable(ClientService.class);
        ClientService clientService = getOsgiService(ClientService.class);

        // use the client service to manipulate the booking service
        Booking booking = new Booking("Karaf Itest", "IT001");
        clientService.addBooking(booking);
        List<Booking> bookings = clientService.bookings();
        boolean found = false;
        for (Booking b : bookings) {
            if (b.getCustomer().equals("Karaf Itest") && b.getFlight().equals("IT001")) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);
    }

}
