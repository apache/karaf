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
package org.apache.karaf.shell.support.ansi;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

public class AnsiSplitter {

    public static List<String> splitLines(String text, int maxLength, int tabs) throws IOException {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.tabs(tabs);
        sb.appendAnsi(text);
        return sb.columnSplitLength(maxLength)
                .stream()
                .map(AttributedString::toAnsi)
                .collect(Collectors.toList());
    }

    public static String substring(String text, int begin, int end, int tabs) throws IOException {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.tabs(tabs);
        sb.appendAnsi(text);
        return sb.columnSubSequence(begin, end).toAnsi();
    }

    public static int length(String text, int tabs) throws IOException {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.tabs(tabs);
        sb.appendAnsi(text);
        return sb.columnLength();
    }

    public static String cut(String text, int maxLength, int tabs)  throws IOException {
        return splitLines(text, maxLength, tabs).get(0);
    }

    public static AnsiBufferedReader window(InputStream is, int begin, int end, int tabs) throws IOException {
        AnsiBufferedReader reader = new AnsiBufferedReader(is, begin, end, Integer.MAX_VALUE);
        reader.setTabs(tabs);
        return reader;
    }

    public static AnsiBufferedReader splitter(InputStream is, int maxLength, int tabs) throws IOException {
        AnsiBufferedReader reader = new AnsiBufferedReader(is, 0, Integer.MAX_VALUE, maxLength);
        reader.setTabs(tabs);
        return reader;
    }


    public static class AnsiBufferedReader implements Closeable {

        private final BufferedReader reader;
        private final int begin;
        private final int end;
        private final int maxLength;
        private final AttributedStringBuilder builder;
        private final List<String> lines;

        public AnsiBufferedReader(InputStream in, int begin, int end, int maxLength) {
            this.reader = new BufferedReader(new InputStreamReader(in));
            this.begin = begin;
            this.end = end;
            this.maxLength = maxLength;
            this.builder = new AttributedStringBuilder();
            this.lines = new ArrayList<>();
        }

        public String readLine() throws IOException {
            if (lines.isEmpty()) {
                String line = reader.readLine();
                if (line == null) {
                    return null;
                }
                if (line.isEmpty()) {
                    lines.add("");
                } else {
                    builder.setLength(0);
                    builder.appendAnsi(line);
                    if (builder.length() > 0) {
                        builder.style(builder.styleAt(builder.length() - 1));
                    }
                    AttributedString str = builder.columnSubSequence(begin, end);
                    str.columnSplitLength(maxLength)
                            .stream()
                            .map(AttributedString::toAnsi)
                            .forEach(lines::add);
                }
            }
            return lines.remove(0);
        }

        @Override
        public void close() throws IOException {
        }

        public void setTabs(int tabs) {
            this.builder.tabs(tabs);
        }
    }

}
