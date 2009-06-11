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
package aQute.shell.runtime;

import java.lang.reflect.*;
import java.util.*;

import org.osgi.service.command.*;

public class Reflective {
    public final static Object      NO_MATCH = new Object();
    public final static Set<String> KEYWORDS = new HashSet<String>(Arrays
                                                     .asList(new String[] {
            "abstract", "continue", "for", "new", "switch", "assert",
            "default", "goto", "package", "synchronized", "boolean", "do",
            "if", "private", "this", "break", "double", "implements",
            "protected", "throw", "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient", "catch",
            "extends", "int", "short", "try", "char", "final", "interface",
            "static", "void", "class", "finally", "long", "strictfp",
            "volatile", "const", "float", "native", "super", "while" }));

    public Object method(CommandSession session, Object target, String name,
            List<Object> args) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, Exception {
        Method[] methods = target.getClass().getMethods();
        name = name.toLowerCase();

        String get = "get" + name;
        String is = "is" + name;
        String set = "set" + name;

        if (KEYWORDS.contains(name))
            name = "_" + name;

        Method bestMethod = null;
        Object[] bestArgs = null;
        int match = -1;
        for (Method m : methods) {
            String mname = m.getName().toLowerCase();
            if (mname.equals(name) || mname.equals(get) || mname.equals(set)
                    || mname.equals(is)) {
                Class<?>[] types = m.getParameterTypes();

                // Check if the command takes a session
                if (types.length > 0
                        && CommandSession.class.isAssignableFrom(types[0])) {
                    args.add(0, session);
                }

                Object[] parms = new Object[types.length];
                // if (types.length >= args.size() ) {
                int local = coerce(session, target, types, parms, args);
                if (local == types.length || local > match) {
                    bestMethod = m;
                    bestArgs = parms;
                    match = local;
                }
                // }
                // if (match == -1 && types.length == 1
                // && types[0] == Object[].class) {
                // bestMethod = m;
                // Object value = args.toArray();
                // bestArgs = new Object[] { value };
                // }
            }
        }

        if (bestMethod != null) {
            bestMethod.setAccessible(true);
            return bestMethod.invoke(target, bestArgs);
        } else
            throw new IllegalArgumentException("Cannot find command:" + name
                    + " with args:" + args);
    }

    /**
     * Complex routein to convert the arguments given from the command line to
     * the arguments of the method call. First, an attempt is made to convert
     * each argument. If this fails, a check is made to see if varargs can be
     * applied. This happens when the last method argument is an array.
     * 
     * @param session
     * @param target
     * @param types
     * @param out
     * @param in
     * @return
     * @throws Exception
     */
    private int coerce(CommandSession session, Object target, Class<?> types[],
            Object out[], List<Object> in) throws Exception {
        int i = 0;
        while (i < out.length) {
            out[i] = null;
            try {
                // Try to convert one argument
                out[i] = coerce(session, target, types[i], in.get(i));
                if (out[i] == NO_MATCH) {
                    // Failed
                    // No match, check for varargs
                    if (types[i].isArray() && i == types.length - 1) {
                        // Try to parse the remaining arguments in an array
                        Class<?> component = types[i].getComponentType();
                        Object components = Array.newInstance(component, in
                                .size()
                                - i);
                        int n = i;
                        while (i < in.size()) {
                            Object t = coerce(session, target, component, in
                                    .get(i));
                            if (t == NO_MATCH)
                                return -1;
                            Array.set(components, i - n, t);
                            i++;
                        }
                        out[n] = components;
                        // Is last element, so we will quite hereafter
                        return n;
                    }
                    return -1;
                }
                i++;
            } catch (Exception e) {
                // e.printStackTrace();
                System.err.println(e);
                // should get rid of those exceptions, but requires
                // reg ex matching to see if it throws an exception.
                // dont know what is better
                return -1;
            }
        }
        return i;
    }

    Object coerce(CommandSession session, Object target, Class<?> type,
            Object arg) throws Exception {
        if (arg == null)
            return null;

        if (type.isAssignableFrom(arg.getClass()))
            return arg;

        Object converted = session.convert(type, arg);
        if (converted != null)
            return converted;

        String string = arg.toString();
        if (type.isAssignableFrom(String.class))
            return string;

        if (type.isArray()) {
            // Must handle array types
            return NO_MATCH;
        } else if (!type.isPrimitive()) {
            try {
                return type.getConstructor(String.class).newInstance(string);
            } catch (Exception e) {
                return NO_MATCH;
            }
        }
        if (type == boolean.class)
            return new Boolean(string);
        else if (type == byte.class)
            return new Byte(string);
        else if (type == char.class) {
            if (string.length() == 1)
                return string.charAt(0);
        } else if (type == short.class)
            return new Short(string);
        else if (type == int.class)
            return new Integer(string);
        else if (type == float.class)
            return new Float(string);
        else if (type == double.class)
            return new Double(string);
        else if (type == long.class)
            return new Long(string);

        return NO_MATCH;
    }

    public static boolean hasCommand(Object target, String function) {
        Method[] methods = target.getClass().getMethods();
        for (Method m : methods) {
            if (m.getName().equals(function))
                return true;
        }
        return false;
    }
}
