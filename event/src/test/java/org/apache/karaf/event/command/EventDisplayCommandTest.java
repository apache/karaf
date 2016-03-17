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

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;

import java.util.HashMap;

import org.apache.karaf.event.command.EventDisplayCommand;
import org.apache.karaf.event.service.EventCollector;
import org.apache.karaf.shell.api.console.Session;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.osgi.service.event.Event;

public class EventDisplayCommandTest {

    @Test
    public void testExecute() throws Exception {
        IMocksControl c = createControl();
        EventDisplayCommand display = new EventDisplayCommand();
        display.session = c.createMock(Session.class);
        expect(display.session.getConsole()).andReturn(System.out);
        display.collector = new EventCollector();
        display.collector.handleEvent(new Event("myTopic", new HashMap<>()));
        c.replay();
        display.execute();
        c.verify();
    }
}
