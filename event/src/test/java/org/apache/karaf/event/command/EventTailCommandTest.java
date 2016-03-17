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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.event.command.EventTailCommand;
import org.apache.karaf.event.service.EventCollector;
import org.apache.karaf.shell.api.console.Session;
import org.junit.Test;
import org.osgi.service.event.Event;

public class EventTailCommandTest {

    private Exception exception;

    @Test
    public void testTail() throws Exception {
        EventTailCommand tail = new EventTailCommand();
        tail.session = mock(Session.class);
        tail.collector = new EventCollector();
        PrintStream out = System.out;
        expect(tail.session.getConsole()).andReturn(out);
        exception = null;
        replay(tail.session);
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                tail.execute();
            } catch (Exception e) {
                exception = e;
            }
        });
        tail.collector.handleEvent(event());
        Thread.sleep(200);
        executor.shutdownNow(); // Will interrupt the tail
        executor.awaitTermination(10, TimeUnit.SECONDS);
        if (exception != null) {
            throw exception;
        }
        verify(tail.session);
    }

    private Event event() {
        return new Event("myTopic", new HashMap<>());
    }
}
