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
package org.apache.karaf.shell.commands;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class WcTest extends TestCase {

    private static final String SMALL_FILE = "WcActionTestSmall.txt";
    private static final String LARGE_FILE = "WcActionTestLarge.txt";

    public void testEmptyStringWordCounts() throws Exception {
        String string = "";
        WcAction.WordCounts expected = new WcAction.WordCounts();
        WcAction.WordCounts actual = getWordCounts(string);
        assertEquals(expected, actual);
    }

    public void testHelloWorldWordCounts() throws Exception {
        String string = "Hello World";
        WcAction.WordCounts expected = new WcAction.WordCounts();
        expected.wordCount = 2;
        expected.lineCount = 0;
        expected.charCount = 11;
        expected.byteCount = 11;
        WcAction.WordCounts actual = getWordCounts(string);
        assertEquals(expected, actual);
    }

    public void testLargeStringWordCounts() throws Exception {
        InputStream stream = WcTest.class.getResourceAsStream(LARGE_FILE);

        WcAction.WordCounts expected = new WcAction.WordCounts();
        expected.wordCount = 487;
        expected.lineCount = 34;
        expected.charCount = 3307;
        expected.byteCount = 3307;
        WcAction.WordCounts actual = getWordCounts(stream);
        assertEquals(expected, actual);
    }

    public void testOneFileWordCounts() throws Exception {
        File file = new File(WcTest.class.getResource(LARGE_FILE).toURI());
        List<File> files = Arrays.asList(file);

        WcAction wcAction = new WcAction();
        wcAction.setDefaultOptions();

        String result = wcAction.getFilesWordCount(files);

        assertEquals("A single file report should not contain a total count row", "\t34\t487\t3307\tWcActionTestLarge.txt\n", result);
    }

    public void testThreeFilesWordCounts() throws Exception {
        File smallFile = new File(WcTest.class.getResource(SMALL_FILE).toURI());
        File largeFile = new File(WcTest.class.getResource(LARGE_FILE).toURI());

        List<File> files = Arrays.asList(smallFile, largeFile, smallFile);

        WcAction wcAction = new WcAction();
        wcAction.setDefaultOptions();

        String result = wcAction.getFilesWordCount(files);

        assertEquals("A single file report should not contain a total",
                                "\t11\t12\t34\tWcActionTestSmall.txt\n" +
                                        "\t34\t487\t3307\tWcActionTestLarge.txt\n" +
                                        "\t11\t12\t34\tWcActionTestSmall.txt\n" +
                                        "\t56\t511\t3375\ttotal"
                                        , result);
    }

    public WcAction.WordCounts getWordCounts(String string) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(string.getBytes());
        return getWordCounts(inputStream);
    }

    public WcAction.WordCounts getWordCounts(InputStream inputStream) throws Exception {
        WcAction wcAction = new WcAction();
        return wcAction.getWordCounts(inputStream);
    }

    public void assertEquals(WcAction.WordCounts expected, WcAction.WordCounts actual) {
        assertEquals("The expected wordCount should be equal", expected.wordCount, actual.wordCount);
        assertEquals("The expected lineCount should be equal", expected.lineCount, actual.lineCount);
        assertEquals("The expected charCount should be equal", expected.charCount, actual.charCount);
        assertEquals("The expected byteCount should be equal", expected.byteCount, actual.byteCount);
    }

}
