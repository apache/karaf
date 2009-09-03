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
package org.apache.felix.fileinstall.internal;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import junit.framework.TestCase;
import org.apache.felix.fileinstall.internal.Util;

public class UtilTest extends TestCase
{
    public void testBasicSubstitution()
    {
        System.setProperty("value1", "sub_value1");
        Dictionary props = new Hashtable();
        props.put("key0", "value0");
        props.put("key1", "${value1}");
        props.put("key2", "${value2}");

        for (Enumeration e = props.keys(); e.hasMoreElements();)
        {
            String name = (String) e.nextElement();
            props.put(name,
                Util.substVars((String) props.get(name), name, null, props));
        }

        assertEquals("value0", props.get("key0"));
        assertEquals("sub_value1", props.get("key1"));
        assertEquals("", props.get("key2"));

    }

    public void testSubstitutionFailures()
    {
        assertEquals("a}", Util.substVars("a}", "b", null, new Hashtable()));
        assertEquals("${a", Util.substVars("${a", "b", null, new Hashtable()));
    }

}