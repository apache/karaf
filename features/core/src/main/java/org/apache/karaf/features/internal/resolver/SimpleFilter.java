/*
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
package org.apache.karaf.features.internal.resolver;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.utils.version.VersionRange;

@SuppressWarnings("rawtypes")
public class SimpleFilter {
    public static final int MATCH_ALL = 0;
    public static final int AND = 1;
    public static final int OR = 2;
    public static final int NOT = 3;
    public static final int EQ = 4;
    public static final int LTE = 5;
    public static final int GTE = 6;
    public static final int SUBSTRING = 7;
    public static final int PRESENT = 8;
    public static final int APPROX = 9;

    private final String name;
    private final Object value;
    private final int op;

    public SimpleFilter(String name, Object value, int op) {
        this.name = name;
        this.value = value;
        this.op = op;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public int getOperation() {
        return op;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void toString(StringBuilder sb)
    {
        switch (op)
        {
        case AND:
            sb.append("(&");
            toString(sb, (List) value);
            sb.append(")");
            break;
        case OR:
            sb.append("(|");
            toString(sb, (List) value);
            sb.append(")");
            break;
        case NOT:
            sb.append("(!");
            toString(sb, (List) value);
            sb.append(")");
            break;
        case EQ:
            sb.append("(")
                    .append(name)
                    .append("=");
            toEncodedString(sb, value);
            sb.append(")");
            break;
        case LTE:
            sb.append("(")
                    .append(name)
                    .append("<=");
            toEncodedString(sb, value);
            sb.append(")");
            break;
        case GTE:
            sb.append("(")
                    .append(name)
                    .append(">=");
            toEncodedString(sb, value);
            sb.append(")");
            break;
        case SUBSTRING:
            sb.append("(").append(name).append("=");
            unparseSubstring(sb, (List) value);
            sb.append(")");
            break;
        case PRESENT:
            sb.append("(").append(name).append("=*)");
            break;
        case APPROX:
            sb.append("(").append(name).append("~=");
            toEncodedString(sb, value);
            sb.append(")");
            break;
        case MATCH_ALL:
            sb.append("(*)");
            break;
        }
    }

    private static void toString(StringBuilder sb, List list) {
        for (Object o : list) {
            SimpleFilter sf = (SimpleFilter) o;
            sf.toString(sb);
        }
    }


    private static String toDecodedString(String s, int startIdx, int endIdx) {
        StringBuilder sb = null;
        boolean escaped = false;
        for (int i = startIdx; i < endIdx; i++) {
            char c = s.charAt(i);
            if (!escaped && (c == '\\')) {
                sb = new StringBuilder(endIdx - startIdx);
                sb.append(s, startIdx, i);
                escaped = true;
            } else {
                escaped = false;
                if (sb != null) {
                    sb.append(c);
                }
            }
        }
        return sb != null ? sb.toString() : s.substring(startIdx, endIdx);
    }

    private static void toEncodedString(StringBuilder sb, Object o) {
        if (o instanceof String) {
            String s = (String) o;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if ((c == '\\') || (c == '(') || (c == ')') || (c == '*')) {
                    sb.append('\\');
                }
                sb.append(c);
            }
        } else {
            sb.append(o);
        }
    }

    @SuppressWarnings("unchecked")
    public static SimpleFilter parse(String filter) {
        int idx = skipWhitespace(filter, 0);

        if ((filter == null) || (filter.length() == 0) || (idx >= filter.length())) {
            throw new IllegalArgumentException("Null or empty filter.");
        } else if (filter.charAt(idx) != '(') {
            throw new IllegalArgumentException("Missing opening parenthesis: " + filter);
        }

        SimpleFilter sf = null;
        Deque<Object> stack = new LinkedList<>();
        boolean isEscaped = false;
        while (idx < filter.length()) {
            if (sf != null) {
                throw new IllegalArgumentException(
                        "Only one top-level operation allowed: " + filter);
            }

            if (!isEscaped && (filter.charAt(idx) == '(')) {
                // Skip paren and following whitespace.
                idx = skipWhitespace(filter, idx + 1);

                if (filter.charAt(idx) == '&') {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(') {
                        idx = peek - 1;
                        stack.addFirst(new SimpleFilter(null, new ArrayList(), SimpleFilter.AND));
                    } else {
                        stack.addFirst(idx);
                    }
                } else if (filter.charAt(idx) == '|') {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(') {
                        idx = peek - 1;
                        stack.addFirst(new SimpleFilter(null, new ArrayList(), SimpleFilter.OR));
                    } else {
                        stack.addFirst(idx);
                    }
                } else if (filter.charAt(idx) == '!') {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(') {
                        idx = peek - 1;
                        stack.addFirst(new SimpleFilter(null, new ArrayList(), SimpleFilter.NOT));
                    } else {
                        stack.addFirst(idx);
                    }
                } else {
                    stack.addFirst(idx);
                }
            } else if (!isEscaped && (filter.charAt(idx) == ')')) {
                Object top = stack.removeFirst();
                Object next = stack.peekFirst();
                if (top instanceof SimpleFilter) {
                    if (next instanceof SimpleFilter) {
                        ((List<Object>) ((SimpleFilter) next).value).add(top);
                    } else {
                        sf = (SimpleFilter) top;
                    }
                } else if (next instanceof SimpleFilter) {
                    ((List<Object>) ((SimpleFilter) next).value).add(
                            SimpleFilter.subfilter(filter, (Integer) top, idx));
                } else {
                    sf = SimpleFilter.subfilter(filter, (Integer) top, idx);
                }
            } else {
                isEscaped = !isEscaped && (filter.charAt(idx) == '\\');
            }

            idx = skipWhitespace(filter, idx + 1);
        }

        if (sf == null) {
            throw new IllegalArgumentException("Missing closing parenthesis: " + filter);
        }

        return sf;
    }

    private static SimpleFilter subfilter(String filter, int startIdx, int endIdx) {
        final String opChars = "=<>~";

        // Determine the ending index of the attribute name.
        int attrEndIdx = startIdx;
        for (int i = 0; i < (endIdx - startIdx); i++) {
            char c = filter.charAt(startIdx + i);
            if (opChars.indexOf(c) >= 0) {
                break;
            } else if (!Character.isWhitespace(c)) {
                attrEndIdx = startIdx + i + 1;
            }
        }
        if (attrEndIdx == startIdx) {
            throw new IllegalArgumentException(
                    "Missing attribute name: " + filter.substring(startIdx, endIdx));
        }
        String attr = filter.substring(startIdx, attrEndIdx);

        // Skip the attribute name and any following whitespace.
        startIdx = skipWhitespace(filter, attrEndIdx);

        // Determine the operator type.
        int op;
        switch (filter.charAt(startIdx)) {
        case '=':
            op = EQ;
            startIdx++;
            break;
        case '<':
            if (filter.charAt(startIdx + 1) != '=') {
                throw new IllegalArgumentException(
                        "Unknown operator: " + filter.substring(startIdx, endIdx));
            }
            op = LTE;
            startIdx += 2;
            break;
        case '>':
            if (filter.charAt(startIdx + 1) != '=') {
                throw new IllegalArgumentException(
                        "Unknown operator: " + filter.substring(startIdx, endIdx));
            }
            op = GTE;
            startIdx += 2;
            break;
        case '~':
            if (filter.charAt(startIdx + 1) != '=') {
                throw new IllegalArgumentException(
                        "Unknown operator: " + filter.substring(startIdx, endIdx));
            }
            op = APPROX;
            startIdx += 2;
            break;
        default:
            throw new IllegalArgumentException(
                    "Unknown operator: " + filter.substring(startIdx, endIdx));
        }

        // Parse value.
        Object value = toDecodedString(filter, startIdx, endIdx);

        // Check if the equality comparison is actually a substring
        // or present operation.
        if (op == EQ) {
            String valueStr = filter.substring(startIdx, endIdx);
            List<String> values = parseSubstring(valueStr);
            if ((values.size() == 2)
                    && (values.get(0).length() == 0)
                    && (values.get(1).length() == 0)) {
                op = PRESENT;
            } else if (values.size() > 1) {
                op = SUBSTRING;
                value = values;
            }
        }

        return new SimpleFilter(attr, value, op);
    }

    public static List<String> parseSubstring(String value) {
        List<String> pieces = new ArrayList<>();
        int length = value.length();

        boolean isSimple = true;
        for (int idx = 0; idx < length; idx++) {
            char c = value.charAt(idx);
            if (c == '*' || c == '\\') {
                isSimple = false;
                break;
            }
        }
        if (isSimple) {
            pieces.add(value);
            return pieces;
        }

        StringBuilder ss = new StringBuilder();
        // int kind = SIMPLE; // assume until proven otherwise
        boolean wasStar = false; // indicates last piece was a star
        boolean leftstar = false; // track if the initial piece is a star
        boolean rightstar = false; // track if the final piece is a star

        int idx = 0;

        // We assume (sub)strings can contain leading and trailing blanks
        boolean escaped = false;
        for (;;) {
            if (idx >= length) {
                if (wasStar) {
                    // insert last piece as "" to handle trailing star
                    rightstar = true;
                } else {
                    pieces.add(ss.toString());
                    // accumulate the last piece
                    // note that in the case of
                    // (cn=); this might be
                    // the string "" (!=null)
                }
                ss.setLength(0);
                break;
            }

            // Read the next character and account for escapes.
            char c = value.charAt(idx++);
            if (!escaped && (c == '*')) {
                // If we have successive '*' characters, then we can
                // effectively collapse them by ignoring succeeding ones.
                if (!wasStar) {
                    if (ss.length() > 0) {
                        pieces.add(ss.toString()); // accumulate the pieces
                        // between '*' occurrences
                    }
                    ss.setLength(0);
                    // if this is a leading star, then track it
                    if (pieces.isEmpty()) {
                        leftstar = true;
                    }
                    wasStar = true;
                }
            } else if (!escaped && (c == '\\')) {
                escaped = true;
            } else {
                escaped = false;
                wasStar = false;
                ss.append(c);
            }
        }
        if (leftstar || rightstar || pieces.size() > 1) {
            // insert leading and/or trailing "" to anchor ends
            if (rightstar) {
                pieces.add("");
            }
            if (leftstar) {
                pieces.add(0, "");
            }
        }
        return pieces;
    }

    public static void unparseSubstring(StringBuilder sb, List<String> pieces) {
        for (int i = 0; i < pieces.size(); i++) {
            if (i > 0) {
                sb.append("*");
            }
            toEncodedString(sb, pieces.get(i));
        }
    }

    public static boolean compareSubstring(List<String> pieces, String s) {
        // Walk the pieces to match the string
        // There are implicit stars between each piece,
        // and the first and last pieces might be "" to anchor the match.
        // assert (pieces.length > 1)
        // minimal case is <string>*<string>

        boolean result = true;
        int len = pieces.size();

        // Special case, if there is only one piece, then
        // we must perform an equality test.
        if (len == 1) {
            return s.equals(pieces.get(0));
        }

        // Otherwise, check whether the pieces match
        // the specified string.

        int index = 0;

        for (int i = 0; i < len; i++) {
            String piece = pieces.get(i);

            // If this is the first piece, then make sure the
            // string starts with it.
            if (i == 0) {
                if (!s.startsWith(piece)) {
                    result = false;
                    break;
                }
            }

            // If this is the last piece, then make sure the
            // string ends with it.
            if (i == (len - 1)) {
                result = s.endsWith(piece) && (s.length() >= (index + piece.length()));
                break;
            }

            // If this is neither the first or last piece, then
            // make sure the string contains it.
            if ((i > 0) && (i < (len - 1))) {
                index = s.indexOf(piece, index);
                if (index < 0) {
                    result = false;
                    break;
                }
            }

            // Move string index beyond the matching piece.
            index += piece.length();
        }

        return result;
    }

    private static int skipWhitespace(String s, int startIdx) {
        int len = s.length();
        while ((startIdx < len) && Character.isWhitespace(s.charAt(startIdx))) {
            startIdx++;
        }
        return startIdx;
    }

    /**
     * Converts a attribute map to a filter. The filter is created by iterating
     * over the map's entry set. If ordering of attributes is important (e.g.,
     * for hitting attribute indices), then the map's entry set should iterate
     * in the desired order. Equality testing is assumed for all attribute types
     * other than version ranges, which are handled appropriated. If the attribute
     * map is empty, then a filter that matches anything is returned.
     *
     * @param attrs Map of attributes to convert to a filter.
     * @return A filter corresponding to the attributes.
     */
    @SuppressWarnings("unchecked")
    public static SimpleFilter convert(Map<String, Object> attrs) {
        // Rather than building a filter string to be parsed into a SimpleFilter,
        // we will just create the parsed SimpleFilter directly.

        List<SimpleFilter> filters = new ArrayList<>(attrs.size());

        for (Entry<String, Object> entry : attrs.entrySet()) {
            if (entry.getValue() instanceof VersionRange) {
                VersionRange vr = (VersionRange) entry.getValue();
                if (!vr.isOpenFloor()) {
                    filters.add(
                            new SimpleFilter(
                                    entry.getKey(),
                                    vr.getFloor().toString(),
                                    SimpleFilter.GTE)
                    );
                } else {
                    SimpleFilter not =
                            new SimpleFilter(null, new ArrayList(), SimpleFilter.NOT);
                    ((List<Object>) not.getValue()).add(
                            new SimpleFilter(
                                    entry.getKey(),
                                    vr.getFloor().toString(),
                                    SimpleFilter.LTE)
                    );
                    filters.add(not);
                }

                if (vr.getCeiling() != null) {
                    if (!vr.isOpenCeiling()) {
                        filters.add(
                                new SimpleFilter(
                                        entry.getKey(),
                                        vr.getCeiling().toString(),
                                        SimpleFilter.LTE)
                        );
                    } else if (!vr.getCeiling().equals(VersionRange.INFINITE_VERSION)) {
                        SimpleFilter not =
                                new SimpleFilter(null, new ArrayList(), SimpleFilter.NOT);
                        ((List<Object>) not.getValue()).add(
                                new SimpleFilter(
                                        entry.getKey(),
                                        vr.getCeiling().toString(),
                                        SimpleFilter.GTE)
                        );
                        filters.add(not);
                    }
                }
            } else {
                List<String> values = SimpleFilter.parseSubstring(entry.getValue().toString());
                if (values.size() > 1) {
                    filters.add(
                            new SimpleFilter(
                                    entry.getKey(),
                                    values,
                                    SimpleFilter.SUBSTRING)
                    );
                } else {
                    filters.add(
                            new SimpleFilter(
                                    entry.getKey(),
                                    values.get(0),
                                    SimpleFilter.EQ)
                    );
                }
            }
        }

        SimpleFilter sf = null;

        if (filters.size() == 1) {
            sf = filters.get(0);
        } else if (attrs.size() > 1) {
            sf = new SimpleFilter(null, filters, SimpleFilter.AND);
        } else if (filters.isEmpty()) {
            sf = new SimpleFilter(null, null, SimpleFilter.MATCH_ALL);
        }

        return sf;
    }
}
