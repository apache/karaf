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
package org.apache.felix.framework.util;

import java.util.Properties;
import junit.framework.TestCase;

public class UtilTest extends TestCase
{
    public void testVariableSubstitution()
    {
        Properties props = new Properties();
        props.setProperty("one", "${two}");
        props.setProperty("two", "2");
        String v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("2", v);

        props.clear();
        props.setProperty("one", "${two}${three}");
        props.setProperty("two", "2");
        props.setProperty("three", "3");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("23", v);

        props.clear();
        props.setProperty("one", "${two${three}}");
        props.setProperty("two3", "2");
        props.setProperty("three", "3");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("2", v);

        props.clear();
        props.setProperty("one", "${two${three}}");
        props.setProperty("two3", "2");
        System.setProperty("three", "3");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        System.getProperties().remove("three");
        assertEquals("2", v);

        props.clear();
        props.setProperty("one", "${two}");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("", v);

        props.clear();
        props.setProperty("one", "{two");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("{two", v);

        props.clear();
        props.setProperty("one", "{two}");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("{two}", v);

        props.clear();
        props.setProperty("one", "${two");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("${two", v);

        props.clear();
        props.setProperty("one", "${two${two}");
        props.setProperty("two", "2");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("${two2", v);

        props.clear();
        props.setProperty("one", "{two${two}}");
        props.setProperty("two", "2");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("{two2}", v);

        props.clear();
        props.setProperty("one", "{two}${two");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("{two}${two", v);

        props.clear();
        props.setProperty("one", "leading text ${two}");
        props.setProperty("two", "2");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("leading text 2", v);

        props.clear();
        props.setProperty("one", "${two} trailing text");
        props.setProperty("two", "2");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("2 trailing text", v);

        props.clear();
        props.setProperty("one", "${two} middle text ${three}");
        props.setProperty("two", "2");
        props.setProperty("three", "3");
        v = Util.substVars(props.getProperty("one"), "one", null, props);
        assertEquals("2 middle text 3", v);
    }
}