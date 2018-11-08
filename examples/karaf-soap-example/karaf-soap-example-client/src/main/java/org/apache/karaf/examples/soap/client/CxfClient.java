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
package org.apache.karaf.examples.soap.client;

import java.util.Collection;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.karaf.examples.soap.api.Booking;
import org.apache.karaf.examples.soap.provider.BookingServiceSoap;

public class CxfClient {

    private BookingServiceSoap bookingService;

    public CxfClient(String url) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress(url);
        factory.setServiceClass(BookingServiceSoap.class);
        bookingService = (BookingServiceSoap) factory.create();
    }

    public void add(Booking booking) {
        bookingService.add(booking);
    }

    public Collection<Booking> list() {
        return bookingService.list();
    }
}
