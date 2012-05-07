/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.commands.impl;

import junit.framework.TestCase;

import org.apache.karaf.shell.commands.impl.InfoAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfoTest extends TestCase {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(TestCase.class);

    public void testUptime() throws Exception {
        InfoAction infoAction = new InfoAction();
        // uptime 30 seconds
        assertEquals("30.000 seconds", infoAction.printDuration(30 * 1000));
        // uptime 2 minutes
        assertEquals("2 minutes", infoAction.printDuration(2 * 60 * 1000));
        // uptime 2 hours
        assertEquals("2 hours", infoAction.printDuration(2 * 3600 * 1000));
        // update 2 days and 2 hours
        assertEquals("2 days 2 hours", infoAction.printDuration(50 * 3600 * 1000));
    }

}
