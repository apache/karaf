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
package org.apache.karaf.examples.jpa.command;

import org.apache.karaf.examples.jpa.Booking;
import org.apache.karaf.examples.jpa.BookingService;
import org.apache.karaf.examples.jpa.completers.BookingIdCompleter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Service
@Command(scope = "booking", name = "get", description = "Get the booking by id")
public class GetCommand implements Action {

    @Reference
    private BookingService bookingService;

    @Argument(index = 0, name = "id", description = "Id of booking to retreive", required = true, multiValued = false)
    @Completion(BookingIdCompleter.class)
    Long id;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("ID");
        table.column("Flight");
        table.column("Customer");
        Booking booking = bookingService.get(id);
        table.addRow().addContent(booking.getId(), booking.getFlight(), booking.getCustomer());
        table.print(System.out);
        return null;
    }

}
