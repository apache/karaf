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

package org.apache.geronimo.gshell.commands.text;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.vfs.FileObject;
import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.io.Closer;
import org.apache.geronimo.gshell.vfs.FileObjects;
import org.apache.geronimo.gshell.vfs.support.VfsActionSupport;

/**
 * Sort lines of text
 *
 * TODO: remove this file when gshell is upgraded
 *
 */
public class SortAction extends VfsActionSupport {

    @Option(name = "-f")
    private boolean caseInsensitive;

    @Option(name = "-r")
    private boolean reverse;

    @Option(name = "-u")
    private boolean unique;

    @Option(name = "-t")
    private String separator;

    @Option(name = "-b")
    private boolean ignoreBlanks;

    @Option(name = "-k", argumentRequired = true, multiValued = true)
    private List<String> sortFields;

    @Option(name = "-n")
    private boolean numeric;

    @Argument(index = 0, required=false)
    private String path;


    public Object execute(CommandContext context) throws Exception {
        assert context != null;

        if (path != null) {
            FileObject file = resolveFile(context, path);

            try {
                sort(context, file);
            }
            finally {
                FileObjects.close(file);
            }
        }
        else {
            sort(context.getIo().inputStream, context.getIo().out);
        }
        return Result.SUCCESS;
    }

    protected void sort(final CommandContext context, final FileObject file) throws Exception {
        assert context != null;
        assert file != null;

        ensureFileExists(file);
        ensureFileHasContent(file);
        ensureFileIsReadable(file);

        BufferedInputStream input = new BufferedInputStream(file.getContent().getInputStream());
        try {
            sort(input, context.getIo().out);
        }
        finally {
            Closer.close(input);
        }
    }

    protected void sort(InputStream input, PrintWriter out) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(input));
        List<String> strings = new ArrayList<String>();
        for (String s = r.readLine(); s != null; s = r.readLine()) {
            strings.add(s);
        }
        char sep = (separator == null || separator.length() == 0) ? '\0' : separator.charAt(0);
        Collections.sort(strings, new SortComparator(caseInsensitive, reverse, ignoreBlanks, numeric, sep, sortFields));
        String last = null;
        for (String s : strings) {
            if (last == null) {
                last = s;
            } else if (!unique || !s.equals(last)) {
                out.println(s);
            }
        }
    }

    public static class SortComparator implements Comparator<String> {

        private boolean caseInsensitive;
        private boolean reverse;
        private boolean ignoreBlanks;
        private boolean numeric;
        private char separator;
        private List<Key> sortKeys;

        private static Pattern fpPattern;
        static {
            final String Digits     = "(\\p{Digit}+)";
            final String HexDigits  = "(\\p{XDigit}+)";
            final String Exp        = "[eE][+-]?" + Digits;
            final String fpRegex    = "([\\x00-\\x20]*[+-]?(NaN|Infinity|(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|(\\.(" + Digits + ")(" + Exp + ")?)|(((0[xX]" + HexDigits + "(\\.)?)|(0[xX]" + HexDigits + "?(\\.)" + HexDigits + "))[pP][+-]?" + Digits + "))" + "[fFdD]?))[\\x00-\\x20]*)(.*)";
            fpPattern = Pattern.compile(fpRegex);
        }

        public SortComparator(boolean caseInsensitive,
                              boolean reverse,
                              boolean ignoreBlanks,
                              boolean numeric,
                              char separator,
                              List<String> sortFields) {
            this.caseInsensitive = caseInsensitive;
            this.reverse = reverse;
            this.separator = separator;
            this.ignoreBlanks = ignoreBlanks;
            this.numeric = numeric;
            if (sortFields == null || sortFields.size() == 0) {
                sortFields = new ArrayList<String>();
                sortFields.add("1");
            }
            sortKeys = new ArrayList<Key>();
            for (String f : sortFields) {
                sortKeys.add(new Key(f));
            }
        }

        public int compare(String o1, String o2) {
            int res = 0;

            List<Integer> fi1 = getFieldIndexes(o1);
            List<Integer> fi2 = getFieldIndexes(o2);
            for (Key key : sortKeys) {
                int[] k1 = getSortKey(o1, fi1, key);
                int[] k2 = getSortKey(o2, fi2, key);
                if (key.numeric) {
                    Double d1 = getDouble(o1, k1[0], k1[1]);
                    Double d2 = getDouble(o2, k2[0], k2[1]);
                    res = d1.compareTo(d2);
                } else {
                    res = compareRegion(o1, k1[0], k1[1], o2, k2[0], k2[1], key.caseInsensitive);
                }
                if (res != 0) {
                    if (key.reverse) {
                        res = - res;
                    }
                    break;
                }
            }
            return res;
        }

        protected Double getDouble(String s, int start, int end) {
            Matcher m = fpPattern.matcher(s.substring(start, end));
            m.find();
            return new Double(s.substring(0, m.end(1)));
        }

        protected int compareRegion(String s1, int start1, int end1, String s2, int start2, int end2, boolean caseInsensitive) {
            int n1 = end1, n2 = end2;
            for (int i1 = start1, i2 = start2; i1 < end1 && i2 < n2; i1++, i2++) {
                char c1 = s1.charAt(i1);
                char c2 = s2.charAt(i2);
                if (c1 != c2) {
                    if (caseInsensitive) {
                        c1 = Character.toUpperCase(c1);
                        c2 = Character.toUpperCase(c2);
                        if (c1 != c2) {
                            c1 = Character.toLowerCase(c1);
                            c2 = Character.toLowerCase(c2);
                            if (c1 != c2) {
                                return c1 - c2;
                            }
                        }
                    } else {
                        return c1 - c2;
                    }
                }
            }
            return n1 - n2;
        }

        protected int[] getSortKey(String str, List<Integer> fields, Key key) {
            int start;
            int end;
            if (key.startField * 2 < fields.size()) {
                start = fields.get((key.startField - 1) * 2);
                if (key.ignoreBlanksStart) {
                    while (start < fields.get((key.startField - 1) * 2 + 1) && Character.isWhitespace(str.charAt(start))) {
                        start++;
                    }
                }
                if (key.startChar > 0) {
                    start = Math.min(start + key.startChar - 1, fields.get((key.startField - 1) * 2 + 1));
                }
            } else {
                start = 0;
            }
            if (key.endField > 0 && key.endField * 2 < fields.size()) {
                end =  fields.get((key.endField - 1) * 2);
                if (key.ignoreBlanksEnd) {
                    while (end < fields.get((key.endField - 1) * 2 + 1) && Character.isWhitespace(str.charAt(end))) {
                        end++;
                    }
                }
                if (key.endChar > 0) {
                    end = Math.min(end + key.endChar - 1, fields.get((key.endField - 1) * 2 + 1));
                }
            } else {
                end = str.length();
            }
            return new int[] { start, end };
        }

        protected List<Integer> getFieldIndexes(String o) {
            List<Integer> fields = new ArrayList<Integer>();
            if (o.length() > 0) {
                if (separator == '\0') {
                    int i = 0;
                    fields.add(0);
                    for (int idx = 1; idx < o.length(); idx++) {
                        if (Character.isWhitespace(o.charAt(idx)) && !Character.isWhitespace(o.charAt(idx - 1))) {
                            fields.add(idx - 1);
                            fields.add(idx);
                        }
                    }
                    fields.add(o.length() - 1);
                } else {
                    int last = -1;
                    for (int idx = o.indexOf(separator); idx >= 0; idx = o.indexOf(separator, idx + 1)) {
                        if (last >= 0) {
                            fields.add(last);
                            fields.add(idx - 1);
                        } else if (idx > 0) {
                            fields.add(0);
                            fields.add(idx - 1);
                        }
                        last = idx + 1;
                    }
                    if (last < o.length()) {
                        fields.add(last < 0 ? 0 : last);
                        fields.add(o.length() - 1);
                    }
                }
            }
            return fields;
        }

        public class Key {
            int startField;
            int startChar;
            int endField;
            int endChar;
            boolean ignoreBlanksStart;
            boolean ignoreBlanksEnd;
            boolean caseInsensitive;
            boolean reverse;
            boolean numeric;

            public Key(String str) {
                boolean modifiers = false;
                boolean startPart = true;
                boolean inField = true;
                boolean inChar = false;
                for (char c : str.toCharArray()) {
                    switch (c) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            if (!inField && !inChar) {
                                throw new IllegalArgumentException("Bad field syntax: " + str);
                            }
                            if (startPart) {
                                if (inChar) {
                                    startChar = startChar * 10 + (c - '0');
                                } else {
                                    startField = startField * 10 + (c - '0');
                                }
                            } else {
                                if (inChar) {
                                    endChar = endChar * 10 + (c - '0');
                                } else {
                                    endField = endField * 10 + (c - '0');
                                }
                            }
                            break;
                        case '.':
                            if (!inField) {
                                throw new IllegalArgumentException("Bad field syntax: " + str);
                            }
                            inField = false;
                            inChar = true;
                            break;
                        case 'n':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            numeric = true;
                            break;
                        case 'f':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            caseInsensitive = true;
                            break;
                        case 'r':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            reverse = true;
                            break;
                        case 'b':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            if (startPart) {
                                ignoreBlanksStart = true;
                            } else {
                                ignoreBlanksEnd = true;
                            }
                            break;
                        case ',':
                            inField = true;
                            inChar = false;
                            startPart = false;
                            break;
                        default:
                            throw new IllegalArgumentException("Bad field syntax: " + str);
                    }
                }
                if (!modifiers) {
                    ignoreBlanksStart = ignoreBlanksEnd = SortComparator.this.ignoreBlanks;
                    reverse = SortComparator.this.reverse;
                    caseInsensitive = SortComparator.this.caseInsensitive;
                    numeric = SortComparator.this.numeric;
                }
                if (startField < 1) {
                    throw new IllegalArgumentException("Bad field syntax: " + str);
                }
            }
        }
    }

}
