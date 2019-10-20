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

import org.apache.karaf.examples.jdbc.api.BookingService;
import org.apache.karaf.itests.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JdbcExampleTest extends BaseTest {

    @Test
    public void test() throws Exception {
        // adding jdbc example features repository
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-jdbc-example-features/" + System.getProperty("karaf.version") + "/xml");

        // install the karaf-jdbc-example-provider feature
        installAndAssertFeature("karaf-jdbc-example-provider");
        assertServiceAvailable(BookingService.class);

        // install the karaf-jdbc-example feature
        installAndAssertFeature("karaf-jdbc-example");

        // add booking
        executeCommand("booking:add Foo AF520");
        // list booking
        String bookings = executeCommand("booking:list");
        System.out.println(bookings);
        assertContains("AF520", bookings);
        // get booking
        String booking = executeCommand("booking:get 1");
        System.out.println(booking);
        assertContains("AF520", booking);
        // remove booking
        executeCommand("booking:remove 1");
        bookings = executeCommand("booking:list");
        System.out.println(bookings);
        assertContainsNot("AF520", bookings);
    }

}
