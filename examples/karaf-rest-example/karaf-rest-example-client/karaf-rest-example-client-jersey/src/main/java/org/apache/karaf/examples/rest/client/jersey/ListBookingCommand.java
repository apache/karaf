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
package org.apache.karaf.examples.rest.client.jersey;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.karaf.examples.rest.api.Booking;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import java.util.List;

@Service
@Command(scope = "booking", name = "list", description = "List booking")
public class ListBookingCommand implements Action {

    @Option(name = "--url", description = "Location of the REST service", required = false, multiValued = false)
    String restLocation = "http://localhost:8181/cxf/booking/";

    @Override
    public Object execute() throws Exception {
        Client client = ClientBuilder.newClient();
        client.register(new JacksonJsonProvider());
        WebTarget target = client.target(restLocation);

        List<Booking> bookings = (List<Booking>) target.request(MediaType.APPLICATION_JSON).get(new GenericType<List<Booking>>() {});
        for (Booking booking : bookings) {
            System.out.println(booking.getId() + " " + booking.getCustomer() + " " + booking.getFlight());
        }

        return null;
    }

}