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
// DWB11: add removeCommand: https://www.osgi.org/bugzilla/show_bug.cgi?id=49
// DWB12: there is no API to list commands: https://www.osgi.org/bugzilla/show_bug.cgi?id=49
// DWB13: addCommand() fails to add static methods (if target is Class)
package org.apache.felix.gogo.runtime.shell;

import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Converter;
import org.osgi.service.command.Function;
import org.osgi.service.threadio.ThreadIO;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.*;

public class CommandShellImpl implements CommandProcessor
{
    Set<Converter> converters = new HashSet<Converter>();
    protected ThreadIO threadIO;
    public final static Object NO_SUCH_COMMAND = new Object();
    Map<String, Object> commands = new LinkedHashMap<String, Object>();

    public CommandShellImpl()
    {
        addCommand("shell", this, "addCommand");
        addCommand("shell", this, "removeCommand");    // derek
    }

    public CommandSession createSession(InputStream in, PrintStream out, PrintStream err)
    {

        return new CommandSessionImpl(this, in, out, err);
    }

    public void setThreadio(ThreadIO threadIO)
    {
        this.threadIO = threadIO;
    }

    public void setConverter(Converter c)
    {
        converters.add(c);
    }

    public void unsetConverter(Converter c)
    {
        converters.remove(c);
    }

    public Object get(String name)
    {
        if (name == null)
        {
            return commands.keySet();
        }
        name = name.toLowerCase();
        int n = name.indexOf(':');
        if (n < 0)
        {
            return null;
        }

        String function = name.substring(n);

        Object cmd = null;

        if (commands.containsKey(name))
        {
            cmd = commands.get(name);
        }
        else
        {
            String scope = name.substring(0, n);
            if (scope.equals("*"))
            {
                for (Map.Entry<String, Object> entry : commands.entrySet())
                {
                    if (entry.getKey().endsWith(function))
                    {
                        cmd = entry.getValue();
                        break;
                    }
                }
            }

            // XXX: derek.baum@paremus.com
            // there is no API to list commands
            // so override otherwise illegal name ":"
            if (cmd == null && name.equals(":"))
            {
                return Collections.unmodifiableSet(commands.keySet());
            }
        }

        if (cmd == null)
        {
            return null;
        }

        if (cmd instanceof Function)
        {
            return cmd;
        }
        else
        {
            return new Command(cmd, function.substring(1));
        }
    }

    public void addCommand(String scope, Object target)
    {
        // derek - fix target class
        Class<?> tc = (target instanceof Class) ? (Class<?>) target : target.getClass();
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

    // derek.baum@paremus.com: need removeCommand, so stopped bundles can clean up.
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
            if (m.getDeclaringClass().equals(Object.class))    // derek
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
