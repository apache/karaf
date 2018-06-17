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
package org.apache.karaf.examples.scr.provider;

import org.apache.karaf.examples.scr.api.Booking;
import org.apache.karaf.examples.scr.api.BookingService;
import org.osgi.service.component.annotations.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation of the {@link BookingService} storing the {@link Booking}
 * instances in a list in memory.
 */
@Component
public class BookingServiceMemoryImpl implements BookingService {

    private final Map<Long, Booking> bookings = new HashMap<>();

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
