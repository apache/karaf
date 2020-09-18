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

import org.apache.karaf.examples.bundle.common.BookingService;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {

    private ServiceTracker<BookingService, BookingService> bookingServiceTracker;
    private ServiceRegistration clientServiceRegistration;
    private Display display;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        bookingServiceTracker = new ServiceTracker<BookingService, BookingService>(bundleContext, BookingService.class, null) {

            @Override
            public BookingService addingService(ServiceReference<BookingService> reference) {
                BookingService bookingService = bundleContext.getService(reference);

                ClientServiceImpl clientService = new ClientServiceImpl();
                clientService.setBookingService(bookingService);
                clientServiceRegistration = bundleContext.registerService(ClientService.class, clientService, null);

                display = new Display();
                display.setClientService(clientService);
                display.init();

                return bookingService;
            }

            @Override
            public void removedService(ServiceReference<BookingService> reference, BookingService service) {
                display.destroy();

                clientServiceRegistration.unregister();
            }
        };

        bookingServiceTracker.open();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        bookingServiceTracker.close();
    }
}
