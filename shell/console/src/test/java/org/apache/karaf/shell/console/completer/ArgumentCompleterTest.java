/**
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
package org.apache.karaf.shell.console.completer;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArgumentCompleterTest {

    @Test
    public void testParser1() throws Exception {
        Parser parser = new Parser("echo foo | cat bar ; ta", 23);
        List<List<List<CharSequence>>> p = parser.program();
        assertEquals(1, parser.c0);
        assertEquals(0, parser.c1);
        assertEquals(0, parser.c2);
        assertEquals(2, parser.c3);
    }

    @Test
    public void testParser2() throws Exception {
        Parser parser = new Parser("echo foo ; cat bar | ta", 23);
        List<List<List<CharSequence>>> p = parser.program();
        assertEquals(1, parser.c0);
        assertEquals(1, parser.c1);
        assertEquals(0, parser.c2);
        assertEquals(2, parser.c3);
    }

    @Test
    public void testParser3() throws Exception {
        Parser parser = new Parser("echo foo ; cat bar | ta", 22);
        List<List<List<CharSequence>>> p = parser.program();
        assertEquals(1, parser.c0);
        assertEquals(1, parser.c1);
        assertEquals(0, parser.c2);
        assertEquals(1, parser.c3);
    }

    @Test
    public void testParser4() throws Exception {
        Parser parser = new Parser("echo foo ; cat bar | ta reta", 27);
        List<List<List<CharSequence>>> p = parser.program();
        assertEquals(1, parser.c0);
        assertEquals(1, parser.c1);
        assertEquals(1, parser.c2);
        assertEquals(3, parser.c3);
    }

    @Test
    public void testParser5() throws Exception {
        Parser parser = new Parser("echo foo ; cat bar | ta reta", 24);
        List<List<List<CharSequence>>> p = parser.program();
        assertEquals(1, parser.c0);
        assertEquals(1, parser.c1);
        assertEquals(1, parser.c2);
        assertEquals(0, parser.c3);
    }

}
