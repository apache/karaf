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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

public class SystemPropertiesTest extends TestCase
{

    SystemPropertiesCommandImpl sysprop = new SystemPropertiesCommandImpl();

    public void testCanDisplayASingleProperty()
    {
        System.setProperty("first", "foo");
        assertEquals("foo", System.getProperty("first"));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);

        sysprop.execute("sysprop first", ps, System.err);
        assertTrue(bos.toString().startsWith("first=foo"));
    }

    public void testCanCreateProperty()
    {
        assertNull(System.getProperty("foo"));
        sysprop.execute("sysprop foo bar", System.out, System.err);
        assertEquals("bar", System.getProperty("foo"));
    }

    public void testCanChangePropertyValue()
    {
        System.setProperty("foo", "bar");
        assertEquals("bar", System.getProperty("foo"));

        sysprop.execute("sysprop foo barbar", System.out, System.err);

        assertEquals("barbar", System.getProperty("foo"));
    }

    public void testCanRemoveProperty()
    {
        System.setProperty("foo", "bar");
        assertEquals("bar", System.getProperty("foo"));
        sysprop.execute("sysprop -r foo", System.out, System.err);

        assertNull(System.getProperty("foo"));
    }
}
