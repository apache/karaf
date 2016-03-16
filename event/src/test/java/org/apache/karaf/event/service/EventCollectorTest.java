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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.Test;
import org.osgi.service.event.Event;

public class EventCollectorTest {

    @Test
    public void testHandleEvent() throws Exception {
        EventCollector collector = new EventCollector();
        assertThat(collector.getEvents().count(), equalTo(0l));
        collector.handleEvent(event("myTopic"));
        assertThat(collector.getEvents().count(), equalTo(1l));
        assertThat(collector.getEvents().findFirst().get().getTopic(), equalTo("myTopic"));
        
    }
    
    @Test
    public void testLimit() {
        EventCollector collector = new EventCollector();
        collector.handleEvent(event("first"));
        IntStream.rangeClosed(1, 99).forEach(c -> collector.handleEvent(event("myTopic")));
        assertThat(collector.getEvents().count(), equalTo(100l));
        collector.handleEvent(event("last"));
        assertThat(collector.getEvents().count(), equalTo(100l));
        assertTrue(collector.getEvents().noneMatch(event -> event.getTopic().endsWith("first")));
        assertTrue(collector.getEvents().anyMatch(event -> event.getTopic().endsWith("last")));
    }

    @Test
    public void testAddRemoveConsumer() {
        final AtomicInteger count = new AtomicInteger();
        Consumer<Event> countingConsumer = event -> count.incrementAndGet();
        EventCollector collector = new EventCollector();
        collector.handleEvent(event("myTopic"));
        collector.addConsumer(countingConsumer);
        assertThat(count.get(), equalTo(1));

        collector.handleEvent(event("another"));
        assertThat(count.get(), equalTo(2));

        collector.removeConsumer(countingConsumer);
        collector.handleEvent(event("and/another"));
        assertThat(count.get(), equalTo(2));
    }

    private Event event(String topic) {
        return new Event(topic, new HashMap<>());
    }

}
