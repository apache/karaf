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
package org.apache.karaf.examples.redis.service;

import com.google.gson.GsonBuilder;
import org.apache.karaf.examples.redis.api.Booking;
import org.apache.karaf.examples.redis.api.BookingService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

@Component(service = BookingService.class)
public class BookingServiceImpl implements BookingService {

    private Jedis jedis;
    private String bookingListName;

    @Activate
    public void activate(RedisConfig config) {
        jedis = new Jedis(config.host());
        bookingListName = config.bookingListName();
    }

    @Deactivate
    public void deactivate() {
        jedis.close();
    }

    @Override
    public List<Booking> list() {
        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < jedis.llen(bookingListName); i++) {
            Booking booking = new GsonBuilder().create().fromJson(jedis.lindex(bookingListName, i), Booking.class);
            bookings.add(booking);
        }
        return bookings;
    }

    @Override
    public void add(Booking booking) {
        jedis.lpush(bookingListName, new GsonBuilder().create().toJson(booking, Booking.class));
    }

}
