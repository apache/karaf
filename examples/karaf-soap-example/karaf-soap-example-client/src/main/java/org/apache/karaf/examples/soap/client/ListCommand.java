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
package org.apache.karaf.examples.soap.client;

import org.apache.karaf.examples.soap.api.Booking;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Service
@Command(scope = "booking", name = "list", description = "List bookings")
public class ListCommand implements Action {

    @Option(name = "--url", description = "Location of the SOAP service", required = false, multiValued = false)
    String url = "http://localhost:8181/cxf/example";

    @Override
    public Object execute() throws Exception {
        CxfClient client = new CxfClient(url);
        ShellTable table = new ShellTable();
        table.column("ID");
        table.column("Customer");
        table.column("Flight");
        if (client.list() != null) {
            for (Booking booking : client.list()) {
                table.addRow().addContent(booking.getId(), booking.getCustomer(), booking.getFlight());
            }
        }
        table.print(System.out);
        return null;
    }

}
