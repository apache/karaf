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
package org.apache.felix.gogo.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.felix.gogo.options.Option;
import org.apache.felix.gogo.options.Options;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;

/**
 * gosh built-in commands.
 */
public class Builtin
{

    static final String[] functions = { "format", "getopt", "new", "set", "tac", "type" };

    private static final String[] packages = { "java.lang", "java.io", "java.net",
            "java.util" };

    public CharSequence format(CommandSession session)
    {
        return format(session, session.get("_"));    // last result
    }
    
    public CharSequence format(CommandSession session, Object arg)
    {
        CharSequence result = session.format(arg, Converter.INSPECT);
        System.out.println(result);
        return result;
    }

    /**
     * script access to Options.
     */
    public Option getopt(List<Object> spec, Object[] args)
    {
        String[] optSpec = new String[spec.size()];
        for (int i = 0; i < optSpec.length; ++i)
        {
            optSpec[i] = spec.get(i).toString();
        }
        return Options.compile(optSpec).parse(args);
    }

    // FIXME: the "new" command should be provided by runtime,
    // so it can leverage same argument coercion mechanism, used to invoke methods.
    public Object _new(Object name, Object[] argv) throws Exception
    {
        Class<?> clazz = null;

        if (name instanceof Class<?>)
        {
            clazz = (Class<?>) name;
        }
        else
        {
            clazz = loadClass(name.toString());
        }

        for (Constructor<?> c : clazz.getConstructors())
        {
            Class<?>[] types = c.getParameterTypes();
            if (types.length != argv.length)
            {
                continue;
            }

            boolean match = true;

            for (int i = 0; i < argv.length; ++i)
            {
                if (!types[i].isAssignableFrom(argv[i].getClass()))
                {
                    if (!types[i].isAssignableFrom(String.class))
                    {
                        match = false;
                        break;
                    }
                    argv[i] = argv[i].toString();
                }
            }

            if (!match)
            {
                continue;
            }

            try
            {
                return c.newInstance(argv);
            }
            catch (InvocationTargetException ite)
            {
                Throwable cause = ite.getCause();
                if (cause instanceof Exception)
                {
                    throw (Exception) cause;
                }
                throw ite;
            }
        }

        throw new IllegalArgumentException("can't coerce " + Arrays.asList(argv)
            + " to any of " + Arrays.asList(clazz.getConstructors()));
    }

    private Class<?> loadClass(String name) throws ClassNotFoundException
    {
        if (!name.contains("."))
        {
            for (String p : packages)
            {
                String pkg = p + "." + name;
                try
                {
                    return Class.forName(pkg);
                }
                catch (ClassNotFoundException e)
                {
                }
            }
        }
        return Class.forName(name);
    }

    public void set(CommandSession session, String[] argv) throws Exception
    {
        final String[] usage = {
                "set - show session variables",
                "Usage: set [OPTIONS] [PREFIX]",
                "  -? --help                show help",
                "  -a --all                 show all variables, including those starting with .",
                "  -x                       set xtrace option",
                "  +x                       unset xtrace option",
                "If PREFIX given, then only show variable(s) starting with PREFIX" };

        Option opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help"))
        {
            opt.usage();
            return;
        }

        List<String> args = opt.args();
        String prefix = (args.isEmpty() ? "" : args.get(0));

        if (opt.isSet("x"))
        {
            session.put("echo", true);
        }
        else if ("+x".equals(prefix))
        {
            session.put("echo", null);
        }
        else
        {
            boolean all = opt.isSet("all");
            for (String key : new TreeSet<String>(Shell.getVariables(session)))
            {
                if (!key.startsWith(prefix))
                    continue;

                if (key.startsWith(".") && !(all || prefix.length() > 0))
                    continue;

                Object target = session.get(key);
                String type = null;
                String value = null;

                if (target != null)
                {
                    Class<? extends Object> clazz = target.getClass();
                    type = clazz.getSimpleName();
                    value = target.toString();
                }

                String trunc = value == null || value.length() < 55 ? "" : "...";
                System.out.println(String.format("%-15.15s %-15s %.45s%s", type, key,
                    value, trunc));
            }
        }
    }

    public Object tac(CommandSession session, String[] argv) throws IOException
    {
        final String[] usage = {
                "tac - capture stdin as String or List and optionally write to file.",
                "Usage: tac [-al] [FILE]", "  -a --append              append to FILE",
                "  -l --list                return List<String>",
                "  -? --help                show help" };

        Option opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help"))
        {
            opt.usage();
            return null;
        }

        List<String> args = opt.args();
        BufferedWriter fw = null;

        if (args.size() == 1)
        {
            String path = args.get(0);
            File file = new File(Shell.cwd(session).resolve(path));
            fw = new BufferedWriter(new FileWriter(file, opt.isSet("append")));
        }

        StringWriter sw = new StringWriter();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));

        ArrayList<String> list = null;

        if (opt.isSet("list"))
        {
            list = new ArrayList<String>();
        }

        boolean first = true;
        String s;

        while ((s = rdr.readLine()) != null)
        {
            if (list != null)
            {
                list.add(s);
            }
            else
            {
                if (!first)
                {
                    sw.write(' ');
                }
                first = false;
                sw.write(s);
            }

            if (fw != null)
            {
                fw.write(s);
                fw.newLine();
            }
        }

        if (fw != null)
        {
            fw.close();
        }

        return list != null ? list : sw.toString();
    }

    // FIXME: expose API in runtime so type command doesn't have to duplicate the runtime
    // command search strategy.
    public boolean type(CommandSession session, String[] argv) throws Exception
    {
        final String[] usage = { "type - show command type",
                "Usage: type [OPTIONS] [name[:]]",
                "  -a --all                 show all matches",
                "  -? --help                show help",
                "  -q --quiet               don't print anything, just return status",
                "  -s --scope=NAME          list all commands in named scope",
                "  -t --types               show full java type names" };

        Option opt = Options.compile(usage).parse(argv);
        List<String> args = opt.args();

        if (opt.isSet("help"))
        {
            opt.usage();
            return true;
        }
        
        boolean all = opt.isSet("all");

        String optScope = null;
        if (opt.isSet("scope"))
        {
            optScope = opt.get("scope");
        }

        if (args.size() == 1)
        {
            String arg = args.get(0);
            if (arg.endsWith(":"))
            {
                optScope = args.remove(0);
            }
        }

        if (optScope != null || (args.isEmpty() && all))
        {
            Set<String> snames = new TreeSet<String>();

            for (String sname : (getCommands(session)))
            {
                if ((optScope == null) || sname.startsWith(optScope))
                {
                    snames.add(sname);
                }
            }

            for (String sname : snames)
            {
                System.out.println(sname);
            }

            return true;
        }

        if (args.size() == 0)
        {
            Map<String, Integer> scopes = new TreeMap<String, Integer>();

            for (String sname : getCommands(session))
            {
                int colon = sname.indexOf(':');
                String scope = sname.substring(0, colon);
                Integer count = scopes.get(scope);
                if (count == null)
                {
                    count = 0;
                }
                scopes.put(scope, ++count);
            }

            for (Entry<String, Integer> entry : scopes.entrySet())
            {
                System.out.println(entry.getKey() + ":" + entry.getValue());
            }

            return true;
        }

        final String name = args.get(0).toLowerCase();

        final int colon = name.indexOf(':');
        final String MAIN = "_main"; // FIXME: must match Reflective.java

        StringBuilder buf = new StringBuilder();
        Set<String> cmds = new LinkedHashSet<String>();

        // get all commands
        if ((colon != -1) || (session.get(name) != null))
        {
            cmds.add(name);
        }
        else if (session.get(MAIN) != null)
        {
            cmds.add(MAIN);
        }
        else
        {
            String path = session.get("SCOPE") != null ? session.get("SCOPE").toString()
                : "*";

            for (String s : path.split(":"))
            {
                if (s.equals("*"))
                {
                    for (String sname : getCommands(session))
                    {
                        if (sname.endsWith(":" + name))
                        {
                            cmds.add(sname);
                            if (!all)
                            {
                                break;
                            }
                        }
                    }
                }
                else
                {
                    String sname = s + ":" + name;
                    if (session.get(sname) != null)
                    {
                        cmds.add(sname);
                        if (!all)
                        {
                            break;
                        }
                    }
                }
            }
        }

        for (String key : cmds)
        {
            Object target = session.get(key);
            if (target == null)
            {
                continue;
            }

            CharSequence source = getClosureSource(session, key);

            if (source != null)
            {
                buf.append(name);
                buf.append(" is function {");
                buf.append(source);
                buf.append("}");
                continue;
            }

            for (Method m : getMethods(session, key))
            {
                StringBuilder params = new StringBuilder();

                for (Class<?> type : m.getParameterTypes())
                {
                    if (params.length() > 0)
                    {
                        params.append(", ");
                    }
                    params.append(type.getSimpleName());
                }

                String rtype = m.getReturnType().getSimpleName();

                if (buf.length() > 0)
                {
                    buf.append("\n");
                }

                if (opt.isSet("types"))
                {
                    String cname = m.getDeclaringClass().getName();
                    buf.append(String.format("%s %s.%s(%s)", rtype, cname, m.getName(),
                        params));
                }
                else
                {
                    buf.append(String.format("%s is %s %s(%s)", name, rtype, key, params));
                }
            }
        }

        if (buf.length() > 0)
        {
            if (!opt.isSet("quiet"))
            {
                System.out.println(buf);
            }
            return true;
        }

        if (!opt.isSet("quiet"))
        {
            System.err.println("type: " + name + " not found.");
        }

        return false;
    }

    /*
     * the following methods depend on the internals of the runtime implementation.
     * ideally, they should be available via some API.
     */

    @SuppressWarnings("unchecked")
    static Set<String> getCommands(CommandSession session)
    {
        return (Set<String>) session.get(".commands");
    }

    private boolean isClosure(Object target)
    {
        return target.getClass().getSimpleName().equals("Closure");
    }

    private boolean isCommand(Object target)
    {
        return target.getClass().getSimpleName().equals("CommandProxy");
    }

    private CharSequence getClosureSource(CommandSession session, String name)
        throws Exception
    {
        Object target = session.get(name);

        if (target == null)
        {
            return null;
        }

        if (!isClosure(target))
        {
            return null;
        }

        Field sourceField = target.getClass().getDeclaredField("source");
        sourceField.setAccessible(true);
        return (CharSequence) sourceField.get(target);
    }

    private List<Method> getMethods(CommandSession session, String scmd) throws Exception
    {
        final int colon = scmd.indexOf(':');
        final String function = colon == -1 ? scmd : scmd.substring(colon + 1);
        final String name = KEYWORDS.contains(function) ? ("_" + function) : function;
        final String get = "get" + function;
        final String is = "is" + function;
        final String set = "set" + function;
        final String MAIN = "_main"; // FIXME: must match Reflective.java

        Object target = session.get(scmd);
        if (target == null)
        {
            return null;
        }

        if (isClosure(target))
        {
            return null;
        }

        if (isCommand(target))
        {
            Method method = target.getClass().getMethod("getTarget", (Class[])null);
            method.setAccessible(true);
            target = method.invoke(target, (Object[])null);
        }

        ArrayList<Method> list = new ArrayList<Method>();
        Class<?> tc = (target instanceof Class<?>) ? (Class<?>) target
            : target.getClass();
        Method[] methods = tc.getMethods();

        for (Method m : methods)
        {
            String mname = m.getName().toLowerCase();

            if (mname.equals(name) || mname.equals(get) || mname.equals(set)
                || mname.equals(is) || mname.equals(MAIN))
            {
                list.add(m);
            }
        }

        return list;
    }

    private final static Set<String> KEYWORDS = new HashSet<String>(
        Arrays.asList(new String[] { "abstract", "continue", "for", "new", "switch",
                "assert", "default", "goto", "package", "synchronized", "boolean", "do",
                "if", "private", "this", "break", "double", "implements", "protected",
                "throw", "byte", "else", "import", "public", "throws", "case", "enum",
                "instanceof", "return", "transient", "catch", "extends", "int", "short",
                "try", "char", "final", "interface", "static", "void", "class",
                "finally", "long", "strictfp", "volatile", "const", "float", "native",
                "super", "while" }));

}
