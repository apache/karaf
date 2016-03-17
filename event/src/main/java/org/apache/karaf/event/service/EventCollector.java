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
package org.apache.karaf.event.service;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class EventCollector implements EventHandler {
    private Deque<Event> events;
    private int maxSize;
    private Set<Consumer<Event>> consumers;
    
    public EventCollector() {
        events = new ConcurrentLinkedDeque<>();
        maxSize = 100;
        consumers = new HashSet<>();
    }

    @Override
    public synchronized void handleEvent(Event event) {
        events.addLast(event);
        if (events.size() > maxSize) {
            events.removeFirst();
        }
        consumers.forEach(c -> c.accept(event));
    }

    public Stream<Event> getEvents() {
        return events.stream();
    }
    
    public synchronized void addConsumer(Consumer<Event> eventConsumer) {
        events.forEach(eventConsumer);
        consumers.add(eventConsumer);
    }
    
    public synchronized void removeConsumer(Consumer<Event> eventConsumer) {
        consumers.remove(eventConsumer);
    }

}
