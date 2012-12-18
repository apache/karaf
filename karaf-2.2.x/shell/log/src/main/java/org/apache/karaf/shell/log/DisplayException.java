/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.log;

import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Command;

@Command(scope = "log", name = "display-exception", description = "Displays the last occurred exception from the log.")
public class DisplayException extends OsgiCommandSupport {

    protected LruList events;

    public LruList getEvents() {
        return events;
    }

    public void setEvents(LruList events) {
        this.events = events;
    }

    protected Object doExecute() throws Exception {
        PaxLoggingEvent throwableEvent = null;
        Iterable<PaxLoggingEvent> le = events.getElements(Integer.MAX_VALUE);
        for (PaxLoggingEvent event : le) {
            if (event.getThrowableStrRep() != null) {
                throwableEvent = event;
                // Do not break, as we iterate from the oldest to the newest event
            }
        }
        if (throwableEvent != null) {
            for (String r : throwableEvent.getThrowableStrRep()) {
                System.out.println(r);
            }
            System.out.println();
        }
        return null;
    }

}
