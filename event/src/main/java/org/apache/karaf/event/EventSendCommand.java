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
package org.apache.karaf.event;

import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@Command(scope = "event", name = "send", description = "Send a simple event to a topic")
@Service
public class EventSendCommand implements Action {
    @Reference
    Session session;
    
    @Reference
    EventAdmin eventAdmin;

    @Argument
    String topic;

    @Argument(multiValued=true)
    String propertiesSt;

    @Override
    public Object execute() throws Exception {
        eventAdmin.sendEvent(new Event(topic, parse(propertiesSt)));
        return null;
    }

    Map<String, String> parse(String propSt) {
        Map<String, String> properties = new HashMap<>();
        for (String keyValue : propSt.split(",")) {
            String[] splitted = keyValue.split("=");
            if (splitted.length != 2) {
                throw new IllegalArgumentException("Invalid entry " + keyValue);
            }
            properties.put(splitted[0], splitted[1]);
        };
        return properties;
    }

}
