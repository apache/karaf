/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.felix.framework.capabilityset;

import java.util.ArrayList;
import java.util.List;

public class SimpleFilter
{
    public static final int AND = 1;
    public static final int OR = 2;
    public static final int NOT = 3;
    public static final int EQ = 4;
    public static final int LTE = 5;
    public static final int GTE = 6;
// TODO: FELIX3 - Should we handle substring as a separate operator or as a
//       special case of string equality comparison?
    public static final int SUBSTRING = 7;

    private final String m_name;
    private final Object m_value;
    private final int m_op;

    public SimpleFilter(String attr, Object value, int op)
    {
        m_name = attr;
        m_value = value;
        m_op = op;
    }

    public String getName()
    {
        return m_name;
    }

    public Object getValue()
    {
        return m_value;
    }

    public int getOperation()
    {
        return m_op;
    }

    public String toString()
    {
        String s = null;
        switch (m_op)
        {
            case AND:
                s = "(&" + toString((List) m_value) + ")";
                break;
            case OR:
                s = "(|" + toString((List) m_value) + ")";
                break;
            case NOT:
                s = "(!" + toString((List) m_value) + ")";
                break;
            case EQ:
                s = "(" + m_name + "=" + m_value + ")";
                break;
            case LTE:
                s = "(" + m_name + "<=" + m_value + ")";
                break;
            case GTE:
                s = "(" + m_name + ">=" + m_value + ")";
                break;
            case SUBSTRING:
                s = "(" + m_name + "=" + unparseSubstring((List<String>) m_value) + ")";
                break;
        }
        return s;
    }

    private String toString(List list)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < list.size(); i++)
        {
            sb.append(list.get(i).toString());
        }
        return sb.toString();
    }

    public static SimpleFilter parse(String filter)
    {
        int idx = skipWhitespace(filter, 0);

        if ((filter == null) || (filter.length() == 0) || (idx >= filter.length()))
        {
            throw new IllegalArgumentException("Null or empty filter.");
        }
        else if (filter.charAt(idx) != '(')
        {
            throw new IllegalArgumentException("Missing opening parenthesis: " + filter);
        }

        SimpleFilter sf = null;
        List stack = new ArrayList();
        while (idx < filter.length())
        {
            if (sf != null)
            {
                throw new IllegalArgumentException(
                    "Only one top-level operation allowed: " + filter);
            }
            if (filter.charAt(idx) == '(')
            {
                // Skip paren and following whitespace.
                idx = skipWhitespace(filter, idx + 1);

                if (filter.charAt(idx) == '&')
                {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(')
                    {
                        idx = peek - 1;
                        stack.add(0, new SimpleFilter(null, new ArrayList(), SimpleFilter.AND));
                    }
                    else
                    {
                        stack.add(0, new Integer(idx));
                    }
                }
                else if (filter.charAt(idx) == '|')
                {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(')
                    {
                        idx = peek - 1;
                        stack.add(0, new SimpleFilter(null, new ArrayList(), SimpleFilter.OR));
                    }
                    else
                    {
                        stack.add(0, new Integer(idx));
                    }
                }
                else if (filter.charAt(idx) == '!')
                {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(')
                    {
                        idx = peek - 1;
                        stack.add(0, new SimpleFilter(null, new ArrayList(), SimpleFilter.NOT));
                    }
                    else
                    {
                        stack.add(0, new Integer(idx));
                    }
                }
                else
                {
                    stack.add(0, new Integer(idx));
                }
            }
            else if (filter.charAt(idx) == ')')
            {
                Object top = stack.remove(0);
                if (top instanceof SimpleFilter)
                {
                    if (!stack.isEmpty() && (stack.get(0) instanceof SimpleFilter))
                    {
                        ((List) ((SimpleFilter) stack.get(0)).m_value).add(top);
                    }
                    else
                    {
                        sf = (SimpleFilter) top;
                    }
                }
                else if (!stack.isEmpty() && (stack.get(0) instanceof SimpleFilter))
                {
                    ((List) ((SimpleFilter) stack.get(0)).m_value).add(
                        SimpleFilter.subfilter(filter, ((Integer) top).intValue(), idx));
                }
                else
                {
                    sf = SimpleFilter.subfilter(filter, ((Integer) top).intValue(), idx);
                }
            }

            idx = skipWhitespace(filter, idx + 1);
        }

        if (sf == null)
        {
            throw new IllegalArgumentException("Missing closing parenthesis: " + filter);
        }

        return sf;
    }

    private static SimpleFilter subfilter(String filter, int startIdx, int endIdx)
    {
        final String opChars = "=<>";

        // Determine the ending index of the attribute name.
        int attrEndIdx = startIdx;
        for (int i = 0; i < (endIdx - startIdx); i++)
        {
            char c = filter.charAt(startIdx + i);
            if (opChars.indexOf(c) >= 0)
            {
                break;
            }
            else if (!Character.isWhitespace(c))
            {
                attrEndIdx = startIdx + i;
            }
        }
        if (attrEndIdx == startIdx)
        {
            throw new IllegalArgumentException(
                "Missing attribute name: " + filter.substring(startIdx, endIdx));
        }
        String attr = filter.substring(startIdx, attrEndIdx + 1);

        // Skip the attribute name and any following whitespace.
        startIdx = skipWhitespace(filter, attrEndIdx + 1);

        // Determine the operator type.
        int op = -1;
        switch (filter.charAt(startIdx))
        {
            case '=':
                op = EQ;
                startIdx++;
                break;
            case '<':
                if (filter.charAt(startIdx + 1) != '=')
                {
                    throw new IllegalArgumentException(
                        "Unknown operator: " + filter.substring(startIdx, endIdx));
                }
                op = LTE;
                startIdx += 2;
                break;
            case '>':
                if (filter.charAt(startIdx + 1) != '=')
                {
                    throw new IllegalArgumentException(
                        "Unknown operator: " + filter.substring(startIdx, endIdx));
                }
                op = GTE;
                startIdx += 2;
                break;
            default:
                throw new IllegalArgumentException(
                    "Unknown operator: " + filter.substring(startIdx, endIdx));
        }

        // Parse value.
        Object value = filter.subSequence(startIdx, endIdx);

        // Check if the equality comparison is actually a substring
        // comparison.
        if (op == EQ)
        {
            List<String> values = parseSubstring((String) value);
            if (values.size() > 1)
            {
                op = SUBSTRING;
                value = values;
            }
        }

        return new SimpleFilter(attr, value, op);
    }

// TODO: FELIX3 - Merge with Util class.
    public static List<String> parseSubstring(String value)
    {
        List<String> pieces = new ArrayList();
        StringBuffer ss = new StringBuffer();
        // int kind = SIMPLE; // assume until proven otherwise
        boolean wasStar = false; // indicates last piece was a star
        boolean leftstar = false; // track if the initial piece is a star
        boolean rightstar = false; // track if the final piece is a star

        int idx = 0;

        // We assume (sub)strings can contain leading and trailing blanks
loop:   for (;;)
        {
            if (idx >= value.length())
            {
                if (wasStar)
                {
                    // insert last piece as "" to handle trailing star
                    rightstar = true;
                }
                else
                {
                    pieces.add(ss.toString());
                    // accumulate the last piece
                    // note that in the case of
                    // (cn=); this might be
                    // the string "" (!=null)
                }
                ss.setLength(0);
                break loop;
            }

            char c = value.charAt(idx++);
            if (c == '*')
            {
                if (wasStar)
                {
                    // encountered two successive stars;
                    // I assume this is illegal
                    throw new IllegalArgumentException("Invalid filter string: " + value);
                }
                if (ss.length() > 0)
                {
                    pieces.add(ss.toString()); // accumulate the pieces
                    // between '*' occurrences
                }
                ss.setLength(0);
                // if this is a leading star, then track it
                if (pieces.size() == 0)
                {
                    leftstar = true;
                }
                wasStar = true;
            }
            else
            {
                wasStar = false;
                ss.append(c);
            }
        }
        if (leftstar || rightstar || pieces.size() > 1)
        {
            // insert leading and/or trailing "" to anchor ends
            if (rightstar)
            {
                pieces.add("");
            }
            if (leftstar)
            {
                pieces.add(0, "");
            }
        }
        return pieces;
    }

    public static String unparseSubstring(List<String> pieces)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < pieces.size(); i++)
        {
            if (i > 0)
            {
                sb.append("*");
            }
            sb.append(pieces.get(i));
        }
        return sb.toString();
    }

// TODO: FELIX3 - Merge with Util class.
    public static boolean compareSubstring(String s, List<String> pieces)
    {
        // Walk the pieces to match the string
        // There are implicit stars between each piece,
        // and the first and last pieces might be "" to anchor the match.
        // assert (pieces.length > 1)
        // minimal case is <string>*<string>

        boolean result = true;
        int len = pieces.size();

        int index = 0;

loop:   for (int i = 0; i < len; i++)
        {
            String piece = pieces.get(i);

            // If this is the first piece, then make sure the
            // string starts with it.
            if (i == 0)
            {
                if (!s.startsWith(piece))
                {
                    result = false;
                    break loop;
                }
            }

            // If this is the last piece, then make sure the
            // string ends with it.
            if (i == len - 1)
            {
                if (s.endsWith(piece))
                {
                    result = true;
                }
                else
                {
                    result = false;
                }
                break loop;
            }

            // If this is neither the first or last piece, then
            // make sure the string contains it.
            if ((i > 0) && (i < (len - 1)))
            {
                index = s.indexOf(piece, index);
                if (index < 0)
                {
                    result = false;
                    break loop;
                }
            }

            // Move string index beyond the matching piece.
            index += piece.length();
        }

        return result;
    }

    private static int skipWhitespace(String s, int startIdx)
    {
        int len = s.length();
        while ((startIdx < len) && Character.isWhitespace(s.charAt(startIdx)))
        {
            startIdx++;
        }
        return startIdx;
    }
}