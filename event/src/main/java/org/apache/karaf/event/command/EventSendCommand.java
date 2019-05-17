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
package org.apache.karaf.event.command;

import java.util.HashMap;
import java.util.List;
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

    @Argument(index=0, required=true)
    String topic;

    @Argument(index=1, multiValued=true, description="Event properties in format key=value key2=value2 ...")
    List<String> properties;

    @Override
    public Object execute() throws Exception {
        eventAdmin.sendEvent(new Event(topic, parse(properties)));
        return null;
    }

    static Map<String, String> parse(List<String> propList) {
        Map<String, String> properties = new HashMap<>();
        if (propList != null) {
            for (String keyValue : propList) {
                int splitAt = keyValue.indexOf("=");
                if (splitAt <= 0) {
                    throw new IllegalArgumentException("Invalid property " + keyValue);
                } else {
                    String key = keyValue.substring(0, splitAt);
                    String value = keyValue.substring(splitAt + 1);
                    properties.put(key, value);
                }
            }
        }
        return properties;
    }

}
