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
/*
 * Copyright (c) OSGi Alliance (2005, 2009). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.bundlerepository;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import javax.security.auth.x500.X500Principal;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class FilterImpl implements Filter {

    /* filter operators */
    private static final int			EQUAL		= 1;
    private static final int			APPROX		= 2;
    private static final int			GREATER		= 3;
    private static final int			LESS		= 4;
    private static final int			PRESENT		= 5;
    private static final int			SUBSTRING	= 6;
    private static final int			AND			= 7;
    private static final int			OR			= 8;
    private static final int			NOT			= 9;
    private static final int			SUBSET		= 10;
    private static final int			SUPERSET	= 11;

    /** filter operation */
    private final int					op;
    /** filter attribute or null if operation AND, OR or NOT */
    private final String				attr;
    /** filter operands */
    private final Object				value;
    /** optim in case of version */
    private final Object                converted;

    /* normalized filter string for Filter object */
    private transient volatile String	filterString;

    /**
     * Constructs a {@link FilterImpl} object. This filter object may be
     * used to match a {@link org.osgi.framework.ServiceReference} or a Dictionary.
     *
     * <p>
     * If the filter cannot be parsed, an {@link org.osgi.framework.InvalidSyntaxException}
     * will be thrown with a human readable message where the filter became
     * unparsable.
     *
     * @param filterString the filter string.
     * @exception InvalidSyntaxException If the filter parameter contains an
     *            invalid filter string that cannot be parsed.
     */
    static FilterImpl newInstance(String filterString)
            throws InvalidSyntaxException {
        return newInstance(filterString, false);
    }

    static FilterImpl newInstance(String filterString, boolean ignoreCase)
            throws InvalidSyntaxException {
        return new Parser(filterString, ignoreCase).parse();
    }

    FilterImpl(int operation, String attr, Object value) {
        this.op = operation;
        this.attr = attr;
        this.value = value;
        Object conv = null;
        try {
            if (op == SUBSET || op == SUPERSET)
            {
                conv = getSet(value);
            }
            else if ("version".equalsIgnoreCase(attr))
            {
                if (value instanceof String) {
                    conv = new Version((String) value);
                } else if (value instanceof Version) {
                    conv = (Version) value;
                }
            }
        } catch (Throwable t) {
            // Ignore any conversion issue
        }
        converted = conv;
    }


    /**
     * Filter using a service's properties.
     * <p>
     * This <code>Filter</code> is executed using the keys and values of the
     * referenced service's properties. The keys are case insensitively
     * matched with this <code>Filter</code>.
     *
     * @param reference The reference to the service whose properties are
     *        used in the match.
     * @return <code>true</code> if the service's properties match this
     *         <code>Filter</code>; <code>false</code> otherwise.
     */
    public boolean match(ServiceReference reference) {
        return match0(new ServiceReferenceDictionary(reference));
    }

    /**
     * Filter using a <code>Dictionary</code>. This <code>Filter</code> is
     * executed using the specified <code>Dictionary</code>'s keys and
     * values. The keys are case insensitively matched with this
     * <code>Filter</code>.
     *
     * @param dictionary The <code>Dictionary</code> whose keys are used in
     *        the match.
     * @return <code>true</code> if the <code>Dictionary</code>'s keys and
     *         values match this filter; <code>false</code> otherwise.
     * @throws IllegalArgumentException If <code>dictionary</code> contains
     *         case variants of the same key name.
     */
    public boolean match(Dictionary dictionary) {
        return match0(new CaseInsensitiveDictionary(dictionary));
    }

    /**
     * Filter with case sensitivity using a <code>Dictionary</code>. This
     * <code>Filter</code> is executed using the specified
     * <code>Dictionary</code>'s keys and values. The keys are case
     * sensitively matched with this <code>Filter</code>.
     *
     * @param dictionary The <code>Dictionary</code> whose keys are used in
     *        the match.
     * @return <code>true</code> if the <code>Dictionary</code>'s keys and
     *         values match this filter; <code>false</code> otherwise.
     * @since 1.3
     */
    public boolean matchCase(Dictionary dictionary) {
        return match0(dictionary);
    }

    /**
     * Filter using a <code>Map</code>. This <code>Filter</code> is
     * executed using the specified <code>Map</code>'s keys and
     * values. The keys are case insensitively matched with this
     * <code>Filter</code>.
     *
     * @param map The <code>Map</code> whose keys are used in
     *        the match.
     * @return <code>true</code> if the <code>Map</code>'s keys and
     *         values match this filter; <code>false</code> otherwise.
     * @throws IllegalArgumentException If <code>map</code> contains
     *         case variants of the same key name.
     */
    public boolean matchCase(Map map) {
        return match0(map);
    }

    /**
     * Returns this <code>Filter</code>'s filter string.
     * <p>
     * The filter string is normalized by removing whitespace which does not
     * affect the meaning of the filter.
     *
     * @return This <code>Filter</code>'s filter string.
     */
    public String toString() {
        String result = filterString;
        if (result == null) {
            filterString = result = normalize();
        }
        return result;
    }

    /**
     * Returns this <code>Filter</code>'s normalized filter string.
     * <p>
     * The filter string is normalized by removing whitespace which does not
     * affect the meaning of the filter.
     *
     * @return This <code>Filter</code>'s filter string.
     */
    private String normalize() {
        StringBuffer sb = new StringBuffer();
        sb.append('(');

        switch (op) {
            case AND : {
                sb.append('&');

                FilterImpl[] filters = (FilterImpl[]) value;
                for (int i = 0, size = filters.length; i < size; i++) {
                    sb.append(filters[i].normalize());
                }

                break;
            }

            case OR : {
                sb.append('|');

                FilterImpl[] filters = (FilterImpl[]) value;
                for (int i = 0, size = filters.length; i < size; i++) {
                    sb.append(filters[i].normalize());
                }

                break;
            }

            case NOT : {
                sb.append('!');
                FilterImpl filter = (FilterImpl) value;
                sb.append(filter.normalize());

                break;
            }

            case SUBSTRING : {
                sb.append(attr);
                sb.append('=');

                String[] substrings = (String[]) value;

                for (int i = 0, size = substrings.length; i < size; i++) {
                    String substr = substrings[i];

                    if (substr == null) /* * */{
                        sb.append('*');
                    }
                    else /* xxx */{
                        sb.append(encodeValue(substr));
                    }
                }

                break;
            }
            case EQUAL : {
                sb.append(attr);
                sb.append('=');
                sb.append(encodeValue((String) value));

                break;
            }
            case GREATER : {
                sb.append(attr);
                sb.append(">=");
                sb.append(encodeValue((String) value));

                break;
            }
            case LESS : {
                sb.append(attr);
                sb.append("<=");
                sb.append(encodeValue((String) value));

                break;
            }
            case APPROX : {
                sb.append(attr);
                sb.append("~=");
                sb.append(encodeValue(approxString((String) value)));

                break;
            }
            case PRESENT : {
                sb.append(attr);
                sb.append("=*");

                break;
            }
            case SUBSET : {
                sb.append(attr);
                sb.append(":<*");
                sb.append(encodeValue(approxString((String) value)));

                break;
            }
            case SUPERSET : {
                sb.append(attr);
                sb.append(":*>");
                sb.append(encodeValue(approxString((String) value)));

                break;
            }
        }

        sb.append(')');

        return sb.toString();
    }

    /**
     * Compares this <code>Filter</code> to another <code>Filter</code>.
     *
     * <p>
     * This implementation returns the result of calling
     * <code>this.toString().equals(obj.toString()</code>.
     *
     * @param obj The object to compare against this <code>Filter</code>.
     * @return If the other object is a <code>Filter</code> object, then
     *         returns the result of calling
     *         <code>this.toString().equals(obj.toString()</code>;
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof FilterImpl)) {
            return false;
        }

        return this.toString().equals(obj.toString());
    }

    /**
     * Returns the hashCode for this <code>Filter</code>.
     *
     * <p>
     * This implementation returns the result of calling
     * <code>this.toString().hashCode()</code>.
     *
     * @return The hashCode of this <code>Filter</code>.
     */
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * Internal match routine. Dictionary parameter must support
     * case-insensitive get.
     *
     * @param properties A dictionary whose keys are used in the match.
     * @return If the Dictionary's keys match the filter, return
     *         <code>true</code>. Otherwise, return <code>false</code>.
     */
    private boolean match0(Dictionary properties) {
        switch (op) {
            case AND : {
                FilterImpl[] filters = (FilterImpl[]) value;
                for (int i = 0, size = filters.length; i < size; i++) {
                    if (!filters[i].match0(properties)) {
                        return false;
                    }
                }

                return true;
            }

            case OR : {
                FilterImpl[] filters = (FilterImpl[]) value;
                for (int i = 0, size = filters.length; i < size; i++) {
                    if (filters[i].match0(properties)) {
                        return true;
                    }
                }

                return false;
            }

            case NOT : {
                FilterImpl filter = (FilterImpl) value;

                return !filter.match0(properties);
            }

            case SUBSTRING :
            case EQUAL :
            case GREATER :
            case LESS :
            case APPROX :
            case SUBSET :
            case SUPERSET : {
                Object prop = (properties == null) ? null : properties
                        .get(attr);

                return compare(op, prop, value);
            }

            case PRESENT : {
                Object prop = (properties == null) ? null : properties
                        .get(attr);

                return prop != null;
            }
        }

        return false;
    }

    private boolean match0(Map properties) {
        switch (op) {
            case AND : {
                FilterImpl[] filters = (FilterImpl[]) value;
                for (int i = 0, size = filters.length; i < size; i++) {
                    if (!filters[i].match0(properties)) {
                        return false;
                    }
                }

                return true;
            }

            case OR : {
                FilterImpl[] filters = (FilterImpl[]) value;
                for (int i = 0, size = filters.length; i < size; i++) {
                    if (filters[i].match0(properties)) {
                        return true;
                    }
                }

                return false;
            }

            case NOT : {
                FilterImpl filter = (FilterImpl) value;

                return !filter.match0(properties);
            }

            case SUBSTRING :
            case EQUAL :
            case GREATER :
            case LESS :
            case APPROX :
            case SUBSET :
            case SUPERSET : {
                Object prop = (properties == null) ? null : properties
                        .get(attr);

                return compare(op, prop, value);
            }

            case PRESENT : {
                Object prop = (properties == null) ? null : properties
                        .get(attr);

                return prop != null;
            }
        }

        return false;
    }
    /**
     * Encode the value string such that '(', '*', ')' and '\' are escaped.
     *
     * @param value unencoded value string.
     * @return encoded value string.
     */
    private static String encodeValue(String value) {
        boolean encoded = false;
        int inlen = value.length();
        int outlen = inlen << 1; /* inlen 2 */

        char[] output = new char[outlen];
        value.getChars(0, inlen, output, inlen);

        int cursor = 0;
        for (int i = inlen; i < outlen; i++) {
            char c = output[i];

            switch (c) {
                case '(' :
                case '*' :
                case ')' :
                case '\\' : {
                    output[cursor] = '\\';
                    cursor++;
                    encoded = true;

                    break;
                }
            }

            output[cursor] = c;
            cursor++;
        }

        return encoded ? new String(output, 0, cursor) : value;
    }

    private Collection getSet(Object value)
    {
        Collection s;
        if (value instanceof Set)
        {
            s = (Set) value;
        }
        else if (value instanceof Collection)
        {
            s = (Collection) value;
            if (s.size() > 1) {
                s = new HashSet(s);
            }
        }
        else if (value != null)
        {
            String v = value.toString();
            if (v.indexOf(',') < 0)
            {
                s = Collections.singleton(v);
            }
            else {
                StringTokenizer st = new StringTokenizer(value.toString(), ",");
                s = new HashSet();
                while (st.hasMoreTokens())
                {
                    s.add(st.nextToken().trim());
                }
            }
        }
        else
        {
            s = Collections.emptySet();
        }
        return s;
    }

    private boolean compare(int operation, Object value1, Object value2) {
        if (op == SUPERSET || op == SUBSET)
        {
            Collection s1 = getSet(value1);
            Collection s2 = converted instanceof Collection ? (Collection) converted : getSet(value2);
            if (op == SUPERSET)
            {
                return s1.containsAll(s2);
            }
            else
            {
                return s2.containsAll(s1);
            }
        }

        if (value1 == null) {
            return false;
        }
        if (value1 instanceof String) {
            return compare_String(operation, (String) value1, value2);
        }

        Class clazz = value1.getClass();
        if (clazz.isArray()) {
            Class type = clazz.getComponentType();
            if (type.isPrimitive()) {
                return compare_PrimitiveArray(operation, type, value1,
                        value2);
            }
            return compare_ObjectArray(operation, (Object[]) value1, value2);
        }
        if (value1 instanceof Version) {
            if (converted != null) {
                switch (operation) {
                    case APPROX :
                    case EQUAL : {
                        return ((Version) value1).compareTo(converted) == 0;
                    }
                    case GREATER : {
                        return ((Version) value1).compareTo(converted) >= 0;
                    }
                    case LESS : {
                        return ((Version) value1).compareTo(converted) <= 0;
                    }
                }
            } else {
                return compare_Comparable(operation, (Version) value1, value2);
            }
        }
        if (value1 instanceof Collection) {
            return compare_Collection(operation, (Collection) value1,
                    value2);
        }
        if (value1 instanceof Integer) {
            return compare_Integer(operation,
                    ((Integer) value1).intValue(), value2);
        }
        if (value1 instanceof Long) {
            return compare_Long(operation, ((Long) value1).longValue(),
                    value2);
        }
        if (value1 instanceof Byte) {
            return compare_Byte(operation, ((Byte) value1).byteValue(),
                    value2);
        }
        if (value1 instanceof Short) {
            return compare_Short(operation, ((Short) value1).shortValue(),
                    value2);
        }
        if (value1 instanceof Character) {
            return compare_Character(operation, ((Character) value1)
                    .charValue(), value2);
        }
        if (value1 instanceof Float) {
            return compare_Float(operation, ((Float) value1).floatValue(),
                    value2);
        }
        if (value1 instanceof Double) {
            return compare_Double(operation, ((Double) value1)
                    .doubleValue(), value2);
        }
        if (value1 instanceof Boolean) {
            return compare_Boolean(operation, ((Boolean) value1)
                    .booleanValue(), value2);
        }
        if (value1 instanceof Comparable) {
            return compare_Comparable(operation, (Comparable) value1,
                    value2);
        }
        return compare_Unknown(operation, value1, value2); // RFC 59
    }

    private boolean compare_Collection(int operation,
            Collection collection, Object value2) {
        if (op == SUBSET || op == SUPERSET)
        {
            StringSet set = new StringSet(value2.toString());
            if (op == SUBSET)
            {
                return set.containsAll(collection);
            }
            else
            {
                return (collection).containsAll(set);
            }
        }
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            if (compare(operation, iterator.next(), value2)) {
                return true;
            }
        }
        return false;
    }

    private boolean compare_ObjectArray(int operation, Object[] array,
            Object value2) {
        for (int i = 0, size = array.length; i < size; i++) {
            if (compare(operation, array[i], value2)) {
                return true;
            }
        }
        return false;
    }

    private boolean compare_PrimitiveArray(int operation, Class type,
            Object primarray, Object value2) {
        if (Integer.TYPE.isAssignableFrom(type)) {
            int[] array = (int[]) primarray;
            for (int i = 0, size = array.length; i < size; i++) {
                if (compare_Integer(operation, array[i], value2)) {
                    return true;
                }
            }
            return false;
        }
        if (Long.TYPE.isAssignableFrom(type)) {
            long[] array = (long[]) primarray;
            for (int i = 0, size = array.length; i < size; i++) {
                if (compare_Long(operation, array[i], value2)) {
                    return true;
                }
            }
            return false;
        }
        if (Byte.TYPE.isAssignableFrom(type)) {
            byte[] array = (byte[]) primarray;
            for (int i = 0, size = array.length; i < size; i++) {
                if (compare_Byte(operation, array[i], value2)) {
                    return true;
                }
            }
            return false;
        }
        if (Short.TYPE.isAssignableFrom(type)) {
            short[] array = (short[]) primarray;
            for (int i = 0, size = array.length; i < size; i++) {
                if (compare_Short(operation, array[i], value2)) {
                    return true;
                }
            }
            return false;
        }
        if (Character.TYPE.isAssignableFrom(type)) {
            char[] array = (char[]) primarray;
            for (int i = 0, size = array.length; i < size; i++) {
                if (compare_Character(operation, array[i], value2)) {
                    return true;
                }
            }
            return false;
        }
        if (Float.TYPE.isAssignableFrom(type)) {
            float[] array = (float[]) primarray;
            for (int i = 0, size = array.length; i < size; i++) {
                if (compare_Float(operation, array[i], value2)) {
                    return true;
                }
            }
            return false;
        }
        if (Double.TYPE.isAssignableFrom(type)) {
            double[] array = (double[]) primarray;
            for (int i = 0, size = array.length; i < size; i++) {
                if (compare_Double(operation, array[i], value2)) {
                    return true;
                }
            }
            return false;
        }
        if (Boolean.TYPE.isAssignableFrom(type)) {
            boolean[] array = (boolean[]) primarray;
            for (int i = 0, size = array.length; i < size; i++) {
                if (compare_Boolean(operation, array[i], value2)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean compare_String(int operation, String string,
            Object value2) {
        switch (operation) {
            case SUBSTRING : {
                String[] substrings = (String[]) value2;
                int pos = 0;
                for (int i = 0, size = substrings.length; i < size; i++) {
                    String substr = substrings[i];

                    if (i + 1 < size) /* if this is not that last substr */{
                        if (substr == null) /* * */{
                            String substr2 = substrings[i + 1];

                            if (substr2 == null) /* ** */
                                continue; /* ignore first star */
                            /* xxx */
                            int index = string.indexOf(substr2, pos);
                            if (index == -1) {
                                return false;
                            }

                            pos = index + substr2.length();
                            if (i + 2 < size) // if there are more
                                // substrings, increment
                                // over the string we just
                                // matched; otherwise need
                                // to do the last substr
                                // check
                                i++;
                        }
                        else /* xxx */{
                            int len = substr.length();
                            if (string.regionMatches(pos, substr, 0, len)) {
                                pos += len;
                            }
                            else {
                                return false;
                            }
                        }
                    }
                    else /* last substr */{
                        if (substr == null) /* * */{
                            return true;
                        }
                        /* xxx */
                        return string.endsWith(substr);
                    }
                }

                return true;
            }
            case EQUAL : {
                return string.equals(value2);
            }
            case APPROX : {
                string = approxString(string);
                String string2 = approxString((String) value2);

                return string.equalsIgnoreCase(string2);
            }
            case GREATER : {
                return string.compareTo((String) value2) >= 0;
            }
            case LESS : {
                return string.compareTo((String) value2) <= 0;
            }
        }
        return false;
    }

    private boolean compare_Integer(int operation, int intval, Object value2) {
        if (operation == SUBSTRING) {
            return false;
        }
        int intval2;
        try {
            intval2 = Integer.parseInt(((String) value2).trim());
        }
        catch (IllegalArgumentException e) {
            return false;
        }
        switch (operation) {
            case APPROX :
            case EQUAL : {
                return intval == intval2;
            }
            case GREATER : {
                return intval >= intval2;
            }
            case LESS : {
                return intval <= intval2;
            }
        }
        return false;
    }

    private boolean compare_Long(int operation, long longval, Object value2) {
        if (operation == SUBSTRING) {
            return false;
        }
        long longval2;
        try {
            longval2 = Long.parseLong(((String) value2).trim());
        }
        catch (IllegalArgumentException e) {
            return false;
        }

        switch (operation) {
            case APPROX :
            case EQUAL : {
                return longval == longval2;
            }
            case GREATER : {
                return longval >= longval2;
            }
            case LESS : {
                return longval <= longval2;
            }
        }
        return false;
    }

    private boolean compare_Byte(int operation, byte byteval, Object value2) {
        if (operation == SUBSTRING) {
            return false;
        }
        byte byteval2;
        try {
            byteval2 = Byte.parseByte(((String) value2).trim());
        }
        catch (IllegalArgumentException e) {
            return false;
        }

        switch (operation) {
            case APPROX :
            case EQUAL : {
                return byteval == byteval2;
            }
            case GREATER : {
                return byteval >= byteval2;
            }
            case LESS : {
                return byteval <= byteval2;
            }
        }
        return false;
    }

    private boolean compare_Short(int operation, short shortval,
            Object value2) {
        if (operation == SUBSTRING) {
            return false;
        }
        short shortval2;
        try {
            shortval2 = Short.parseShort(((String) value2).trim());
        }
        catch (IllegalArgumentException e) {
            return false;
        }

        switch (operation) {
            case APPROX :
            case EQUAL : {
                return shortval == shortval2;
            }
            case GREATER : {
                return shortval >= shortval2;
            }
            case LESS : {
                return shortval <= shortval2;
            }
        }
        return false;
    }

    private boolean compare_Character(int operation, char charval,
            Object value2) {
        if (operation == SUBSTRING) {
            return false;
        }
        char charval2;
        try {
            charval2 = ((String) value2).charAt(0);
        }
        catch (IndexOutOfBoundsException e) {
            return false;
        }

        switch (operation) {
            case EQUAL : {
                return charval == charval2;
            }
            case APPROX : {
                return (charval == charval2)
                        || (Character.toUpperCase(charval) == Character
                                .toUpperCase(charval2))
                        || (Character.toLowerCase(charval) == Character
                                .toLowerCase(charval2));
            }
            case GREATER : {
                return charval >= charval2;
            }
            case LESS : {
                return charval <= charval2;
            }
        }
        return false;
    }

    private boolean compare_Boolean(int operation, boolean boolval,
            Object value2) {
        if (operation == SUBSTRING) {
            return false;
        }
        boolean boolval2 = Boolean.valueOf(((String) value2).trim())
                .booleanValue();
        switch (operation) {
            case APPROX :
            case EQUAL :
            case GREATER :
            case LESS : {
                return boolval == boolval2;
            }
        }
        return false;
    }

    private boolean compare_Float(int operation, float floatval,
            Object value2) {
        if (operation == SUBSTRING) {
            return false;
        }
        float floatval2;
        try {
            floatval2 = Float.parseFloat(((String) value2).trim());
        }
        catch (IllegalArgumentException e) {
            return false;
        }

        switch (operation) {
            case APPROX :
            case EQUAL : {
                return Float.compare(floatval, floatval2) == 0;
            }
            case GREATER : {
                return Float.compare(floatval, floatval2) >= 0;
            }
            case LESS : {
                return Float.compare(floatval, floatval2) <= 0;
            }
        }
        return false;
    }

    private boolean compare_Double(int operation, double doubleval,
            Object value2) {
        if (operation == SUBSTRING) {
            return false;
        }
        double doubleval2;
        try {
            doubleval2 = Double.parseDouble(((String) value2).trim());
        }
        catch (IllegalArgumentException e) {
            return false;
        }

        switch (operation) {
            case APPROX :
            case EQUAL : {
                return Double.compare(doubleval, doubleval2) == 0;
            }
            case GREATER : {
                return Double.compare(doubleval, doubleval2) >= 0;
            }
            case LESS : {
                return Double.compare(doubleval, doubleval2) <= 0;
            }
        }
        return false;
    }

    private static final Class[]	constructorType	= new Class[] {String.class};

    private boolean compare_Comparable(int operation, Comparable value1,
            Object value2) {
        if (operation == SUBSTRING) {
            return false;
        }
        Constructor constructor;
        try {
            constructor = value1.getClass().getConstructor(constructorType);
        }
        catch (NoSuchMethodException e) {
            return false;
        }
        try {
            if (!constructor.isAccessible())
                AccessController.doPrivileged(new SetAccessibleAction(
                        constructor));
            value2 = constructor
                    .newInstance(new Object[] {((String) value2).trim()});
        }
        catch (IllegalAccessException e) {
            return false;
        }
        catch (InvocationTargetException e) {
            return false;
        }
        catch (InstantiationException e) {
            return false;
        }

        switch (operation) {
            case APPROX :
            case EQUAL : {
                return value1.compareTo(value2) == 0;
            }
            case GREATER : {
                return value1.compareTo(value2) >= 0;
            }
            case LESS : {
                return value1.compareTo(value2) <= 0;
            }
        }
        return false;
    }

    private boolean compare_Unknown(int operation, Object value1,
            Object value2) {
        if (operation == SUBSTRING) {
            return false;
        }
        Constructor constructor;
        try {
            constructor = value1.getClass().getConstructor(constructorType);
        }
        catch (NoSuchMethodException e) {
            return false;
        }
        try {
            if (!constructor.isAccessible())
                AccessController.doPrivileged(new SetAccessibleAction(
                        constructor));
            value2 = constructor
                    .newInstance(new Object[] {((String) value2).trim()});
        }
        catch (IllegalAccessException e) {
            return false;
        }
        catch (InvocationTargetException e) {
            return false;
        }
        catch (InstantiationException e) {
            return false;
        }

        switch (operation) {
            case APPROX :
            case EQUAL :
            case GREATER :
            case LESS : {
                return value1.equals(value2);
            }
        }
        return false;
    }

    /**
     * Map a string for an APPROX (~=) comparison.
     *
     * This implementation removes white spaces. This is the minimum
     * implementation allowed by the OSGi spec.
     *
     * @param input Input string.
     * @return String ready for APPROX comparison.
     */
    private static String approxString(String input) {
        boolean changed = false;
        char[] output = input.toCharArray();
        int cursor = 0;
        for (int i = 0, length = output.length; i < length; i++) {
            char c = output[i];

            if (Character.isWhitespace(c)) {
                changed = true;
                continue;
            }

            output[cursor] = c;
            cursor++;
        }

        return changed ? new String(output, 0, cursor) : input;
    }

    /**
     * Parser class for OSGi filter strings. This class parses the complete
     * filter string and builds a tree of Filter objects rooted at the
     * parent.
     */
    private static class Parser {
        private final String	filterstring;
        private final boolean   ignoreCase;
        private final char[]	filterChars;
        private int				pos;

        Parser(String filterstring, boolean ignoreCase) {
            this.filterstring = filterstring;
            this.ignoreCase = ignoreCase;
            filterChars = filterstring.toCharArray();
            pos = 0;
        }

        FilterImpl parse() throws InvalidSyntaxException {
            FilterImpl filter;
            try {
                filter = parse_filter();
            }
            catch (ArrayIndexOutOfBoundsException e) {
                throw new InvalidSyntaxException("Filter ended abruptly",
                        filterstring);
            }

            if (pos != filterChars.length) {
                throw new InvalidSyntaxException(
                        "Extraneous trailing characters: "
                                + filterstring.substring(pos), filterstring);
            }
            return filter;
        }

        private FilterImpl parse_filter() throws InvalidSyntaxException {
            FilterImpl filter;
            skipWhiteSpace();

            if (filterChars[pos] != '(') {
                throw new InvalidSyntaxException("Missing '(': "
                        + filterstring.substring(pos), filterstring);
            }

            pos++;

            filter = parse_filtercomp();

            skipWhiteSpace();

            if (filterChars[pos] != ')') {
                throw new InvalidSyntaxException("Missing ')': "
                        + filterstring.substring(pos), filterstring);
            }

            pos++;

            skipWhiteSpace();

            return filter;
        }

        private FilterImpl parse_filtercomp() throws InvalidSyntaxException {
            skipWhiteSpace();

            char c = filterChars[pos];

            switch (c) {
                case '&' : {
                    pos++;
                    return parse_and();
                }
                case '|' : {
                    pos++;
                    return parse_or();
                }
                case '!' : {
                    pos++;
                    return parse_not();
                }
            }
            return parse_item();
        }

        private FilterImpl parse_and() throws InvalidSyntaxException {
            int lookahead = pos;
            skipWhiteSpace();

            if (filterChars[pos] != '(') {
                pos = lookahead - 1;
                return parse_item();
            }

            List operands = new ArrayList(10);

            while (filterChars[pos] == '(') {
                FilterImpl child = parse_filter();
                operands.add(child);
            }

            return new FilterImpl(FilterImpl.AND, null, operands
                    .toArray(new FilterImpl[operands.size()]));
        }

        private FilterImpl parse_or() throws InvalidSyntaxException {
            int lookahead = pos;
            skipWhiteSpace();

            if (filterChars[pos] != '(') {
                pos = lookahead - 1;
                return parse_item();
            }

            List operands = new ArrayList(10);

            while (filterChars[pos] == '(') {
                FilterImpl child = parse_filter();
                operands.add(child);
            }

            return new FilterImpl(FilterImpl.OR, null, operands
                    .toArray(new FilterImpl[operands.size()]));
        }

        private FilterImpl parse_not() throws InvalidSyntaxException {
            int lookahead = pos;
            skipWhiteSpace();

            if (filterChars[pos] != '(') {
                pos = lookahead - 1;
                return parse_item();
            }

            FilterImpl child = parse_filter();

            return new FilterImpl(FilterImpl.NOT, null, child);
        }

        private FilterImpl parse_item() throws InvalidSyntaxException {
            String attr = parse_attr();

            skipWhiteSpace();

            switch (filterChars[pos]) {
                case ':': {
                    if (filterChars[pos + 1] == '<' && filterChars[pos + 2] == '*') {
                        pos += 3;
                        return new FilterImpl(FilterImpl.SUBSET, attr,
                                parse_value());
                    }
                    if (filterChars[pos + 1] == '*' && filterChars[pos + 2] == '>') {
                        pos += 3;
                        return new FilterImpl(FilterImpl.SUPERSET, attr,
                                parse_value());
                    }
                }
                case '~' : {
                    if (filterChars[pos + 1] == '=') {
                        pos += 2;
                        return new FilterImpl(FilterImpl.APPROX, attr,
                                parse_value());
                    }
                    break;
                }
                case '>' : {
                    if (filterChars[pos + 1] == '=') {
                        pos += 2;
                        return new FilterImpl(FilterImpl.GREATER, attr,
                                parse_value());
                    }
                    break;
                }
                case '<' : {
                    if (filterChars[pos + 1] == '=') {
                        pos += 2;
                        return new FilterImpl(FilterImpl.LESS, attr,
                                parse_value());
                    }
                    break;
                }
                case '=' : {
                    if (filterChars[pos + 1] == '*') {
                        int oldpos = pos;
                        pos += 2;
                        skipWhiteSpace();
                        if (filterChars[pos] == ')') {
                            return new FilterImpl(FilterImpl.PRESENT, attr,
                                    null);
                        }
                        pos = oldpos;
                    }

                    pos++;
                    Object string = parse_substring();

                    if (string instanceof String) {
                        return new FilterImpl(FilterImpl.EQUAL, attr,
                                string);
                    }
                    return new FilterImpl(FilterImpl.SUBSTRING, attr,
                            string);
                }
            }

            throw new InvalidSyntaxException("Invalid operator: "
                    + filterstring.substring(pos), filterstring);
        }

        private String parse_attr() throws InvalidSyntaxException {
            skipWhiteSpace();

            int begin = pos;
            int end = pos;

            char c = filterChars[pos];

            while (c != '~' && c != '<' && c != '>' && c != '=' && c != '('
                    && c != ')') {

                if (c == ':' && filterChars[pos+1] == '<' && filterChars[pos+2] == '*') {
                    break;
                }
                if (c == ':' && filterChars[pos+1] == '*' && filterChars[pos+2] == '>') {
                    break;
                }
                pos++;

                if (!Character.isWhitespace(c)) {
                    end = pos;
                }

                c = filterChars[pos];
            }

            int length = end - begin;

            if (length == 0) {
                throw new InvalidSyntaxException("Missing attr: "
                        + filterstring.substring(pos), filterstring);
            }

            String str = new String(filterChars, begin, length);
            if (ignoreCase)
            {
                str = str.toLowerCase();
            }
            return str;
        }

        private String parse_value() throws InvalidSyntaxException {
            StringBuffer sb = new StringBuffer(filterChars.length - pos);

            parseloop: while (true) {
                char c = filterChars[pos];

                switch (c) {
                    case ')' : {
                        break parseloop;
                    }

                    case '(' : {
                        throw new InvalidSyntaxException("Invalid value: "
                                + filterstring.substring(pos), filterstring);
                    }

                    case '\\' : {
                        pos++;
                        c = filterChars[pos];
                        /* fall through into default */
                    }

                    default : {
                        sb.append(c);
                        pos++;
                        break;
                    }
                }
            }

            if (sb.length() == 0) {
                throw new InvalidSyntaxException("Missing value: "
                        + filterstring.substring(pos), filterstring);
            }

            return sb.toString();
        }

        private Object parse_substring() throws InvalidSyntaxException {
            StringBuffer sb = new StringBuffer(filterChars.length - pos);

            List operands = new ArrayList(10);

            parseloop: while (true) {
                char c = filterChars[pos];

                switch (c) {
                    case ')' : {
                        if (sb.length() > 0) {
                            operands.add(sb.toString());
                        }

                        break parseloop;
                    }

                    case '(' : {
                        throw new InvalidSyntaxException("Invalid value: "
                                + filterstring.substring(pos), filterstring);
                    }

                    case '*' : {
                        if (sb.length() > 0) {
                            operands.add(sb.toString());
                        }

                        sb.setLength(0);

                        operands.add(null);
                        pos++;

                        break;
                    }

                    case '\\' : {
                        pos++;
                        c = filterChars[pos];
                        /* fall through into default */
                    }

                    default : {
                        sb.append(c);
                        pos++;
                        break;
                    }
                }
            }

            int size = operands.size();

            if (size == 0) {
                return "";
            }

            if (size == 1) {
                Object single = operands.get(0);

                if (single != null) {
                    return single;
                }
            }

            return operands.toArray(new String[size]);
        }

        private void skipWhiteSpace() {
            for (int length = filterChars.length; (pos < length)
                    && Character.isWhitespace(filterChars[pos]);) {
                pos++;
            }
        }
    }

    /**
     * This Dictionary is used for case-insensitive key lookup during filter
     * evaluation. This Dictionary implementation only supports the get
     * operation using a String key as no other operations are used by the
     * Filter implementation.
     */
    private static class CaseInsensitiveDictionary extends Dictionary {
        private final Dictionary	dictionary;
        private final String[]		keys;

        /**
         * Create a case insensitive dictionary from the specified dictionary.
         *
         * @param dictionary
         * @throws IllegalArgumentException If <code>dictionary</code> contains
         *         case variants of the same key name.
         */
        CaseInsensitiveDictionary(Dictionary dictionary) {
            if (dictionary == null) {
                this.dictionary = null;
                this.keys = new String[0];
                return;
            }
            this.dictionary = dictionary;
            List keyList = new ArrayList(dictionary.size());
            for (Enumeration e = dictionary.keys(); e.hasMoreElements();) {
                Object k = e.nextElement();
                if (k instanceof String) {
                    String key = (String) k;
                    for (Iterator i = keyList.iterator(); i.hasNext();) {
                        if (key.equalsIgnoreCase((String) i.next())) {
                            throw new IllegalArgumentException();
                        }
                    }
                    keyList.add(key);
                }
            }
            this.keys = (String[]) keyList.toArray(new String[keyList.size()]);
        }

        public Object get(Object o) {
            String k = (String) o;
            for (int i = 0, length = keys.length; i < length; i++) {
                String key = keys[i];
                if (key.equalsIgnoreCase(k)) {
                    return dictionary.get(key);
                }
            }
            return null;
        }

        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        public Enumeration keys() {
            throw new UnsupportedOperationException();
        }

        public Enumeration elements() {
            throw new UnsupportedOperationException();
        }

        public Object put(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            throw new UnsupportedOperationException();
        }
    }

    private static class SetAccessibleAction implements PrivilegedAction {
        private final AccessibleObject accessible;

        SetAccessibleAction(AccessibleObject accessible) {
            this.accessible = accessible;
        }

        public Object run() {
            accessible.setAccessible(true);
            return null;
        }
    }

    /**
     * This class contains a method to match a distinguished name (DN) chain
     * against and DN chain pattern.
     * <p>
     * The format of DNs are given in RFC 2253. We represent a signature chain
     * for an X.509 certificate as a semicolon separated list of DNs. This is
     * what we refer to as the DN chain. Each DN is made up of relative
     * distinguished names (RDN) which in turn are made up of key value pairs.
     * For example:
     *
     * <pre>
     *   cn=ben+ou=research,o=ACME,c=us;ou=Super CA,c=CA
     * </pre>
     *
     * is made up of two DNs: "<code>cn=ben+ou=research,o=ACME,c=us</code>
     * " and " <code>ou=Super CA,c=CA</code>
     * ". The first DN is made of of three RDNs: "
     * <code>cn=ben+ou=research</code>" and "<code>o=ACME</code>" and "
     * <code>c=us</code>". The first RDN has two name value pairs: "
     * <code>cn=ben</code>" and "<code>ou=research</code>".
     * <p>
     * A chain pattern makes use of wildcards ('*' or '-') to match against DNs,
     * and wildcards ('*') to match againts DN prefixes, and value. If a DN in a
     * match pattern chain is made up of a wildcard ("*"), that wildcard will
     * match zero or one DNs in the chain. If a DN in a match pattern chain is
     * made up of a wildcard ("-"), that wildcard will match zero or more DNs in
     * the chain. If the first RDN of a DN is the wildcard ("*"), that DN will
     * match any other DN with the same suffix (the DN with the wildcard RDN
     * removed). If a value of a name/value pair is a wildcard ("*"), the value
     * will match any value for that name.
     */
    private static class DNChainMatching {
        private static final String	MINUS_WILDCARD	= "-";
        private static final String	STAR_WILDCARD	= "*";

        /**
         * Check the name/value pairs of the rdn against the pattern.
         *
         * @param rdn List of name value pairs for a given RDN.
         * @param rdnPattern List of name value pattern pairs.
         * @return true if the list of name value pairs match the pattern.
         */
        private static boolean rdnmatch(List rdn, List rdnPattern) {
            if (rdn.size() != rdnPattern.size()) {
                return false;
            }
            for (int i = 0; i < rdn.size(); i++) {
                String rdnNameValue = (String) rdn.get(i);
                String patNameValue = (String) rdnPattern.get(i);
                int rdnNameEnd = rdnNameValue.indexOf('=');
                int patNameEnd = patNameValue.indexOf('=');
                if (rdnNameEnd != patNameEnd
                        || !rdnNameValue.regionMatches(0, patNameValue, 0,
                                rdnNameEnd)) {
                    return false;
                }
                String patValue = patNameValue.substring(patNameEnd);
                String rdnValue = rdnNameValue.substring(rdnNameEnd);
                if (!rdnValue.equals(patValue) && !patValue.equals("=*")
                        && !patValue.equals("=#16012a")) {
                    return false;
                }
            }
            return true;
        }

        private static boolean dnmatch(List dn, List dnPattern) {
            int dnStart = 0;
            int patStart = 0;
            int patLen = dnPattern.size();
            if (patLen == 0) {
                return false;
            }
            if (dnPattern.get(0).equals(STAR_WILDCARD)) {
                patStart = 1;
                patLen--;
            }
            if (dn.size() < patLen) {
                return false;
            }
            else {
                if (dn.size() > patLen) {
                    if (!dnPattern.get(0).equals(STAR_WILDCARD)) {
                        // If the number of rdns do not match we must have a
                        // prefix map
                        return false;
                    }
                    // The rdnPattern and rdn must have the same number of
                    // elements
                    dnStart = dn.size() - patLen;
                }
            }
            for (int i = 0; i < patLen; i++) {
                if (!rdnmatch((List) dn.get(i + dnStart), (List) dnPattern
                        .get(i + patStart))) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Parses a distinguished name chain pattern and returns a List where
         * each element represents a distinguished name (DN) in the chain of
         * DNs. Each element will be either a String, if the element represents
         * a wildcard ("*" or "-"), or a List representing an RDN. Each element
         * in the RDN List will be a String, if the element represents a
         * wildcard ("*"), or a List of Strings, each String representing a
         * name/value pair in the RDN.
         *
         * @param dnChain
         * @return a list of DNs.
         * @throws IllegalArgumentException
         */
        private static List parseDNchainPattern(String dnChain) {
            if (dnChain == null) {
                throw new IllegalArgumentException(
                        "The DN chain must not be null.");
            }
            List parsed = new ArrayList();
            int startIndex = 0;
            startIndex = skipSpaces(dnChain, startIndex);
            while (startIndex < dnChain.length()) {
                int endIndex = startIndex;
                boolean inQuote = false;
                out: while (endIndex < dnChain.length()) {
                    char c = dnChain.charAt(endIndex);
                    switch (c) {
                        case '"' :
                            inQuote = !inQuote;
                            break;
                        case '\\' :
                            endIndex++; // skip the escaped char
                            break;
                        case ';' :
                            if (!inQuote)
                                break out;
                    }
                    endIndex++;
                }
                if (endIndex > dnChain.length()) {
                    throw new IllegalArgumentException("unterminated escape");
                }
                parsed.add(dnChain.substring(startIndex, endIndex));
                startIndex = endIndex + 1;
                startIndex = skipSpaces(dnChain, startIndex);
            }
            return parseDNchain(parsed);
        }

        private static List parseDNchain(List chain) {
            if (chain == null) {
                throw new IllegalArgumentException("DN chain must not be null.");
            }
            chain = new ArrayList(chain);
            // Now we parse is a list of strings, lets make List of rdn out
            // of them
            for (int i = 0; i < chain.size(); i++) {
                String dn = (String) chain.get(i);
                if (dn.equals(STAR_WILDCARD) || dn.equals(MINUS_WILDCARD)) {
                    continue;
                }
                List rdns = new ArrayList();
                if (dn.charAt(0) == '*') {
                    if (dn.charAt(1) != ',') {
                        throw new IllegalArgumentException(
                                "invalid wildcard prefix");
                    }
                    rdns.add(STAR_WILDCARD);
                    dn = new X500Principal(dn.substring(2))
                            .getName(X500Principal.CANONICAL);
                }
                else {
                    dn = new X500Principal(dn).getName(X500Principal.CANONICAL);
                }
                // Now dn is a nice CANONICAL DN
                parseDN(dn, rdns);
                chain.set(i, rdns);
            }
            if (chain.size() == 0) {
                throw new IllegalArgumentException("empty DN chain");
            }
            return chain;
        }

        /**
         * Increment startIndex until the end of dnChain is hit or until it is
         * the index of a non-space character.
         */
        private static int skipSpaces(String dnChain, int startIndex) {
            while (startIndex < dnChain.length()
                    && dnChain.charAt(startIndex) == ' ') {
                startIndex++;
            }
            return startIndex;
        }

        /**
         * Takes a distinguished name in canonical form and fills in the
         * rdnArray with the extracted RDNs.
         *
         * @param dn the distinguished name in canonical form.
         * @param rdn the list to fill in with RDNs extracted from the dn
         * @throws IllegalArgumentException if a formatting error is found.
         */
        private static void parseDN(String dn, List rdn) {
            int startIndex = 0;
            char c = '\0';
            List nameValues = new ArrayList();
            while (startIndex < dn.length()) {
                int endIndex;
                for (endIndex = startIndex; endIndex < dn.length(); endIndex++) {
                    c = dn.charAt(endIndex);
                    if (c == ',' || c == '+') {
                        break;
                    }
                    if (c == '\\') {
                        endIndex++; // skip the escaped char
                    }
                }
                if (endIndex > dn.length()) {
                    throw new IllegalArgumentException("unterminated escape "
                            + dn);
                }
                nameValues.add(dn.substring(startIndex, endIndex));
                if (c != '+') {
                    rdn.add(nameValues);
                    if (endIndex != dn.length()) {
                        nameValues = new ArrayList();
                    }
                    else {
                        nameValues = null;
                    }
                }
                startIndex = endIndex + 1;
            }
            if (nameValues != null) {
                throw new IllegalArgumentException("improperly terminated DN "
                        + dn);
            }
        }

        /**
         * This method will return an 'index' which points to a non-wildcard DN
         * or the end-of-list.
         */
        private static int skipWildCards(List dnChainPattern,
                int dnChainPatternIndex) {
            int i;
            for (i = dnChainPatternIndex; i < dnChainPattern.size(); i++) {
                Object dnPattern = dnChainPattern.get(i);
                if (dnPattern instanceof String) {
                    if (!dnPattern.equals(STAR_WILDCARD)
                            && !dnPattern.equals(MINUS_WILDCARD)) {
                        throw new IllegalArgumentException(
                                "expected wildcard in DN pattern");
                    }
                    // otherwise continue skipping over wild cards
                }
                else {
                    if (dnPattern instanceof List) {
                        // if its a list then we have our 'non-wildcard' DN
                        break;
                    }
                    else {
                        // unknown member of the DNChainPattern
                        throw new IllegalArgumentException(
                                "expected String or List in DN Pattern");
                    }
                }
            }
            // i either points to end-of-list, or to the first
            // non-wildcard pattern after dnChainPatternIndex
            return i;
        }

        /**
         * recursively attempt to match the DNChain, and the DNChainPattern
         * where DNChain is of the format: "DN;DN;DN;" and DNChainPattern is of
         * the format: "DNPattern;*;DNPattern" (or combinations of this)
         */
        private static boolean dnChainMatch(List dnChain, int dnChainIndex,
                List dnChainPattern, int dnChainPatternIndex)
                throws IllegalArgumentException {
            if (dnChainIndex >= dnChain.size()) {
                return false;
            }
            if (dnChainPatternIndex >= dnChainPattern.size()) {
                return false;
            }
            // check to see what the pattern starts with
            Object dnPattern = dnChainPattern.get(dnChainPatternIndex);
            if (dnPattern instanceof String) {
                if (!dnPattern.equals(STAR_WILDCARD)
                        && !dnPattern.equals(MINUS_WILDCARD)) {
                    throw new IllegalArgumentException(
                            "expected wildcard in DN pattern");
                }
                // here we are processing a wild card as the first DN
                // skip all wildcard DN's
                if (dnPattern.equals(MINUS_WILDCARD)) {
                    dnChainPatternIndex = skipWildCards(dnChainPattern,
                            dnChainPatternIndex);
                }
                else {
                    dnChainPatternIndex++; // only skip the '*' wildcard
                }
                if (dnChainPatternIndex >= dnChainPattern.size()) {
                    // return true iff the wild card is '-' or if we are at the
                    // end of the chain
                    return dnPattern.equals(MINUS_WILDCARD) ? true : dnChain
                            .size() - 1 == dnChainIndex;
                }
                //
                // we will now recursively call to see if the rest of the
                // DNChainPattern matches increasingly smaller portions of the
                // rest of the DNChain
                //
                if (dnPattern.equals(STAR_WILDCARD)) {
                    // '*' option: only wildcard on 0 or 1
                    return dnChainMatch(dnChain, dnChainIndex, dnChainPattern,
                            dnChainPatternIndex)
                            || dnChainMatch(dnChain, dnChainIndex + 1,
                                    dnChainPattern, dnChainPatternIndex);
                }
                for (int i = dnChainIndex; i < dnChain.size(); i++) {
                    // '-' option: wildcard 0 or more
                    if (dnChainMatch(dnChain, i, dnChainPattern,
                            dnChainPatternIndex)) {
                        return true;
                    }
                }
                // if we are here, then we didn't find a match.. fall through to
                // failure
            }
            else {
                if (dnPattern instanceof List) {
                    // here we have to do a deeper check for each DN in the
                    // pattern until we hit a wild card
                    do {
                        if (!dnmatch((List) dnChain.get(dnChainIndex),
                                (List) dnPattern)) {
                            return false;
                        }
                        // go to the next set of DN's in both chains
                        dnChainIndex++;
                        dnChainPatternIndex++;
                        // if we finished the pattern then it all matched
                        if ((dnChainIndex >= dnChain.size())
                                && (dnChainPatternIndex >= dnChainPattern
                                        .size())) {
                            return true;
                        }
                        // if the DN Chain is finished, but the pattern isn't
                        // finished then if the rest of the pattern is not
                        // wildcard then we are done
                        if (dnChainIndex >= dnChain.size()) {
                            dnChainPatternIndex = skipWildCards(dnChainPattern,
                                    dnChainPatternIndex);
                            // return TRUE iff the pattern index moved past the
                            // list-size (implying that the rest of the pattern
                            // is all wildcards)
                            return dnChainPatternIndex >= dnChainPattern.size();
                        }
                        // if the pattern finished, but the chain continues then
                        // we have a mis-match
                        if (dnChainPatternIndex >= dnChainPattern.size()) {
                            return false;
                        }
                        // get the next DN Pattern
                        dnPattern = dnChainPattern.get(dnChainPatternIndex);
                        if (dnPattern instanceof String) {
                            if (!dnPattern.equals(STAR_WILDCARD)
                                    && !dnPattern.equals(MINUS_WILDCARD)) {
                                throw new IllegalArgumentException(
                                        "expected wildcard in DN pattern");
                            }
                            // if the next DN is a 'wildcard', then we will
                            // recurse
                            return dnChainMatch(dnChain, dnChainIndex,
                                    dnChainPattern, dnChainPatternIndex);
                        }
                        else {
                            if (!(dnPattern instanceof List)) {
                                throw new IllegalArgumentException(
                                        "expected String or List in DN Pattern");
                            }
                        }
                        // if we are here, then we will just continue to the
                        // match the next set of DN's from the DNChain, and the
                        // DNChainPattern since both are lists
                    } while (true);
                    // should never reach here?
                }
                else {
                    throw new IllegalArgumentException(
                            "expected String or List in DN Pattern");
                }
            }
            // if we get here, the the default return is 'mis-match'
            return false;
        }

        /**
         * Matches a distinguished name chain against a pattern of a
         * distinguished name chain.
         *
         * @param dnChain
         * @param pattern the pattern of distinguished name (DN) chains to match
         *        against the dnChain. Wildcards ("*" or "-") can be used in
         *        three cases:
         *        <ol>
         *        <li>As a DN. In this case, the DN will consist of just the "*"
         *        or "-". When "*" is used it will match zero or one DNs. When
         *        "-" is used it will match zero or more DNs. For example,
         *        "cn=me,c=US;*;cn=you" will match
         *        "cn=me,c=US";cn=you" and "cn=me,c=US;cn=her;cn=you". The
         *        pattern "cn=me,c=US;-;cn=you" will match "cn=me,c=US";cn=you"
         *        and "cn=me,c=US;cn=her;cn=him;cn=you".
         *        <li>As a DN prefix. In this case, the DN must start with "*,".
         *        The wild card will match zero or more RDNs at the start of a
         *        DN. For example, "*,cn=me,c=US;cn=you" will match
         *        "cn=me,c=US";cn=you" and
         *        "ou=my org unit,o=my org,cn=me,c=US;cn=you"</li>
         *        <li>As a value. In this case the value of a name value pair in
         *        an RDN will be a "*". The wildcard will match any value for
         *        the given name. For example, "cn=*,c=US;cn=you" will match
         *        "cn=me,c=US";cn=you" and "cn=her,c=US;cn=you", but it will not
         *        match "ou=my org unit,c=US;cn=you". If the wildcard does not
         *        occur by itself in the value, it will not be used as a
         *        wildcard. In other words, "cn=m*,c=US;cn=you" represents the
         *        common name of "m*" not any common name starting with "m".</li>
         *        </ol>
         * @return true if dnChain matches the pattern.
         * @throws IllegalArgumentException
         */
        static boolean match(String pattern, List/* <String> */dnChain) {
            List parsedDNChain;
            List parsedDNPattern;
            try {
                parsedDNChain = parseDNchain(dnChain);
            }
            catch (RuntimeException e) {
                IllegalArgumentException iae = new IllegalArgumentException(
                        "Invalid DN chain: " + toString(dnChain));
                iae.initCause(e);
                throw iae;
            }
            try {
                parsedDNPattern = parseDNchainPattern(pattern);
            }
            catch (RuntimeException e) {
                IllegalArgumentException iae = new IllegalArgumentException(
                        "Invalid match pattern: " + pattern);
                iae.initCause(e);
                throw iae;
            }
            return dnChainMatch(parsedDNChain, 0, parsedDNPattern, 0);
        }

        private static String toString(List dnChain) {
            if (dnChain == null) {
                return null;
            }
            StringBuffer sb = new StringBuffer();
            for (Iterator iChain = dnChain.iterator(); iChain.hasNext();) {
                sb.append(iChain.next());
                if (iChain.hasNext()) {
                    sb.append("; ");
                }
            }
            return sb.toString();
        }
    }

    /**
     * This Dictionary is used for key lookup from a ServiceReference during
     * filter evaluation. This Dictionary implementation only supports the get
     * operation using a String key as no other operations are used by the
     * Filter implementation.
     */
    private static class ServiceReferenceDictionary extends Dictionary {
        private final ServiceReference	reference;

        ServiceReferenceDictionary(ServiceReference reference) {
            this.reference = reference;
        }

        public Object get(Object key) {
            if (reference == null) {
                return null;
            }
            return reference.getProperty((String) key);
        }

        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        public Enumeration keys() {
            throw new UnsupportedOperationException();
        }

        public Enumeration elements() {
            throw new UnsupportedOperationException();
        }

        public Object put(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            throw new UnsupportedOperationException();
        }
    }

}
