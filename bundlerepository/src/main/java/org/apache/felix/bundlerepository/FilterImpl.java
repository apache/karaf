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
package org.apache.felix.bundlerepository;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;

import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

public class FilterImpl implements Filter
{
    private static final char WILDCARD = 65535;
    private static final int EQ = 0;
    private static final int LE = 1;
    private static final int GE = 2;
    private static final int APPROX = 3;
    private static final int LESS = 4;
    private static final int GREATER = 5;
    private static final int SUBSET = 6;
    private static final int SUPERSET = 7;
    private String m_filter;

    abstract class Query
    {
        static final String GARBAGE = "Trailing garbage";
        static final String MALFORMED = "Malformed query";
        static final String EMPTY = "Empty list";
        static final String SUBEXPR = "No subexpression";
        static final String OPERATOR = "Undefined operator";
        static final String TRUNCATED = "Truncated expression";
        static final String EQUALITY = "Only equality supported";
        private String m_tail;
        protected boolean m_caseSensitive = false;

        boolean match() throws IllegalArgumentException
        {
            m_tail = m_filter;
            boolean val = doQuery();
            if (m_tail.length() > 0)
            {
                error(GARBAGE);
            }
            return val;
        }

        private boolean doQuery() throws IllegalArgumentException
        {
            if (m_tail.length() < 3 || !prefix("("))
            {
                error(MALFORMED);
            }
            boolean val;

            switch (m_tail.charAt(0))
            {
                case '&':
                    val = doAnd();
                    break;
                case '|':
                    val = doOr();
                    break;
                case '!':
                    val = doNot();
                    break;
                default:
                    val = doSimple();
                    break;
            }

            if (!prefix(")"))
            {
                error(MALFORMED);
            }
            return val;
        }

        private boolean doAnd() throws IllegalArgumentException
        {
            m_tail = m_tail.substring(1);
            boolean val = true;
            if (!m_tail.startsWith("("))
            {
                error(EMPTY);
            }
            do
            {
                if (!doQuery())
                {
                    val = false;
                }
            }
            while (m_tail.startsWith("("));
            return val;
        }

        private boolean doOr() throws IllegalArgumentException
        {
            m_tail = m_tail.substring(1);
            boolean val = false;
            if (!m_tail.startsWith("("))
            {
                error(EMPTY);
            }
            do
            {
                if (doQuery())
                {
                    val = true;
                }
            }
            while (m_tail.startsWith("("));
            return val;
        }

        private boolean doNot() throws IllegalArgumentException
        {
            m_tail = m_tail.substring(1);
            if (!m_tail.startsWith("("))
            {
                error(SUBEXPR);
            }
            return !doQuery();
        }

        private boolean doSimple() throws IllegalArgumentException
        {
            int op = 0;
            Object attr = getAttr();

            if (prefix("="))
            {
                op = EQ;
            }
            else if (prefix("<="))
            {
                op = LE;
            }
            else if (prefix(">="))
            {
                op = GE;
            }
            else if (prefix("~="))
            {
                op = APPROX;
            }
            else if (prefix(":*>"))
            {
                op = SUPERSET;
            }
            else if (prefix(":<*"))
            {
                op = SUBSET;
            }
            else if (prefix("<"))
            {
                op = LESS;
            }
            else if (prefix(">"))
            {
                op = GREATER;
            }
            else
            {
                error(OPERATOR);
            }

            return compare(attr, op, getValue());
        }

        private boolean prefix(String pre)
        {
            if (!m_tail.startsWith(pre))
            {
                return false;
            }
            m_tail = m_tail.substring(pre.length());
            return true;
        }

        private Object getAttr()
        {
            int len = m_tail.length();
            int ix = 0;
            label:
            for (; ix < len; ix++)
            {
                switch (m_tail.charAt(ix))
                {
                    case '(':
                    case ')':
                    case '<':
                    case '>':
                    case '=':
                    case '~':
                    case '*':
                    case ':':
                    case '}':
                    case '{':
                    case '\\':
                        break label;
                }
            }
            String attr = m_tail.substring(0, ix).toLowerCase();
            m_tail = m_tail.substring(ix);
            return getProp(attr);
        }

        abstract Object getProp(String key);

        private String getValue()
        {
            StringBuffer sb = new StringBuffer();
            int len = m_tail.length();
            int ix = 0;
            label:
            for (; ix < len; ix++)
            {
                char c = m_tail.charAt(ix);
                switch (c)
                {
                    case '(':
                    case ')':
                        break label;
                    case '*':
                        sb.append(WILDCARD);
                        break;
                    case '\\':
                        if (ix == len - 1)
                        {
                            break label;
                        }
                        sb.append(m_tail.charAt(++ix));
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            m_tail = m_tail.substring(ix);
            return sb.toString();
        }

        private void error(String m) throws IllegalArgumentException
        {
            throw new IllegalArgumentException(m + " " + m_tail);
        }

        private boolean compare(Object obj, int op, String s)
        {
            if (obj == null && (op != SUBSET && op != SUPERSET))
            {
                return false;
            }
            try
            {
                Class numClass = null;
                if (obj != null)
                {
                    numClass = obj.getClass();
                }
                if (numClass == String.class && (op != SUBSET && op != SUPERSET))
                {
                    return compareString((String) obj, op, s);
                }
                else if (numClass == Character.class)
                {
                    return compareString(obj.toString(), op, s);
                }
                else if (numClass == Long.class)
                {
                    return compareSign(op, Long.valueOf(s).compareTo((Long) obj));
                }
                else if (numClass == Integer.class)
                {
                    return compareSign(op, Integer.valueOf(s).compareTo((Integer) obj));
                }
                else if (numClass == Short.class)
                {
                    return compareSign(op, Short.valueOf(s).compareTo((Short) obj));
                }
                else if (numClass == Byte.class)
                {
                    return compareSign(op, Byte.valueOf(s).compareTo((Byte) obj));
                }
                else if (numClass == Double.class)
                {
                    return compareSign(op, Double.valueOf(s).compareTo((Double) obj));
                }
                else if (numClass == Float.class)
                {
                    return compareSign(op, Float.valueOf(s).compareTo((Float) obj));
                }
                else if (numClass == Boolean.class)
                {
                    if (op != EQ)
                    {
                        return false;
                    }
                    int a = Boolean.valueOf(s).booleanValue() ? 1 : 0;
                    int b = ((Boolean) obj).booleanValue() ? 1 : 0;
                    return compareSign(op, a - b);
                }
                else if (numClass == BigInteger.class)
                {
                    return compareSign(op, new BigInteger(s).compareTo((BigInteger) obj));
                }
                else if (obj instanceof Collection)
                {
                    if (op == SUBSET || op == SUPERSET)
                    {
                        StringSet set = new StringSet(s);
                        if (op == SUBSET)
                        {
                            return set.containsAll((Collection) obj);
                        }
                        else
                        {
                            return ((Collection) obj).containsAll(set);
                        }
                    }

                    for (Iterator i = ((Collection) obj).iterator(); i.hasNext();)
                    {
                        Object element = i.next();
                        if (compare(element, op, s))
                        {
                            return true;
                        }
                    }
                }
                else if (numClass.isArray())
                {
                    int len = Array.getLength(obj);
                    for (int i = 0; i < len; i++)
                    {
                        if (compare(Array.get(obj, i), op, s))
                        {
                            return true;
                        }
                    }
                }
                else
                {
                    try
                    {
                        if (op == SUPERSET || op == SUBSET)
                        {
                            StringSet set = new StringSet(s);
                            StringSet objSet = new StringSet((String) obj);

                            if (op == SUPERSET)
                            {

                                boolean found = true;
                                Iterator iterator = set.iterator();
                                while (iterator.hasNext() && found)
                                {
                                    Object object = (Object) iterator.next();
                                    if (!objSet.contains(object))
                                    {
                                        found = false;
                                    }
                                }

                                return found;
                            }
                            else
                            {
                                return set.containsAll(objSet);
                            }
                        }
                        else
                        {
                            Constructor constructor = numClass.getConstructor(new Class[]
                                {
                                    String.class
                                });
                            Object instance = constructor.newInstance(new Object[]
                                {
                                    s
                                });
                            switch (op)
                            {
                                case EQ:
                                    return obj.equals(instance);
                                case LESS:
                                    return ((Comparable) obj).compareTo(instance) < 0;
                                case GREATER:
                                    return ((Comparable) obj).compareTo(instance) > 0;
                                case LE:
                                    return ((Comparable) obj).compareTo(instance) <= 0;
                                case GE:
                                    return ((Comparable) obj).compareTo(instance) >= 0;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        // Ignore
                    }
                }
            }
            catch (Exception e)
            {
            }
            return false;
        }
    }

    class DictQuery extends Query
    {
        private Dictionary m_dict;

        DictQuery(Dictionary dict)
        {
            m_dict = dict;
        }

        DictQuery(Dictionary dict, boolean caseSensitive)
        {
            m_dict = dict;
            m_caseSensitive = caseSensitive;
        }

        Object getProp(String key)
        {

            if (m_caseSensitive)
            {
                return m_dict.get(key);
            }
            else
            {
                Enumeration keys = m_dict.keys();
                while (keys.hasMoreElements())
                {
                    String propertyKey = (String) keys.nextElement();
                    if (propertyKey.equalsIgnoreCase(key))
                    {
                        return m_dict.get(propertyKey);
                    }
                }
            }

            return null;
        }
    }

    class ServiceReferenceQuery extends Query
    {
        private ServiceReference m_ref;

        public ServiceReferenceQuery(ServiceReference ref)
        {
            m_ref = ref;
        }

        Object getProp(String key)
        {

            if (m_caseSensitive)
            {
                return m_ref.getProperty(key);
            }
            else
            {

                String[] propertyKeys = m_ref.getPropertyKeys();
                for (int i = 0; i < propertyKeys.length; i++)
                {
                    String propertyKey = propertyKeys[i];
                    if (propertyKey.equalsIgnoreCase(key))
                    {
                        return m_ref.getProperty(propertyKey);
                    }
                }
            }

            return null;

        }
    }

    public FilterImpl(String filter) throws IllegalArgumentException
    {
        // NYI: Normalize the filter string?
        this.m_filter = filter;
        if (filter == null || filter.length() == 0)
        {
            throw new IllegalArgumentException("Null query");
        }
    }

    public String toString()
    {
        return m_filter;
    }

    public boolean equals(Object obj)
    {
        return obj != null && obj instanceof FilterImpl && m_filter.equals(((FilterImpl) obj).m_filter);
    }

    public int hashCode()
    {
        return m_filter.hashCode();
    }

    private static boolean compareString(String s1, int op, String s2)
    {
        switch (op)
        {
            case EQ:
                return patSubstr(s1, s2);
            case APPROX:
                return patSubstr(fixupString(s1), fixupString(s2));
            default:
                return compareSign(op, s2.compareTo(s1));
        }
    }

    private static boolean compareSign(int op, int cmp)
    {
        switch (op)
        {
            case LE:
                return cmp >= 0;
            case GE:
                return cmp <= 0;
            case EQ:
                return cmp == 0;
            default: /* APPROX */
                return cmp == 0;
        }
    }

    private static String fixupString(String s)
    {
        StringBuffer sb = new StringBuffer();
        int len = s.length();
        boolean isStart = true;
        boolean isWhite = false;
        for (int i = 0; i < len; i++)
        {
            char c = s.charAt(i);
            if (Character.isWhitespace(c))
            {
                isWhite = true;
            }
            else
            {
                if (!isStart && isWhite)
                {
                    sb.append(' ');
                }
                if (Character.isUpperCase(c))
                {
                    c = Character.toLowerCase(c);
                }
                sb.append(c);
                isStart = false;
                isWhite = false;
            }
        }
        return sb.toString();
    }

    private static boolean patSubstr(String s, String pat)
    {
        if (s == null)
        {
            return false;
        }
        if (pat.length() == 0)
        {
            return s.length() == 0;
        }
        if (pat.charAt(0) == WILDCARD)
        {
            pat = pat.substring(1);
            for (;;)
            {
                if (patSubstr(s, pat))
                {
                    return true;
                }
                if (s.length() == 0)
                {
                    return false;
                }
                s = s.substring(1);
            }
        }
        else
        {
            if (s.length() == 0 || s.charAt(0) != pat.charAt(0))
            {
                return false;
            }
            return patSubstr(s.substring(1), pat.substring(1));
        }
    }

    public boolean match(Dictionary dict)
    {
        try
        {
            return new DictQuery(dict).match();
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    public boolean match(ServiceReference reference)
    {
        try
        {
            return new ServiceReferenceQuery(reference).match();
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }

    }

    public boolean matchCase(Dictionary dictionary)
    {
        try
        {
            return new DictQuery(dictionary, true).match();
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }
}