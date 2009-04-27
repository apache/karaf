/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.shell.impl;

import junit.framework.TestCase;

public class LogOptionsTest extends TestCase
{
    public void testOnlyLogCommand()
    {
        LogOptions opt = new LogOptions("log");
        assertEquals(4, opt.getMinLevel());
        assertEquals(-1, opt.getMaxNumberOfLogs());

        opt = new LogOptions("log  ");
        assertEquals(4, opt.getMinLevel());
        assertEquals(-1, opt.getMaxNumberOfLogs());
    }

    public void testLogWithMinLevelDebug()
    {
        LogOptions opt = new LogOptions("log debug");
        assertEquals(4, opt.getMinLevel());
        assertEquals(-1, opt.getMaxNumberOfLogs());

        opt = new LogOptions("log DEBUG");
        assertEquals(4, opt.getMinLevel());
        assertEquals(-1, opt.getMaxNumberOfLogs());
    }

    public void testLogWithMinLevelInfo()
    {
        LogOptions opt = new LogOptions("log info");
        assertEquals(3, opt.getMinLevel());
        assertEquals(-1, opt.getMaxNumberOfLogs());

        opt = new LogOptions("log INFO");
        assertEquals(3, opt.getMinLevel());
        assertEquals(-1, opt.getMaxNumberOfLogs());
    }

    public void testLogWithMinLevelWarn()
    {
        LogOptions opt = new LogOptions("log warn");
        assertEquals(2, opt.getMinLevel());
        assertEquals(-1, opt.getMaxNumberOfLogs());

        opt = new LogOptions("log WARN");
        assertEquals(2, opt.getMinLevel());
        assertEquals(-1, opt.getMaxNumberOfLogs());
    }

    public void testLogWithMinLevelError()
    {
        LogOptions opt = new LogOptions("log error");
        assertEquals(1, opt.getMinLevel());
        assertEquals(-1, opt.getMaxNumberOfLogs());

        opt = new LogOptions("log ERROR");
        assertEquals(1, opt.getMinLevel());
        assertEquals(-1, opt.getMaxNumberOfLogs());
    }

    public void testLogWithMaxNumberOfLogs()
    {
        LogOptions opt = new LogOptions("log 42");
        assertEquals(4, opt.getMinLevel());
        assertEquals(42, opt.getMaxNumberOfLogs());
    }

    public void testLogWithMinLevelDebugAndMaxNumberOfLogs()
    {
        LogOptions opt = new LogOptions("log debug 12");
        assertEquals(4, opt.getMinLevel());
        assertEquals(12, opt.getMaxNumberOfLogs());

        opt = new LogOptions("log 13 DEBUG");
        assertEquals(4, opt.getMinLevel());
        assertEquals(13, opt.getMaxNumberOfLogs());
    }

    public void testLogWithMinLevelInfoAndMaxNumberOfLogs()
    {
        LogOptions opt = new LogOptions("log info 14");
        assertEquals(3, opt.getMinLevel());
        assertEquals(14, opt.getMaxNumberOfLogs());

        opt = new LogOptions("log 15 INFO");
        assertEquals(3, opt.getMinLevel());
        assertEquals(15, opt.getMaxNumberOfLogs());
    }

    public void testLogWithMinLevelWarnAndMaxNumberOfLogs()
    {
        LogOptions opt = new LogOptions("log warn 16");
        assertEquals(2, opt.getMinLevel());
        assertEquals(16, opt.getMaxNumberOfLogs());

        opt = new LogOptions("log 17 WARN");
        assertEquals(2, opt.getMinLevel());
        assertEquals(17, opt.getMaxNumberOfLogs());
    }

    public void testLogWithMinLevelErrorAndMaxNumberOfLogs()
    {
        LogOptions opt = new LogOptions("log error 18");
        assertEquals(1, opt.getMinLevel());
        assertEquals(18, opt.getMaxNumberOfLogs());

        opt = new LogOptions("log 19 ERROR");
        assertEquals(1, opt.getMinLevel());
        assertEquals(19, opt.getMaxNumberOfLogs());
    }
}