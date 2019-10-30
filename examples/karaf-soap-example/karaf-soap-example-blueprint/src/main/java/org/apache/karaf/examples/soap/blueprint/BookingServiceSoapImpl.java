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
package org.apache.karaf.examples.soap.blueprint;

import java.util.*;

import org.apache.karaf.examples.soap.api.Booking;

import javax.jws.WebService;

@WebService(endpointInterface="org.apache.karaf.examples.soap.blueprint.BookingServiceSoap", serviceName="Booking")
public class BookingServiceSoapImpl implements BookingServiceSoap {

    private Map<Long, Booking> bookings = new HashMap<>();

    @Override
    public Collection<Booking> list() {
        return bookings.values();
    }

    @Override
    public Booking get(Long id) {
        return bookings.get(id);
    }

    @Override
    public void add(Booking booking) {
        bookings.put(booking.getId(), booking);
    }

    @Override
    public void remove(Long id) {
        bookings.remove(id);
    }
}
