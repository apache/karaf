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

import java.io.EOFException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.gogo.runtime.Tokenizer.Type;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;

public class Closure implements Function, Evaluate
{
    public static final String LOCATION = ".location";
    private static final String DEFAULT_LOCK = ".defaultLock";

    private static final long serialVersionUID = 1L;
    private static final ThreadLocal<String> location = new ThreadLocal<String>();

    private final CommandSessionImpl session;
    private final Closure parent;
    private final CharSequence source;
    private final List<List<List<Token>>> program;
    private final Object script;

    private Token errTok;
    private Token errTok2;
    private List<Object> parms = null;
    private List<Object> parmv = null;

    public Closure(CommandSessionImpl session, Closure parent, CharSequence source) throws Exception
    {
        this.session = session;
        this.parent = parent;
        this.source = source;
        script = session.get("0"); // by convention, $0 is script name

        try
        {
            program = new Parser(source).program();
        }
        catch (Exception e)
        {
            throw setLocation(e);
        }
    }

    public CommandSessionImpl session()
    {
        return session;
    }

    private Exception setLocation(Exception e)
    {
        if (session.get(DEFAULT_LOCK) == null)
        {
            String loc = location.get();
            if (null == loc)
            {
                loc = (null == script ? "" : script + ":");

                if (e instanceof SyntaxError)
                {
                    SyntaxError se = (SyntaxError) e;
                    loc += se.line() + "." + se.column();
                }
                else if (null != errTok)
                {
                    loc += errTok.line + "." + errTok.column;
                }

                location.set(loc);
            }
            else if (null != script && !loc.contains(":"))
            {
                location.set(script + ":" + loc);
            }

            session.put(LOCATION, location.get());
        }

        if (e instanceof EOFError)
        { // map to public exception, so interactive clients can provide more input
            EOFException eofe = new EOFException(e.getMessage());
            eofe.initCause(e);
            return eofe;
        }

        return e;
    }

    // implements Function interface
    // XXX: session arg x not used?
    public Object execute(CommandSession x, List<Object> values) throws Exception
    {
        try
        {
            location.remove();
            session.variables.remove(LOCATION);
            return execute(values);
        }
        catch (Exception e)
        {
            throw setLocation(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object execute(List<Object> values) throws Exception
    {
        if (null != values)
        {
            parmv = values;
            parms = new ArgList(parmv);
        }
        else if (null != parent)
        {
            // inherit parent closure parameters
            parms = parent.parms;
            parmv = parent.parmv;
        }
        else
        {
            // inherit session parameters
            Object args = session.get("args");
            if (null != args && args instanceof List<?>)
            {
                parmv = (List<Object>) args;
                parms = new ArgList(parmv);
            }
        }

        Pipe last = null;
        Object[] mark = Pipe.mark();

        for (List<List<Token>> pipeline : program)
        {
            ArrayList<Pipe> pipes = new ArrayList<Pipe>();

            for (List<Token> statement : pipeline)
            {
                Pipe current = new Pipe(this, statement);

                if (pipes.isEmpty())
                {
                    if (current.out == null)
                    {
                        current.setIn(session.in);
                        current.setOut(session.out);
                        current.setErr(session.err);
                    }
                }
                else
                {
                    Pipe previous = pipes.get(pipes.size() - 1);
                    previous.connect(current);
                }
                pipes.add(current);
            }

            if (pipes.size() == 1)
            {
                pipes.get(0).run();
            }
            else if (pipes.size() > 1)
            {
                for (Pipe pipe : pipes)
                {
                    pipe.start();
                }
                for (Pipe pipe : pipes)
                {
                    pipe.join();
                }
            }

            last = pipes.remove(pipes.size() - 1);

            for (Pipe pipe : pipes)
            {
                if (pipe.exception != null)
                {
                    // can't throw exception, as result is defined by last pipe
                    Object oloc = session.get(LOCATION);
                    String loc = (String.valueOf(oloc).contains(":") ? oloc + ": "
                        : "pipe: ");
                    session.err.println(loc + pipe.exception);
                    session.put("pipe-exception", pipe.exception);
                }
            }

            if (last.exception != null)
            {
                Pipe.reset(mark);
                throw last.exception;
            }
        }

        Pipe.reset(mark); // reset IO in case same thread used for new client

        return last == null ? null : last.result;
    }

    public Object eval(final Token t) throws Exception
    {
        Object v = null;

        switch (t.type)
        {
            case WORD:
                v = Tokenizer.expand(t, this);
                
                if (t == v)
                {
                    String s = v.toString();
                    if ("null".equals(s))
                    {
                        v = null;
                    }
                    else if ("false".equals(s))
                    {
                        v = false;
                    }
                    else if ("true".equals(s))
                    {
                        v = true;
                    }
                    else
                    {
                        v = s;
                    }
                }
                else if (v instanceof CharSequence)
                {
                    v = v.toString();
                }
                break;

            case CLOSURE:
                v = new Closure(session, this, t);
                break;

            case EXECUTION:
                v = new Closure(session, this, t).execute(session, parms);
                break;

            case ARRAY:
                v = array(t);
                break;

            case ASSIGN:
                v = t.type;
                break;

            default:
                throw new SyntaxError(t.line, t.column, "unexpected token: " + t.type);
        }

        return v;
    }

    public Object executeStatement(List<Token> statement) throws Exception
    {
        // add set -x facility if echo is set
        if (Boolean.TRUE.equals(session.get("echo")))
        {
            StringBuilder buf = new StringBuilder("+");
            for (Token token : statement)
            {
                buf.append(' ');
                buf.append(token.source());
            }
            session.err.println(buf);
        }

        List<Object> values = new ArrayList<Object>();
        errTok = statement.get(0);

        if ((statement.size() > 3) && Type.ASSIGN.equals(statement.get(1).type))
        {
            errTok2 = statement.get(2);
        }

        for (Token t : statement)
        {
            Object v = eval(t);

            if ((Type.EXECUTION == t.type) && (statement.size() == 1))
            {
                return v;
            }

            if (parms == v && parms != null)
            {
                values.addAll(parms); // explode $args array
            }
            else
            {
                values.add(v);
            }
        }

        Object cmd = values.remove(0);
        if (cmd == null)
        {
            if (values.isEmpty())
            {
                return null;
            }

            throw new RuntimeException("Command name evaluates to null: " + errTok);
        }

        return execute(cmd, values);
    }

    private Object execute(Object cmd, List<Object> values) throws Exception
    {
        // Now there are the following cases
        // <string> '=' statement // complex assignment
        // <string> statement // cmd call
        // <object> // value of <object>
        // <object> statement // method call

        boolean dot = values.size() > 1 && ".".equals(String.valueOf(values.get(0)));

        if (cmd instanceof CharSequence && !dot)
        {
            String scmd = cmd.toString();

            if (values.size() > 0 && Type.ASSIGN.equals(values.get(0)))
            {
                Object value;

                if (values.size() == 1)
                {
                    return session.variables.remove(scmd);
                }

                if (values.size() == 2)
                {
                    value = values.get(1);
                }
                else
                {
                    cmd = values.get(1);
                    if (null == cmd)
                    {
                        throw new RuntimeException("Command name evaluates to null: "
                            + errTok2);
                    }
                    value = execute(cmd, values.subList(2, values.size()));
                }

                return assignment(scmd, value);
            }
            else
            {
                String scopedFunction = scmd;
                Object x = get(scmd);

                if (!(x instanceof Function))
                {
                    if (scmd.indexOf(':') < 0)
                    {
                        scopedFunction = "*:" + scmd;
                    }

                    x = get(scopedFunction);

                    if (x == null || !(x instanceof Function))
                    {
                        // try default command handler
                        if (session.get(DEFAULT_LOCK) == null)
                        {
                            x = get("default");
                            if (x == null)
                            {
                                x = get("*:default");
                            }

                            if (x instanceof Function)
                            {
                                try
                                {
                                    session.put(DEFAULT_LOCK, true);
                                    values.add(0, scmd);
                                    return ((Function) x).execute(session, values);
                                }
                                finally
                                {
                                    session.variables.remove(DEFAULT_LOCK);
                                }
                            }
                        }

                        throw new IllegalArgumentException("Command not found: " + scmd);
                    }
                }
                return ((Function) x).execute(session, values);
            }
        }
        else
        {
            if (values.isEmpty())
            {
                return cmd;
            }
            else if (dot)
            {
                // FELIX-1473 - allow methods calls on String objects
                Object target = cmd;
                ArrayList<Object> args = new ArrayList<Object>();
                values.remove(0);

                for (Object arg : values)
                {
                    if (".".equals(arg))
                    {
                        target = Reflective.method(session, target, args.remove(0).toString(), args);
                        args.clear();
                    }
                    else
                    {
                        args.add(arg);
                    }
                }

                if (args.size() == 0)
                {
                    return target;
                }

                return Reflective.method(session, target, args.remove(0).toString(), args);
            }
            else if (cmd.getClass().isArray() && values.size() == 1)
            {
                Object[] cmdv = (Object[])cmd;
                String index = values.get(0).toString();
                return "length".equals(index) ? cmdv.length : cmdv[Integer.parseInt(index)];
            }
            else
            {
                return Reflective.method(session, cmd, values.remove(0).toString(), values);
            }
        }
    }

    private Object assignment(String name, Object value)
    {
        session.variables.put(name, value);
        return value;
    }

    private Object array(Token array) throws Exception
    {
        List<Token> list = new ArrayList<Token>();
        Map<Token, Token> map = new LinkedHashMap<Token, Token>();
        (new Parser(array)).array(list, map);

        if (map.isEmpty())
        {
            List<Object> olist = new ArrayList<Object>();
            for (Token t : list)
            {
                Object oval = eval(t);
                if (oval.getClass().isArray())
                {
                    for (Object o : (Object[])oval)
                    {
                        olist.add(o);
                    }
                }
                else
                {
                    olist.add(oval);
                }
            }
            return olist;
        }
        else
        {
            Map<Object, Object> omap = new LinkedHashMap<Object, Object>();
            for (Entry<Token, Token> e : map.entrySet())
            {
                Token key = e.getKey();
                Object k = eval(key);
                if (!(k instanceof String))
                {
                    throw new SyntaxError(key.line, key.column,
                        "map key null or not String: " + key);
                }
                omap.put(k, eval(e.getValue()));
            }
            return omap;
        }
    }

    public Object get(String name)
    {
        if (parms != null)
        {
            if ("args".equals(name))
            {
                return parms;
            }

            if ("argv".equals(name))
            {
                return parmv;
            }

            if ("it".equals(name))
            {
                return parms.get(0);
            }

            if (name.length() == 1 && Character.isDigit(name.charAt(0)))
            {
                int i = name.charAt(0) - '0';
                if (i > 0)
                {
                    return parms.get(i - 1);
                }
            }
        }

        return session.get(name);
    }

    public Object put(String key, Object value)
    {
        return session.variables.put(key, value);
    }

    @Override
    public String toString()
    {
        return source.toString().trim().replaceAll("\n+", "\n").replaceAll(
            "([^\\\\{(\\[])\n", "\\1;").replaceAll("[ \\\\\t\n]+", " ");
    }

    /**
     * List that overrides toString() for implicit $args expansion.
     * Also checks for index out of bounds, so that $1 evaluates to null
     * rather than throwing IndexOutOfBoundsException.
     * e.g. x = { a$args }; x 1 2 => a1 2 and not a[1, 2]
     */
    class ArgList extends AbstractList<Object>
    {
        private List<Object> list;

        public ArgList(List<Object> args)
        {
            this.list = args;
        }

        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            for (Object o : list)
            {
                if (buf.length() > 0)
                    buf.append(' ');
                buf.append(o);
            }
            return buf.toString();
        }

        @Override
        public Object get(int index)
        {
            return index < list.size() ? list.get(index) : null;
        }

        @Override
        public Object remove(int index)
        {
            return list.remove(index);
        }

        @Override
        public int size()
        {
            return list.size();
        }
    }

}
