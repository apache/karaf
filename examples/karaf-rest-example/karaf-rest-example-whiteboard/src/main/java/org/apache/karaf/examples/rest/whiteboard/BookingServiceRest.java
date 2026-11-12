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
package org.apache.karaf.examples.rest.whiteboard;

import jakarta.ws.rs.core.MediaType;
import org.apache.karaf.examples.rest.api.Booking;
import org.apache.karaf.examples.rest.api.BookingService;
import org.osgi.service.component.annotations.Component;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.annotations.RequireJakartarsWhiteboard;
import org.osgi.service.jakartars.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RequireHttpWhiteboard
@RequireJakartarsWhiteboard
@JSONRequired
@JakartarsResource
@Component(service = BookingServiceRest.class, scope = ServiceScope.PROTOTYPE)
public class BookingServiceRest implements BookingService {

    private final Map<Long, Booking> bookings = new HashMap<>();

    @Override
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Collection<Booking> list() {
        return bookings.values();
    }

    @Override
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Booking get(@PathParam("id") Long id) {
        return bookings.get(id);
    }

    @Override
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    public void add(Booking booking) {
        bookings.put(booking.getId(), booking);
    }

    @Override
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @PUT
    public void update(Booking booking) {
        bookings.remove(booking.getId());
        bookings.put(booking.getId(), booking);
    }

    @Override
    @Path("/{id}")
    @DELETE
    public void remove(@PathParam("id") Long id) {
        bookings.remove(id);
    }
}
