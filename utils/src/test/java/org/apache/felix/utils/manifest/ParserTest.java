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
package org.apache.felix.utils.manifest;

import junit.framework.TestCase;

public class ParserTest extends TestCase
{

    public void testSimple() throws Exception {
        Clause[] paths = Parser.parseHeader("/foo.xml, /foo/bar.xml");
        assertEquals(2, paths.length);
        assertEquals("/foo.xml", paths[0].getName());
        assertEquals(0, paths[0].getAttributes().length);
        assertEquals(0, paths[0].getDirectives().length);
        assertEquals("/foo/bar.xml", paths[1].getName());
        assertEquals(0, paths[1].getAttributes().length);
        assertEquals(0, paths[1].getDirectives().length);
    }

    public void testComplex() throws Exception {
        Clause[] paths = Parser.parseHeader("OSGI-INF/blueprint/comp1_named.xml;ignored-directive:=true,OSGI-INF/blueprint/comp2_named.xml;some-other-attribute=1");
        assertEquals(2, paths.length);
        assertEquals("OSGI-INF/blueprint/comp1_named.xml", paths[0].getName());
        assertEquals(0, paths[0].getAttributes().length);
        assertEquals(1, paths[0].getDirectives().length);
        assertEquals("true", paths[0].getDirective("ignored-directive"));
        assertEquals("OSGI-INF/blueprint/comp2_named.xml", paths[1].getName());
        assertEquals(1, paths[1].getAttributes().length);
        assertEquals("1", paths[1].getAttribute("some-other-attribute"));
        assertEquals(0, paths[1].getDirectives().length);
    }

    public void testPaths() throws Exception {
        Clause[] paths = Parser.parseHeader("OSGI-INF/blueprint/comp1_named.xml;ignored-directive:=true,OSGI-INF/blueprint/comp2_named.xml;foo.xml;a=b;1:=2;c:=d;4=5");
        assertEquals(3, paths.length);
        assertEquals("OSGI-INF/blueprint/comp1_named.xml", paths[0].getName());
        assertEquals(0, paths[0].getAttributes().length);
        assertEquals(1, paths[0].getDirectives().length);
        assertEquals("true", paths[0].getDirective("ignored-directive"));
        assertEquals("OSGI-INF/blueprint/comp2_named.xml", paths[1].getName());
        assertEquals(2, paths[1].getAttributes().length);
        assertEquals("b", paths[1].getAttribute("a"));
        assertEquals("5", paths[1].getAttribute("4"));
        assertEquals(2, paths[1].getDirectives().length);
        assertEquals("d", paths[1].getDirective("c"));
        assertEquals("2", paths[1].getDirective("1"));
        assertEquals("foo.xml", paths[2].getName());
        assertEquals(2, paths[2].getAttributes().length);
        assertEquals("b", paths[2].getAttribute("a"));
        assertEquals("5", paths[2].getAttribute("4"));
        assertEquals(2, paths[2].getDirectives().length);
        assertEquals("d", paths[2].getDirective("c"));
        assertEquals("2", paths[2].getDirective("1"));
    }

}
