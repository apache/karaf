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

import static org.apache.karaf.event.service.TopicPredicate.matchTopic;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.function.Predicate;

import org.junit.Test;
import org.osgi.service.event.Event;

public class TopicPredicateTest {

    @Test
    public void testMatchAll() {
        Predicate<Event> matcher = matchTopic("*");
        assertTrue(matcher.test(event("myTopic")));
        assertTrue(matcher.test(event("my/other")));
    }
     
    @Test
    public void testMatchSpecific() {
        Predicate<Event> matcher = matchTopic("myTopic");
        assertTrue(matcher.test(event("myTopic")));
        assertFalse(matcher.test(event("myTopic/test")));
        assertFalse(matcher.test(event("my/other")));
    }
    
    @Test
    public void testMatchSubTopics() {
        Predicate<Event> matcher = matchTopic("myTopic*");
        assertTrue(matcher.test(event("myTopic")));
        assertTrue(matcher.test(event("myTopic/test")));
        assertFalse(matcher.test(event("my/other")));
    }

    private Event event(String topic) {
        return new Event(topic, new HashMap<String, String>());
    }
}
