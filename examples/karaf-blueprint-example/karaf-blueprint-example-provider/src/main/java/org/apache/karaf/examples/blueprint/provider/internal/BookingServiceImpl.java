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
package org.apache.karaf.examples.blueprint.provider.internal;

import java.util.*;
import org.apache.karaf.examples.blueprint.common.Booking;
import org.apache.karaf.examples.blueprint.common.BookingService;

/** Very simple implementation of a booking service. */
public class BookingServiceImpl implements BookingService {

    private Map<Long, Booking> bookings = new HashMap<>();

    @Override
    public List<Booking> list() {
        return new LinkedList<>(bookings.values());
    }

    @Override
    public Booking get(Long id) {
        return bookings.get(id);
    }

    @Override
    public void add(Booking booking) {
        bookings.put(booking.getId(), booking);
    }
}
