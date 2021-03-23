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
package org.apache.karaf.examples.rest.scr;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.karaf.examples.rest.api.Booking;
import org.apache.karaf.examples.rest.api.BookingService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
public class BookingServiceRest implements BookingService {
    
    private final Map<Long, Booking> bookings = new HashMap<>();

    @Override
    @Path("/")
    @Produces("application/json")
    @GET
    public Collection<Booking> list() {
        return bookings.values();
    }

    @Path("/all")
    @Consumes("multipart/mixed")
    @Produces("multipart/mixed")
    @POST
    public MultipartBody traceMultipart(List<Attachment> atts) {
        final List<Attachment> results = new ArrayList<>();
        final Attachment att1 = new Attachment("text", MediaType.TEXT_HTML, "Hello World!");
        results.add(att1);
        Booking b1 = new Booking();
        b1.setCustomer("me");
        b1.setFlight("far far away");
        b1.setId(42L);
        final Attachment att2 = new Attachment("json", MediaType.APPLICATION_JSON, b1);
        results.add(att2);
        final Attachment att3 = new Attachment("stream", MediaType.APPLICATION_OCTET_STREAM, new ByteArrayInputStream("important information".getBytes()));
        results.add(att3);
        results.addAll(atts);
        return new MultipartBody(results, true);
    }

    @Override
    @Path("/{id}")
    @Produces("application/json")
    @GET
    public Booking get(@PathParam("id") Long id) {
        return bookings.get(id);
    }
    
    @Override
    @Path("/")
    @Consumes("application/json")
    @POST
    public void add(Booking booking) {
        bookings.put(booking.getId(), booking);
    }

    @Override
    @Path("/")
    @Consumes("application/json")
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
