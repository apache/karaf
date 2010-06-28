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
package org.apache.felix.karaf.shell.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import junit.framework.TestCase;

public class SortTest extends TestCase {

    public void testFieldIndexesDefaultSep() {
        SortAction.SortComparator comparator = new SortAction.SortComparator(false, false, false, false, '\0', null);
        List<Integer> indexes = comparator.getFieldIndexes(" ad  re  t ");
        assertTrue(Arrays.asList(0, 2, 3, 6, 7, 9, 10, 10).equals(indexes));
    }

    public void testFieldIndexesWithSep() {
        SortAction.SortComparator comparator = new SortAction.SortComparator(false, false, false, false, '[', null);
        List<Integer> indexes = comparator.getFieldIndexes("[  10] [Active     ] [       ] [    8] OPS4J Pax Logging - Service (1.3.0)");
        assertTrue(Arrays.asList(1, 6, 8, 20, 22, 30, 32, 73 ).equals(indexes));

        indexes = comparator.getFieldIndexes(" ad  re  t ");
        assertTrue(Arrays.asList(0, 10).equals(indexes));
    }

    public void testSort() {
        String s0 = "0321   abcd  ddcba   a";
        String s1 = " 57t   bcad  ddacb   b";
        String s2 = "  128  cab   ddbac   c";
        List<String> strings = Arrays.asList(s0, s1, s2);

        Collections.sort(strings, new SortAction.SortComparator(false, false, false, false, '\0', Arrays.asList("2")));
        assertTrue(Arrays.asList(s0, s1, s2).equals(strings));

        Collections.sort(strings, new SortAction.SortComparator(false, false, false, false, '\0', Arrays.asList("2.2b")));
        assertTrue(Arrays.asList(s2, s0, s1).equals(strings));

        Collections.sort(strings, new SortAction.SortComparator(false, false, false, true, '\0', null));
        assertTrue(Arrays.asList(s1, s2, s0).equals(strings));
    }

}
