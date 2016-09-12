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

import org.apache.karaf.shell.impl.action.command.DefaultActionPreparator;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class GrepTest {

    private static final String ANSI_COLOR = "\u001b[1;31m";
    private static final String ANSI_RESET = "\u001b[0m";

    @Test
    public void testGrep() throws Exception {
        final String expectedColoredString = "1\n" + ANSI_COLOR + "2"
            + ANSI_RESET + "\n"
            + "3\n4\n5\n6\n7\n8\n9";

        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("--color", "always", "-C", "100", "2"));
        final String returnedString = systemInOutDecorator("1\n2\n3\n4\n5\n6\n7\n8\n9\n", grep);
        assertEquals(expectedColoredString, returnedString);
    }

    @Test
    public void testGrepInverted() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-v", "--color", "never", "mine"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("This is Hello World\nHello World!", returnedString);
    }

    @Test
    public void testGrepMatching() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("--color", "never", "mine"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("World is mine", returnedString);
    }

    @Test
    public void testGrepMatchingWithColours() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("--color", "always", "mine"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("World is " + ANSI_COLOR + "mine" + ANSI_RESET, returnedString);
    }

    @Test
    public void testGrepCount() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-c", "Hello World"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("2", returnedString);
    }

    @Test
    public void testGrepCountInvert() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-c", "-v", "Hello World"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("1", returnedString);
    }

    @Test
    public void testGrepInvertedWithLineNumbers() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-n", "-v", "--color", "never", "mine"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("     1  This is Hello World\n     3  Hello World!", returnedString);
    }

    @Test
    public void testGrepMatchingWithLineNumbers() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-n", "--color", "never", "Hello"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("     1  This is Hello World\n     3  Hello World!", returnedString);
    }

    @Test
    public void testGrepWordRegExp() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-w", "--color", "never", "is"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("This is Hello World\nWorld is mine", returnedString);
    }

    @Test
    public void testGrepIs() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("--color", "never", "is"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("This is Hello World\nWorld is mine", returnedString);
    }

    @Test
    public void testGrepRegExpWithColour() throws Exception {
        GrepAction grep = new GrepAction();
        final String expected = "Th"
            + ANSI_COLOR
            + "is" + ANSI_RESET
            + " "
            + ANSI_COLOR
            + "is" + ANSI_RESET
            + " Hello World\nWorld "
            + ANSI_COLOR
            + "is" + ANSI_RESET
            + " mine";
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("--color", "always", "is"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals(expected, returnedString);
    }

    @Test
    public void testGrepWordRegExpWithColour() throws Exception {
        GrepAction grep = new GrepAction();
        final String expected = "This "
            + ANSI_COLOR
            + "is" + ANSI_RESET
            + " Hello World\nWorld "
            + ANSI_COLOR
            + "is" + ANSI_RESET
            + " mine";
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-w", "--color", "always", "is"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals(expected, returnedString);
    }

    @Test
    public void testGrepLineRegExpWithColour() throws Exception {
        GrepAction grep = new GrepAction();
        final String expected = ANSI_COLOR
            + "This is Hello World" + ANSI_RESET;
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-x", "--color", "always", ".*Hello World"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals(expected, returnedString);
    }

    @Test
    public void testGrepTwoLinesRegExpWithColour() throws Exception {
        GrepAction grep = new GrepAction();
        final String expected = ANSI_COLOR
            + "This is Hello World" + ANSI_RESET + "\n"
            + ANSI_COLOR
            + "Hello World!" + ANSI_RESET;
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-x", "--color", "always", ".*Hello World.*"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals(expected, returnedString);
    }

    @Test
    public void testGrepIgnoreCaseWithColour() throws Exception {
        GrepAction grep = new GrepAction();
        final String expected = "This is "
            + ANSI_COLOR
            + "hello" + ANSI_RESET + " World\n"
            + ANSI_COLOR
            + "Hello" + ANSI_RESET + " World!";
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-i", "--color", "always", "HELLO"));
        final String returnedString = systemInOutDecorator("This is hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals(expected, returnedString);
    }

    @Test
    public void testGrepContextOneWithColour() throws Exception {
        GrepAction grep = new GrepAction();
        final String expected = "This is "
            + ANSI_COLOR
            + "Hello" + ANSI_RESET + " World\n"
            + "World is mine\n"
            + ANSI_COLOR
            + "Hello" + ANSI_RESET + " World!";
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-C", "1", "--color", "always", "Hello"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals(expected, returnedString);
    }

    @Test
    public void testGrepBeforeContextOneWithColour() throws Exception {
        GrepAction grep = new GrepAction();
        final String expected = "World is mine\n"
            + ANSI_COLOR
            + "Hello World!" + ANSI_RESET;
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-B", "1", "--color", "always", "Hello World!"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals(expected, returnedString);
    }

    @Test
    public void testGrepAfterContextOneWithColour() throws Exception {
        GrepAction grep = new GrepAction();
        final String expected = "World is "
            + ANSI_COLOR
            + "mine" + ANSI_RESET
            + "\nHello World!";
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-A", "1", "--color", "always", "mine"));
        final String returnedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals(expected, returnedString);
    }

    @Test
    public void testGrepOnlyMatching() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-o", "--color", "never", "He.*rld"));
        final String expectedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("Hello World\nHello World", expectedString);
    }

    @Test
    public void testGrepOnlyMatchingGroup() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("-o", "--color", "never", "(This).*(Hello)"));
        final String expectedString = systemInOutDecorator("This is Hello World\nWorld is mine\nHello World!\n",
            grep);
        assertEquals("This is Hello", expectedString);
    }

    @Test
    public void testHonorColorNever() throws Exception {
        GrepAction grep = new GrepAction();
        DefaultActionPreparator preparator = new DefaultActionPreparator();
        preparator.prepare(grep, null, Arrays.asList("--color", "never", "b"));
        final String expectedString = systemInOutDecorator("abc\n",
            grep);
        assertEquals("abc", expectedString);

    }

    private String systemInOutDecorator(String inputString, GrepAction grepExecute) throws Exception {
        InputStream input = System.in;
        PrintStream output = System.out;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(inputString.getBytes());
            System.setIn(bais);

            String result = ((List<Object>) grepExecute.execute()).stream()
                    .map(Object::toString).collect(Collectors.joining("\n"));
            if (result.length() > 1 && result.charAt(result.length() - 1) == '\n') {
                result = result.substring(0, result.length() - 1);
            }
            return result;
        } finally {
            System.setIn(input);
            System.setOut(output);
        }
    }

}
