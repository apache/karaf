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
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

@Service
@Command(scope = "booking", name = "add", description = "Add booking")
public class AddBookingCommand implements Action {

    @Argument(index = 0, name = "id", description = "Booking ID", required = true, multiValued = false)
    long id;

    @Argument(index = 1, name = "customer", description = "Customer name", required = true, multiValued = false)
    String customer;

    @Argument(index = 2, name = "flight", description = "Flight number", required = true, multiValued = false)
    String flight;

    @Option(name = "--url", description = "Location of the REST service", required = false, multiValued = false)
    String restLocation = "http://localhost:8181/cxf/booking/";

    @Override
    public Object execute() throws Exception {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setFlight(flight);
        booking.setCustomer(customer);

        Client client = ClientBuilder.newClient();
        client.register(new JacksonJsonProvider());
        WebTarget target = client.target(restLocation);
        target.request().post(Entity.json(booking));

        return null;
    }

}
