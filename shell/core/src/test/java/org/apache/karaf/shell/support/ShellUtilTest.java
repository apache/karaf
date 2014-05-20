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
package org.apache.karaf.shell.support;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShellUtilTest {

    @Test
    public void testGetValueString() {
        Object data;
        data = "Hello World";
        assertEquals("Hello World", ShellUtil.getValueString(data));

        data = new int[]{1, 2, 3, 5, 7, 9};
        assertEquals("[1, 2, 3, 5, 7, 9]", ShellUtil.getValueString(data));

        data = new short[]{1, 2, 3, 5, 7, 9};
        assertEquals("[1, 2, 3, 5, 7, 9]", ShellUtil.getValueString(data));

        data = new long[]{1, 2, 3, 5, 7, 9};
        assertEquals("[1, 2, 3, 5, 7, 9]", ShellUtil.getValueString(data));

        data = new byte[]{1, 2, 3, 5, 7, 9};
        assertEquals("[1, 2, 3, 5, 7, 9]", ShellUtil.getValueString(data));

        data = new float[]{1, 2, 3, 5, 7, 9};
        assertEquals("[1.0, 2.0, 3.0, 5.0, 7.0, 9.0]", ShellUtil.getValueString(data));

        data = new double[]{1, 2, 3, 5, 7, 9};
        assertEquals("[1.0, 2.0, 3.0, 5.0, 7.0, 9.0]", ShellUtil.getValueString(data));

        data = new boolean[]{true, true, false};
        assertEquals("[true, true, false]", ShellUtil.getValueString(data));

        data = new char[]{'a', 'c', 'e'};
        assertEquals("[a, c, e]", ShellUtil.getValueString(data));

        data = new Object[]{
                new int[]{1, 2, 3, 5, 8},
                new char[]{'h', 'e', 'l', 'l', 'o'},
                "World"
        };
        assertEquals("[[1, 2, 3, 5, 8], [h, e, l, l, o], World]", ShellUtil.getValueString(data));

        data = Arrays.asList(5, 10, 15, 25);
        assertEquals("[5, 10, 15, 25]", ShellUtil.getValueString(data));

        data = new LinkedHashSet<>(Arrays.asList(5, 10, 15, 25));
        assertEquals("[5, 10, 15, 25]", ShellUtil.getValueString(data));

        data = new int[][]{{1, 2, 3}, {5, 7, 9}};
        assertEquals("[[1, 2, 3], [5, 7, 9]]", ShellUtil.getValueString(data));

    }

}
