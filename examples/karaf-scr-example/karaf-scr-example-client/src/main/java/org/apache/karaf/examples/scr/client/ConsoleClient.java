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
package org.apache.karaf.examples.scr.client;

import org.apache.karaf.examples.scr.api.Booking;
import org.apache.karaf.examples.scr.api.BookingService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component
public class ConsoleClient {

    private boolean running;

    @Reference
    private BookingService bookingService;

    @Activate
    public void start() throws Exception {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setFlight("AF520");
        booking.setCustomer("John Doe");
        bookingService.add(booking);
        booking = new Booking();
        booking.setId(2L);
        booking.setFlight("AF59");
        booking.setCustomer("Alan Parker");
        bookingService.add(booking);

        running = true;
        Thread thread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(5000);
                    for (Booking booking1 : bookingService.list()) {
                        System.out.println();
                        System.out.println("-----------");
                        System.out.println(booking1.getId() + " - " + booking1.getFlight() + " - " + booking1.getCustomer());
                    }
                } catch (Exception e) {
                    // nothing to do
                }
            }
        });
        thread.start();
    }

    @Deactivate
    public void deactivate() throws Exception {
        running = false;
    }

}
