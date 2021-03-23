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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.karaf.examples.rest.api.Booking;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "booking", name = "list-all", description = "List booking with attachments")
public class ListAllBookingCommand implements Action {

    @Option(name = "--url", description = "Location of the REST service", required = false, multiValued = false)
    String restLocation = "http://localhost:8181/cxf/booking/all";

    @Override
    public Object execute() throws Exception {
        List providers = new ArrayList();
        providers.add(new JacksonJsonProvider());
        WebClient webClient = WebClient.create(restLocation, providers);

        List<Attachment> atts = new LinkedList<>();
        Booking b1 = new Booking();
        b1.setCustomer("the one who must not be named");
        b1.setFlight("101");
        b1.setId(43L);
        atts.add(new Attachment("root", "application/json", b1));
        atts.add(new Attachment("image", "application/octet-stream", new ByteArrayInputStream("UGhvdG8=".getBytes(StandardCharsets.UTF_8))));

        Collection<? extends Attachment> results = webClient.type("multipart/mixed")
                .accept("multipart/mixed")
                .postAndGetCollection(atts, Attachment.class);

        for (Attachment a : results) {
            System.out.println(a.getContentId() + ": " + a.getContentType() + ": " + a.getDataHandler().getContent());
        }

        return null;
    }

}
