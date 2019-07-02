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
package org.apache.karaf.examples.bundle.client;

import org.apache.karaf.examples.bundle.common.Booking;

/**
 * Simple class getting the booking service (thanks to Blueprint), adding a random booking and displaying periodically.
 */
public class Display {

    private ClientService clientService;

    private BookingDisplayThread thread;
    private boolean bookingThreadStarted = false;

    /**
     * This setter is used by Blueprint to inject the client service.
     */
    public void setClientService(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Init method used to start the thread.
     */
    public void init() {
        thread = new BookingDisplayThread(clientService);
        thread.start();
    }

    /**
     * Destroy method used to stop the thread.
     */
    public void destroy() {
        thread.terminate();
    }

    /**
     * Very simple thread adding random booking and displaying the bookings on System.out every 5s.
     */
    private class BookingDisplayThread extends Thread {

        private ClientService clientService;
        private volatile boolean running = true;

        public BookingDisplayThread(ClientService clientService) {
            this.clientService = clientService;
        }

        @Override
        public void run() {
            while (running) {
                try {

                    // TODO test
                    Booking booking = new Booking("John Doo", "AF3030");
                    clientService.addBooking(booking);

                    System.out.println(displayBookings());
                    Thread.sleep(5000);
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }

        private String displayBookings() {
            StringBuilder builder = new StringBuilder();
            for (Booking booking : clientService.bookings()) {
                builder.append(booking.getId()).append(" | ").append(booking.getCustomer()).append(" | ").append(booking.getFlight()).append("\n");
            }
            return builder.toString();
        }

        public void terminate() {
            running = false;
        }

    }

}
