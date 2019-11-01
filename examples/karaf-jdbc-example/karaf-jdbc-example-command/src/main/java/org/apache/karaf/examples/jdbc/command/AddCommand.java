/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.examples.jdbc.command;

import org.apache.karaf.examples.jdbc.api.Booking;
import org.apache.karaf.examples.jdbc.api.BookingService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.Random;

@Service
@Command(scope = "booking", name = "add", description = "Add a booking")
public class AddCommand implements Action {

    @Reference
    private BookingService bookingService;

    @Option(name = "-i", aliases = "--id", description = "Booking ID", required = false, multiValued = false)
    Long id = 0L;

    @Argument(index = 0, name = "customer", description = "Booking customer", required = true, multiValued = false)
    String customer;

    @Argument(index = 1, name = "flight", description = "Booking flight", required = true, multiValued = false)
    String flight;

    @Override
    public Object execute() throws Exception {
        Booking booking = new Booking();
        if (id == 0) {
            Random random = new Random();
            id = Long.valueOf(random.nextInt(9000000) + 1000000);
        }
        booking.setId(id);
        booking.setCustomer(customer);
        booking.setFlight(flight);
        bookingService.add(booking);
        return null;
    }

}
