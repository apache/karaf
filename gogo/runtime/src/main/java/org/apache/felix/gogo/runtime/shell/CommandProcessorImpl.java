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
package org.apache.felix.gogo.runtime.shell;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Converter;
import org.osgi.service.command.Function;
import org.osgi.service.threadio.ThreadIO;

public class CommandProcessorImpl implements CommandProcessor
{
    protected final Set<Converter> converters = new HashSet<Converter>();
    protected final Map<String, Object> commands = new LinkedHashMap<String, Object>();
    protected final ThreadIO threadIO;

    public CommandProcessorImpl(ThreadIO tio)
    {
        threadIO = tio;
        addCommand("shell", this, "addCommand");
        addCommand("shell", this, "removeCommand");
    }

    public CommandSession createSession(InputStream in, PrintStream out, PrintStream err)
    {
        return new CommandSessionImpl(this, in, out, err);
    }

    public void addConverter(Converter c)
    {
        converters.add(c);
    }

    public void removeConverter(Converter c)
    {
        converters.remove(c);
    }
    
    public Set<String> getCommands()
    {
        return commands.keySet();
    }

    public Function getCommand(String name)
    {
        name = name.toLowerCase();
        int n = name.indexOf(':');

        if (n < 0)
        {
            return null;
        }
        
        String scope = name.substring(0, n);
        String function = name.substring(n + 1);
        Object cmd = commands.get(name);
        
        if (null == cmd && scope.equals("*"))
        {
            for (String key : commands.keySet())
            {
                if (key.endsWith(":" + function))
                {
                    cmd = commands.get(key);
                    break;
                }
            }
        }

        if ((null == cmd) || (cmd instanceof Function))
        {
            return (Function) cmd;
        }

        return new CommandProxy(cmd, function);
    }

    public void addCommand(String scope, Object target)
    {
        Class<?> tc = (target instanceof Class<?>) ? (Class<?>) target
            : target.getClass();
        addCommand(scope, target, tc);
    }

    public void addCommand(String scope, Object target, Class<?> functions)
    {
        if (target == null)
        {
            return;
        }

        String[] names = getFunctions(functions);
        for (String function : names)
        {
            addCommand(scope, target, function);
        }
    }

    public void addCommand(String scope, Object target, String function)
    {
        commands.put((scope + ":" + function).toLowerCase(), target);
    }

    public void removeCommand(String scope, String function)
    {
        String func = (scope + ":" + function).toLowerCase();
        commands.remove(func);
    }

    public void removeCommand(Object target)
    {
        for (Iterator<Object> i = commands.values().iterator(); i.hasNext();)
        {
            if (i.next() == target)
            {
                i.remove();
            }
        }
    }

    private String[] getFunctions(Class<?> target)
    {
        String[] functions;
        Set<String> list = new TreeSet<String>();
        Method methods[] = target.getMethods();
        for (Method m : methods)
        {
            if (m.getDeclaringClass().equals(Object.class))
            {
                continue;
            }
            list.add(m.getName());
            if (m.getName().startsWith("get"))
            {
                String s = m.getName().substring(3);
                if (s.length() > 0)
                {
                    list.add(s.substring(0, 1).toLowerCase() + s.substring(1));
                }
            }
        }

        functions = list.toArray(new String[list.size()]);
        return functions;
    }

    protected void put(String name, Object target)
    {
        commands.put(name, target);
    }

    public Object convert(Class<?> desiredType, Object in)
    {
        for (Converter c : converters)
        {
            try
            {
                Object converted = c.convert(desiredType, in);
                if (converted != null)
                {
                    return converted;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }
}
