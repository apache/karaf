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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiOutputStream;

public class AnsiSplitter {

    public static List<String> splitLines(String text, int maxLength, int tabs) throws IOException {
        AnsiOutputStreamSplitter splitter = new AnsiOutputStreamSplitter(maxLength);
        splitter.setTabs(tabs);
        splitter.write(text.getBytes());
        splitter.close();
        return splitter.lines;
    }

    public static String substring(String text, int begin, int end, int tabs) throws IOException {
        AnsiOutputStreamSplitter splitter = new AnsiOutputStreamSplitter(begin, end, Integer.MAX_VALUE);
        splitter.setTabs(tabs);
        splitter.write(text.getBytes());
        splitter.close();
        return splitter.lines.get(0);
    }

    public static int length(String curLine, int tabs) throws IOException {
        AnsiOutputStreamSplitter splitter = new AnsiOutputStreamSplitter(0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        splitter.setTabs(tabs);
        splitter.write(curLine.getBytes());
        return splitter.getRealLength();
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

        private final InputStream in;
        private final AnsiOutputStreamSplitter splitter;

        public AnsiBufferedReader(InputStream in, int begin, int end, int maxLength) {
            this.in = in;
            this.splitter = new AnsiOutputStreamSplitter(begin, end, maxLength);
        }

        public String readLine() throws IOException {
            while (splitter.lines.isEmpty()) {
                int c = in.read();
                if (c < 0) {
                    splitter.flushLine(false);
                    break;
                } else {
                    splitter.write(c);
                }
            }
            if (splitter.lines.isEmpty()) {
                return null;
            } else {
                return splitter.lines.remove(0);
            }
        }

        @Override
        public void close() throws IOException {
        }

        public void setTabs(int tabs) {
            this.splitter.setTabs(tabs);
        }
    }

    static class AnsiOutputStreamSplitter extends AnsiOutputStream {

        protected static final int ATTRIBUTE_NEGATIVE_OFF = 27;

        Ansi.Attribute intensity;
        Ansi.Attribute underline;
        Ansi.Attribute blink;
        Ansi.Attribute negative;
        Ansi.Color fg;
        Ansi.Color bg;

        private int begin;
        private int length;
        private int maxLength;
        private int escapeLength;
        private int windowState;
        private int tabs;
        private List<String> lines = new ArrayList<>();

        public AnsiOutputStreamSplitter(int maxLength) {
            this(0, Integer.MAX_VALUE, maxLength);
        }

        public AnsiOutputStreamSplitter(int begin, int end, int maxLength) {
            super(new ByteArrayOutputStream());
            this.begin = begin;
            this.length = end - begin;
            this.maxLength = maxLength - begin;
            this.windowState = begin > 0 ? 0 : 1;
            reset();
        }

        public int getTabs() {
            return tabs;
        }

        public void setTabs(int tabs) {
            this.tabs = tabs;
        }

        protected void reset() {
            intensity = Ansi.Attribute.INTENSITY_BOLD_OFF;
            underline = Ansi.Attribute.UNDERLINE_OFF;
            blink = Ansi.Attribute.BLINK_OFF;
            negative = Ansi.Attribute.NEGATIVE_OFF;
            fg = Ansi.Color.DEFAULT;
            bg = Ansi.Color.DEFAULT;
        }

        public int getRealLength() {
            return ((ByteArrayOutputStream) out).size() - escapeLength;
        }

        @Override
        public void write(int data) throws IOException {
            if (data == '\n') {
                flushLine(true);
            } else if (data == '\t') {
                ByteArrayOutputStream baos = (ByteArrayOutputStream) out;
                do {
                    write(' ');
                } while ((baos.size() - escapeLength) % tabs > 0);
            } else {
                if (windowState != 2) {
                    super.write(data);
                }
                ByteArrayOutputStream baos = (ByteArrayOutputStream) out;
                if (windowState == 0 && baos.size() - escapeLength > begin) {
                    windowState = 1;
                    int nbMissing = baos.size() - escapeLength - begin;
                    byte[] old = baos.toByteArray();
                    beginAttributes();
                    baos.write(old, old.length - nbMissing, nbMissing);
                } else if (windowState == 1 && baos.size() - escapeLength >= length) {
                    windowState = 2;
                    endAttributes();
                    reset();
                }
                if (baos.size() - escapeLength >= maxLength) {
                    flushLine(true);
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (windowState == 0) {
                beginAttributes();
            }
            flushLine(lines.isEmpty());
            super.close();
        }

        private void flushLine(boolean force) throws IOException {
            ByteArrayOutputStream baos = (ByteArrayOutputStream) out;
            if (windowState == 0) {
                beginAttributes();
            }
            if (force || baos.size() > escapeLength) {
                endAttributes();
                lines.add(new String(baos.toByteArray()));
                beginAttributes();
            }
            windowState = 0;
        }

        private void endAttributes() throws IOException {
            if (intensity != Ansi.Attribute.INTENSITY_BOLD_OFF) {
                setAttribute(Ansi.Attribute.INTENSITY_BOLD_OFF);
            }
            if (underline != Ansi.Attribute.UNDERLINE_OFF) {
                setAttribute(Ansi.Attribute.UNDERLINE_OFF);
            }
            if (blink != Ansi.Attribute.BLINK_OFF) {
                setAttribute(Ansi.Attribute.BLINK_OFF);
            }
            if (negative != Ansi.Attribute.NEGATIVE_OFF) {
                setAttribute(Ansi.Attribute.NEGATIVE_OFF);
            }
            if (fg != Ansi.Color.DEFAULT) {
                setAttributeFg(Ansi.Color.DEFAULT);
            }
            if (bg != Ansi.Color.DEFAULT) {
                setAttributeBg(Ansi.Color.DEFAULT);
            }
        }

        private void beginAttributes() throws IOException {
            ((ByteArrayOutputStream) out).reset();
            escapeLength = 0;
            if (intensity != Ansi.Attribute.INTENSITY_BOLD_OFF) {
                setAttribute(intensity);
            }
            if (underline != Ansi.Attribute.UNDERLINE_OFF) {
                setAttribute(underline);
            }
            if (blink != Ansi.Attribute.BLINK_OFF) {
                setAttribute(blink);
            }
            if (negative != Ansi.Attribute.NEGATIVE_OFF) {
                setAttribute(negative);
            }
            if (fg != Ansi.Color.DEFAULT) {
                setAttributeFg(fg);
            }
            if (bg != Ansi.Color.DEFAULT) {
                setAttributeBg(bg);
            }
        }

        @Override
        protected void processAttributeRest() throws IOException {
            setAttribute(Ansi.Attribute.RESET);
            reset();
        }

        @Override
        protected void processSetAttribute(int attribute) throws IOException {
            switch(attribute) {
            case ATTRIBUTE_INTENSITY_BOLD:
                setIntensity(Ansi.Attribute.INTENSITY_BOLD);
                break;
            case ATTRIBUTE_INTENSITY_FAINT:
                setIntensity(Ansi.Attribute.INTENSITY_FAINT);
                break;
            case ATTRIBUTE_INTENSITY_NORMAL:
                setIntensity(Ansi.Attribute.INTENSITY_BOLD_OFF);
                break;
            case ATTRIBUTE_UNDERLINE:
                setUnderline(Ansi.Attribute.UNDERLINE);
                break;
            case ATTRIBUTE_UNDERLINE_DOUBLE:
                setUnderline(Ansi.Attribute.UNDERLINE_DOUBLE);
                break;
            case ATTRIBUTE_UNDERLINE_OFF:
                setUnderline(Ansi.Attribute.UNDERLINE_OFF);
                break;
            case ATTRIBUTE_BLINK_OFF:
                setBlink(Ansi.Attribute.BLINK_OFF);
                break;
            case ATTRIBUTE_BLINK_SLOW:
                setBlink(Ansi.Attribute.BLINK_SLOW);
                break;
            case ATTRIBUTE_BLINK_FAST:
                setBlink(Ansi.Attribute.BLINK_FAST);
                break;
            case ATTRIBUTE_NEGATIVE_ON:
                setNegative(Ansi.Attribute.NEGATIVE_ON);
                break;
            case ATTRIBUTE_NEGATIVE_OFF:
                setNegative(Ansi.Attribute.NEGATIVE_OFF);
                break;
            default:
                break;
            }
        }

        @Override
        protected void processSetForegroundColor(int color) throws IOException {
            Ansi.Color c;
            switch (color) {
            case 0: c = Ansi.Color.BLACK; break;
            case 1: c = Ansi.Color.RED; break;
            case 2: c = Ansi.Color.GREEN; break;
            case 3: c = Ansi.Color.YELLOW; break;
            case 4: c = Ansi.Color.BLUE; break;
            case 5: c = Ansi.Color.MAGENTA; break;
            case 6: c = Ansi.Color.CYAN; break;
            case 7: c = Ansi.Color.WHITE; break;
            case 9: c = Ansi.Color.DEFAULT; break;
            default: return;
            }
            if (this.fg != c) {
                this.fg = c;
                setAttributeFg(c);
            }
        }

        @Override
        protected void processSetBackgroundColor(int color) throws IOException {
            Ansi.Color c;
            switch (color) {
            case 0: c = Ansi.Color.BLACK; break;
            case 1: c = Ansi.Color.RED; break;
            case 2: c = Ansi.Color.GREEN; break;
            case 3: c = Ansi.Color.YELLOW; break;
            case 4: c = Ansi.Color.BLUE; break;
            case 5: c = Ansi.Color.MAGENTA; break;
            case 6: c = Ansi.Color.CYAN; break;
            case 7: c = Ansi.Color.WHITE; break;
            case 9: c = Ansi.Color.DEFAULT; break;
            default: return;
            }
            if (this.bg != c) {
                this.bg = c;
                setAttributeBg(c);
            }
        }

        @Override
        protected void processDefaultTextColor() throws IOException {
            processSetForegroundColor(9);
        }

        @Override
        protected void processDefaultBackgroundColor() throws IOException {
            processSetBackgroundColor(9);
        }

        protected void setIntensity(Ansi.Attribute intensity) throws IOException {
            if (this.intensity != intensity) {
                this.intensity = intensity;
                setAttribute(intensity);
            }
        }

        protected void setUnderline(Ansi.Attribute underline) throws IOException {
            if (this.underline != underline) {
                this.underline = underline;
                setAttribute(underline);
            }
        }

        protected void setBlink(Ansi.Attribute blink) throws IOException {
            if (this.blink != blink) {
                this.blink = blink;
                setAttribute(blink);
            }
        }

        protected void setNegative(Ansi.Attribute negative) throws IOException {
            if (this.negative != negative) {
                this.negative = negative;
                setAttribute(negative);
            }
        }

        private void setAttributeFg(Ansi.Color color) throws IOException {
            String sequence = Ansi.ansi().fg(color).toString();
            escapeLength += sequence.length();
            out.write(sequence.getBytes());
        }

        private void setAttributeBg(Ansi.Color color) throws IOException {
            String sequence = Ansi.ansi().bg(color).toString();
            escapeLength += sequence.length();
            out.write(sequence.getBytes());
        }

        private void setAttribute(Ansi.Attribute attribute) throws IOException {
            String sequence = Ansi.ansi().a(attribute).toString();
            escapeLength += sequence.length();
            out.write(sequence.getBytes());
        }

    }
}
