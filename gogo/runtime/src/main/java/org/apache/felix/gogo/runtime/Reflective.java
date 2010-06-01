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
package org.apache.felix.gogo.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Parameter;

public final class Reflective
{
    public final static Object NO_MATCH = new Object();
    public final static String MAIN = "_main";
    public final static Set<String> KEYWORDS = new HashSet<String>(
        Arrays.asList(new String[] { "abstract", "continue", "for", "new", "switch",
                "assert", "default", "goto", "package", "synchronized", "boolean", "do",
                "if", "private", "this", "break", "double", "implements", "protected",
                "throw", "byte", "else", "import", "public", "throws", "case", "enum",
                "instanceof", "return", "transient", "catch", "extends", "int", "short",
                "try", "char", "final", "interface", "static", "void", "class",
                "finally", "long", "strictfp", "volatile", "const", "float", "native",
                "super", "while" }));

    public static Object method(CommandSession session, Object target, String name,
        List<Object> args) throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException, Exception
    {
        Method[] methods = target.getClass().getMethods();
        name = name.toLowerCase();

        String get = "get" + name;
        String is = "is" + name;
        String set = "set" + name;

        if (KEYWORDS.contains(name))
        {
            name = "_" + name;
        }

        if (target instanceof Class<?>)
        {
            Method[] staticMethods = ((Class<?>) target).getMethods();
            for (Method m : staticMethods)
            {
                String mname = m.getName().toLowerCase();
                if (mname.equals(name) || mname.equals(get) || mname.equals(set)
                    || mname.equals(is) || mname.equals(MAIN))
                {
                    methods = staticMethods;
                    break;
                }
            }
        }

        Method bestMethod = null;
        Object[] bestArgs = null;
        int match = -1;
        ArrayList<Class<?>[]> possibleTypes = new ArrayList<Class<?>[]>();

        for (Method m : methods)
        {
            String mname = m.getName().toLowerCase();
            if (mname.equals(name) || mname.equals(get) || mname.equals(set)
                || mname.equals(is) || mname.equals(MAIN))
            {
                Class<?>[] types = m.getParameterTypes();
                ArrayList<Object> xargs = new ArrayList<Object>(args);

                // pass command name as argv[0] to main, so it can handle
                // multiple commands
                if (mname.equals(MAIN))
                {
                    xargs.add(0, name);
                }

                // Check if the command takes a session
                if ((types.length > 0) && types[0].isInterface()
                    && types[0].isAssignableFrom(session.getClass()))
                {
                    xargs.add(0, session);
                }

                Object[] parms = new Object[types.length];
                int local = coerce(session, target, m, types, parms, xargs);
                if ((local >= xargs.size()) && (local >= types.length))
                {
                    boolean exact = ((local == xargs.size()) && (local == types.length));
                    if (exact || (local > match))
                    {
                        bestMethod = m;
                        bestArgs = parms;
                        match = local;
                    }
                    if (exact)
                    {
                        break;
                    }
                }
                else
                {
                    possibleTypes.add(types);
                }
            }
        }

        if (bestMethod != null)
        {
            bestMethod.setAccessible(true);
            try
            {
                return bestMethod.invoke(target, bestArgs);
            }
            catch (InvocationTargetException e)
            {
                Throwable cause = e.getCause();
                if (cause instanceof Exception)
                {
                    throw (Exception) cause;
                }
                throw e;
            }
        }
        else
        {
            ArrayList<String> list = new ArrayList<String>();
            for (Class<?>[] types : possibleTypes)
            {
                StringBuilder buf = new StringBuilder();
                buf.append('(');
                for (Class<?> type : types)
                {
                    if (buf.length() > 1)
                    {
                        buf.append(", ");
                    }
                    buf.append(type.getSimpleName());
                }
                buf.append(')');
                list.add(buf.toString());
            }

            StringBuilder params = new StringBuilder();
            for (Object arg : args)
            {
                if (params.length() > 1)
                {
                    params.append(", ");
                }
                params.append(arg.getClass().getSimpleName());
            }

            throw new IllegalArgumentException(String.format(
                "Cannot coerce %s(%s) to any of %s", name, params, list));
        }
    }

    /**
     * Complex routein to convert the arguments given from the command line to
     * the arguments of the method call. First, an attempt is made to convert
     * each argument. If this fails, a check is made to see if varargs can be
     * applied. This happens when the last method argument is an array.
     *
     * @param session
     * @param target
     * @param m
     * @param types
     * @param out
     * @param in
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private static int coerce(CommandSession session, Object target, Method m,
        Class<?> types[], Object out[], List<Object> in) throws Exception
    {
        Annotation[][] pas = m.getParameterAnnotations();

        int start = 0;
        for (int argIndex = 0; argIndex < pas.length; argIndex++)
        {
            Annotation as[] = pas[argIndex];
            for (int a = 0; a < as.length; a++)
            {
                if (as[a] instanceof Parameter)
                {
                    Parameter o = (Parameter) as[a];
                    out[argIndex] = coerce(session, target, types[argIndex],
                        o.absentValue());
                    start = argIndex + 1;
                }
            }
        }

        in = new ArrayList(in);
        for (Iterator<Object> itArgs = in.iterator(); itArgs.hasNext();)
        {
            Object item = itArgs.next();
            if (item instanceof String)
            {
                if (((String) item).startsWith("-"))
                {
                    for (int argIndex = 0; argIndex < pas.length; argIndex++)
                    {
                        Annotation as[] = pas[argIndex];
                        for (int a = 0; a < as.length; a++)
                        {
                            if (as[a] instanceof Parameter)
                            {
                                Parameter param = (Parameter) as[a];
                                for (String name : param.names())
                                {
                                    if (name.equals(item))
                                    {
                                        if (param.presentValue() == null)
                                        {
                                            itArgs.remove();
                                            assert itArgs.hasNext();
                                            Object value = itArgs.next();
                                            itArgs.remove();
                                            out[argIndex] = coerce(session, target,
                                                types[argIndex], value);
                                        }
                                        else if (param.presentValue() != null)
                                        {
                                            itArgs.remove();
                                            out[argIndex] = coerce(session, target,
                                                types[argIndex], param.presentValue());
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        int i = start;
        while (i < out.length)
        {
            out[i] = null;
            try
            {
                // Try to convert one argument
                if (in.size() == 0)
                {
                    out[i] = NO_MATCH;
                }
                else
                {
                    out[i] = coerce(session, target, types[i], in.get(0));
                    if (out[i] != NO_MATCH)
                    {
                        in.remove(0);
                    }
                }

                if (out[i] == NO_MATCH)
                {
                    // Failed
                    // No match, check for varargs
                    if (types[i].isArray() && (i == types.length - 1))
                    {
                        // Try to parse the remaining arguments in an array
                        Class<?> component = types[i].getComponentType();
                        Object components = Array.newInstance(component, in.size());
                        int n = i;
                        while (in.size() > 0)
                        {
                            Object t = coerce(session, target, component, in.remove(0));
                            if (t == NO_MATCH)
                            {
                                return -1;
                            }
                            Array.set(components, i - n, t);
                            i++;
                        }
                        out[n] = components;
                        // Is last element, so we will quite hereafter
                        // return n;
                        if (in.size() == 0)
                        {
                            ++i;
                        }
                        return i; // derek - return number of args converted
                    }
                    return -1;
                }
                i++;
            }
            catch (Exception e)
            {
                System.err.println("Reflective:" + e);
                e.printStackTrace();

                // should get rid of those exceptions, but requires
                // reg ex matching to see if it throws an exception.
                // dont know what is better
                return -1;
            }
        }
        return i;
    }

    private static Object coerce(CommandSession session, Object target, Class<?> type,
        Object arg) throws Exception
    {
        if (arg == null)
        {
            return null;
        }

        if (type.isAssignableFrom(arg.getClass()))
        {
            return arg;
        }

        Object converted = session.convert(type, arg);
        if (converted != null)
        {
            return converted;
        }

        String string = arg.toString();
        if (type.isAssignableFrom(String.class))
        {
            return string;
        }

        if (type.isArray())
        {
            // Must handle array types
            return NO_MATCH;
        }
        else
        {
            if (!type.isPrimitive())
            {
                try
                {
                    return type.getConstructor(String.class).newInstance(string);
                }
                catch (Exception e)
                {
                    return NO_MATCH;
                }
            }
        }

        try
        {
            if (type == boolean.class)
            {
                return new Boolean(string);
            }
            else
            {
                if (type == byte.class)
                {
                    return new Byte(string);
                }
                else
                {
                    if (type == char.class)
                    {
                        if (string.length() == 1)
                        {
                            return string.charAt(0);
                        }
                    }
                    else
                    {
                        if (type == short.class)
                        {
                            return new Short(string);
                        }
                        else
                        {
                            if (type == int.class)
                            {
                                return new Integer(string);
                            }
                            else
                            {
                                if (type == float.class)
                                {
                                    return new Float(string);
                                }
                                else
                                {
                                    if (type == double.class)
                                    {
                                        return new Double(string);
                                    }
                                    else
                                    {
                                        if (type == long.class)
                                        {
                                            return new Long(string);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (NumberFormatException e)
        {
        }

        return NO_MATCH;
    }

}