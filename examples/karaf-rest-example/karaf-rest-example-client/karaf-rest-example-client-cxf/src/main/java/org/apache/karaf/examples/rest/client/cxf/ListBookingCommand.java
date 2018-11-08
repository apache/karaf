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
package org.apache.karaf.examples.rest.client.cxf;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.karaf.examples.rest.api.Booking;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "booking", name = "list", description = "List booking")
public class ListBookingCommand implements Action {

    @Option(
            name = "--url",
            description = "Location of the REST service",
            required = false,
            multiValued = false)
    String restLocation = "http://localhost:8181/cxf/booking/";

    @Override
    public Object execute() throws Exception {
        List providers = new ArrayList();
        providers.add(new JacksonJsonProvider());
        WebClient webClient = WebClient.create(restLocation, providers);

        List<Booking> bookings =
                (List<Booking>)
                        webClient.accept(MediaType.APPLICATION_JSON).getCollection(Booking.class);
        for (Booking booking : bookings) {
            System.out.println(
                    booking.getId() + " " + booking.getCustomer() + " " + booking.getFlight());
        }

        return null;
    }
}
