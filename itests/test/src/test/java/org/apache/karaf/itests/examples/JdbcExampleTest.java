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

        // use a unique flight code so the assertions are not disturbed by bookings possibly
        // left over by a previous (retried) run: the example stores bookings in an AUTO_INCREMENT
        // table that is shared across runs of the same Karaf instance
        String flight = "AF" + System.currentTimeMillis();

        // add booking
        executeCommand("booking:add Foo " + flight);
        // list booking
        String bookings = executeCommand("booking:list");
        System.out.println(bookings);
        assertContains(flight, bookings);

        // resolve the actual (auto-incremented) id instead of assuming it is 1
        long id = bookingId(bookings, flight);

        // get booking
        String booking = executeCommand("booking:get " + id);
        System.out.println(booking);
        assertContains(flight, booking);
        // remove booking
        executeCommand("booking:remove " + id);
        bookings = executeCommand("booking:list");
        System.out.println(bookings);
        assertContainsNot(flight, bookings);
    }

    /**
     * Extracts the booking id (first column) of the row matching the given flight code from the
     * {@code booking:list} shell table output.
     */
    private long bookingId(String listOutput, String flight) {
        // the booking:list output is a ShellTable whose columns are separated by the Unicode
        // box-drawing vertical bar (U+2502), not an ASCII '|', so extract the leading id digits
        // directly rather than splitting on a column separator
        java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile("^\\s*(\\d+)");
        for (String line : listOutput.split("\\r?\\n")) {
            if (line.contains(flight)) {
                java.util.regex.Matcher matcher = idPattern.matcher(line);
                if (matcher.find()) {
                    return Long.parseLong(matcher.group(1));
                }
            }
        }
        throw new IllegalStateException("No booking found for flight " + flight + " in:\n" + listOutput);
    }

}
