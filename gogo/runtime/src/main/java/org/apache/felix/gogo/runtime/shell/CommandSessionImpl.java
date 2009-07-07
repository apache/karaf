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
// DWB8: throw IllegatlStateException if session used after closed (as per rfc132)
// DWB9: there is no API to list all variables: https://www.osgi.org/bugzilla/show_bug.cgi?id=49
// DWB10: add SCOPE support: https://www.osgi.org/bugzilla/show_bug.cgi?id=51
package org.apache.felix.gogo.runtime.shell;

import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Converter;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class CommandSessionImpl implements CommandSession, Converter
{
    public static final String VARIABLES = ".variables";
    public static final String COMMANDS = ".commands";

    private static final String COLUMN = "%-20s %s\n";

    InputStream in;
    PrintStream out;
    PrintStream err;
    CommandShellImpl service;
    final Map<Object, Object> variables = new HashMap<Object, Object>();
    private boolean closed;    // derek

    CommandSessionImpl(CommandShellImpl service, InputStream in, PrintStream out, PrintStream err)
    {
        this.service = service;
        this.in = in;
        this.out = out;
        this.err = err;
    }

    public void close()
    {
        this.closed = true;    // derek
    }

    public Object execute(CharSequence commandline) throws Exception
    {
        assert service != null;
        assert service.threadIO != null;

        if (closed)
        {
            throw new IllegalStateException("session is closed");    // derek
        }

        Closure impl = new Closure(this, null, commandline);
        Object result = impl.execute(this, null);
        return result;
    }

    public InputStream getKeyboard()
    {
        return in;
    }

    public Object get(String name)
    {
        // XXX: derek.baum@paremus.com
        // there is no API to list all variables, so overload name == null
        if (name == null || VARIABLES.equals(name))
        {
            return variables.keySet();
        }
        if (COMMANDS.equals(name))
        {
            return service.get(null);
        }

        if (variables.containsKey(name))
        {
            return variables.get(name);
        }

        // XXX: derek: add SCOPE support
        if (name.startsWith("*:"))
        {
            String path = variables.containsKey("SCOPE") ? variables.get("SCOPE").toString() : "osgi:*";
            String func = name.substring(2);
            for (String scope : path.split(":"))
            {
                Object result = service.get(scope + ":" + func);
                if (result != null)
                {
                    return result;
                }
            }
            return null;
        }
        return service.get(name);
    }

    public void put(String name, Object value)
    {
        variables.put(name, value);
    }

    public PrintStream getConsole()
    {
        return out;
    }

    @SuppressWarnings("unchecked")
    public CharSequence format(Object target, int level, Converter escape) throws Exception
    {
        if (target == null)
        {
            return "null";
        }

        if (target instanceof CharSequence)
        {
            return (CharSequence) target;
        }

        for (Converter c : service.converters)
        {
            CharSequence s = c.format(target, level, this);
            if (s != null)
            {
                return s;
            }
        }

        if (target.getClass().isArray())
        {
            if (target.getClass().getComponentType().isPrimitive())
            {
                if (target.getClass().getComponentType() == boolean.class)
                {
                    return Arrays.toString((boolean[]) target);
                }
                else
                {
                    if (target.getClass().getComponentType() == byte.class)
                    {
                        return Arrays.toString((byte[]) target);
                    }
                    else
                    {
                        if (target.getClass().getComponentType() == short.class)
                        {
                            return Arrays.toString((short[]) target);
                        }
                        else
                        {
                            if (target.getClass().getComponentType() == int.class)
                            {
                                return Arrays.toString((int[]) target);
                            }
                            else
                            {
                                if (target.getClass().getComponentType() == long.class)
                                {
                                    return Arrays.toString((long[]) target);
                                }
                                else
                                {
                                    if (target.getClass().getComponentType() == float.class)
                                    {
                                        return Arrays.toString((float[]) target);
                                    }
                                    else
                                    {
                                        if (target.getClass().getComponentType() == double.class)
                                        {
                                            return Arrays.toString((double[]) target);
                                        }
                                        else
                                        {
                                            if (target.getClass().getComponentType() == char.class)
                                            {
                                                return Arrays.toString((char[]) target);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            target = Arrays.asList((Object[]) target);
        }
        if (target instanceof Collection)
        {
            if (level == Converter.INSPECT)
            {
                StringBuilder sb = new StringBuilder();
                Collection<?> c = (Collection<?>) target;
                for (Object o : c)
                {
                    sb.append(format(o, level + 1, this));
                    sb.append("\n");
                }
                return sb;
            }
            else
            {
                if (level == Converter.LINE)
                {
                    StringBuilder sb = new StringBuilder();
                    String del = "[";
                    Collection<?> c = (Collection<?>) target;
                    for (Object o : c)
                    {
                        sb.append(del);
                        sb.append(format(o, level + 1, this));
                        del = ", ";
                    }
                    sb.append("]");
                    return sb;
                }
            }
        }
        if (target instanceof Dictionary)
        {
            Map<Object, Object> result = new HashMap<Object, Object>();
            for (Enumeration e = ((Dictionary) target).keys(); e.hasMoreElements();)
            {
                Object key = e.nextElement();
                result.put(key, ((Dictionary) target).get(key));
            }
            target = result;
        }
        if (target instanceof Map)
        {
            if (level == Converter.INSPECT)
            {
                StringBuilder sb = new StringBuilder();
                Map<?, ?> c = (Map<?, ?>) target;
                for (Map.Entry<?, ?> entry : c.entrySet())
                {
                    CharSequence key = format(entry.getKey(), level + 1, this);
                    sb.append(key);
                    for (int i = key.length(); i < 20; i++)
                    {
                        sb.append(' ');
                    }
                    sb.append(format(entry.getValue(), level + 1, this));
                    sb.append("\n");
                }
                return sb;
            }
            else
            {
                if (level == Converter.LINE)
                {
                    StringBuilder sb = new StringBuilder();
                    String del = "[";
                    Map<?, ?> c = (Map<?, ?>) target;
                    for (Map.Entry<?, ?> entry : c.entrySet())
                    {
                        sb.append(del);
                        sb.append(format(entry, level + 1, this));
                        del = ", ";
                    }
                    sb.append("]");
                    return sb;
                }
            }
        }
        if (level == Converter.INSPECT)
        {
            return inspect(target);
        }
        else
        {
            return target.toString();
        }
    }

    CharSequence inspect(Object b)
    {
        boolean found = false;
        Formatter f = new Formatter();
        Method methods[] = b.getClass().getMethods();
        for (Method m : methods)
        {
            try
            {
                String name = m.getName();
                if (m.getName().startsWith("get") && !m.getName().equals("getClass") && m.getParameterTypes().length == 0 && Modifier.isPublic(m.getModifiers()))
                {
                    found = true;
                    name = name.substring(3);
                    m.setAccessible(true);
                    Object value = m.invoke(b, (Object[]) null);
                    f.format(COLUMN, name, format(value, Converter.LINE, this));
                }
            }
            catch (IllegalAccessException e)
            {
                // Ignore
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        if (found)
        {
            return (StringBuilder) f.out();
        }
        else
        {
            return b.toString();
        }
    }

    public Object convert(Class<?> desiredType, Object in)
    {
        return service.convert(desiredType, in);
    }

    public CharSequence format(Object result, int inspect)
    {
        try
        {
            return format(result, inspect, this);
        }
        catch (Exception e)
        {
            return "<can not format " + result + ":" + e;
        }
    }

}
