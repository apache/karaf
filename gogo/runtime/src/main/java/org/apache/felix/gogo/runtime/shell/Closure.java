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

import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Function;

import java.util.*;

public class Closure extends Reflective implements Function
{
    private static final long serialVersionUID = 1L;
    final CharSequence source;
    final Closure parent;
    CommandSessionImpl session;
    List<Object> parms;

    Closure(CommandSessionImpl session, Closure parent, CharSequence source)
    {
        this.session = session;
        this.parent = parent;
        this.source = source;
    }

    public Object execute(CommandSession x, List<Object> values) throws Exception
    {
        parms = values;
        Parser parser = new Parser(source);
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        List<List<List<CharSequence>>> program = parser.program();

        for (List<List<CharSequence>> statements : program)
        {
            Pipe current = new Pipe(this, statements);

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
        if (pipes.size() == 0)
        {
            return null;
        }

        if (pipes.size() == 1)
        {
            pipes.get(0).run();
        }
        else
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

        Pipe last = pipes.remove(pipes.size() - 1);

        for (Pipe pipe : pipes)
        {
            if (pipe.exception != null)
            {
                // can't throw exception, as result is defined by last pipe
                session.err.println("pipe: " + pipe.exception);
            }
        }

        if (last.exception != null)
        {
            throw last.exception;
        }

        if (last.result instanceof Object[])
        {
            return Arrays.asList((Object[]) last.result);
        }
        return last.result;
    }

    Object executeStatement(List<CharSequence> statement) throws Exception
    {
        // add set -x facility if echo is set
        if (Boolean.TRUE.equals(session.get("echo"))) {
            StringBuilder buf = new StringBuilder("+");
            for (CharSequence token : statement)
            {
		buf.append(' ');
                buf.append(token);
            }
            session.err.println(buf);
        }

        if (statement.size() == 1 && statement.get(0).charAt(0) == '(')
        {
            return eval(statement.get(0));
        }

        Object result;
        List<Object> values = new ArrayList<Object>();
        for (CharSequence token : statement)
        {
            Object v = eval(token);
            if (v != null && v == parms)
            {
                for (Object p : parms)
                {
                    values.add(p);
                }
            }
            else {
                values.add(v);
            }
        }
        result = execute(values.remove(0), values);
        return result;
    }

    private Object execute(Object cmd, List<Object> values) throws Exception
    {
        if (cmd == null)
        {
            if (values.isEmpty())
            {
                return null;
            }
            else
            {
                throw new IllegalArgumentException("Command name evaluates to null");
            }
        }

        // Now there are the following cases
        // <string> '=' statement // complex assignment
        // <string> statement // cmd call
        // <object> // value of <object>
        // <object> statement // method call

        if (cmd instanceof CharSequence)
        {
            String scmd = cmd.toString();

            if (values.size() > 0 && "=".equals(values.get(0)))
            {
                if (values.size() == 1)
                {
                    return session.variables.remove(scmd);
                }
                else if (values.size() == 2)
                {
                    Object value = values.get(1);
                    if (value instanceof CharSequence)
                    {
                        value = eval((CharSequence) value);
                    }
                    return assignment(scmd, value);
                }
                else
                {
                    Object value = execute(values.get(1), values.subList(2, values.size()));
                    return assignment(scmd, value);
                }
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
                        throw new IllegalArgumentException("Command not found:  " + scopedFunction);
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
            else
            {
                return method(session, cmd, values.remove(0).toString(), values);
            }
        }
    }

    private Object assignment(String name, Object value)
    {
        session.variables.put(name, value);
        return value;
    }

    private Object eval(CharSequence seq) throws Exception
    {
        Object res = null;
        StringBuilder sb = null;
        Parser p = new Parser(seq);
        int start = p.current;
        while (!p.eof())
        {
            char c = p.peek();
            if (!p.escaped)
            {
                if (c == '$' || c == '(' || c == '\'' || c == '"' || c == '[' || c == '{')
                {
                    if (start != p.current || res != null)
                    {
                        if (sb == null)
                        {
                            sb = new StringBuilder();
                        }
                        if (res != null)
                        {
                            if (res == parms)
                            {
                                for (int i = 0; i < parms.size(); i++)
                                {
                                    if (i > 0)
                                    {
                                        sb.append(' ');
                                    }
                                    sb.append(parms.get(i));
                                }
                            }
                            else
                            {
                                sb.append(res);
                            }
                            res = null;
                        }
                        if (start != p.current)
                        {
                            sb.append(new Parser(p.text.subSequence(start, p.current)).unescape());
                            start = p.current;
                            continue;
                        }
                    }
                    switch (c)
                    {
                        case '\'':
                            p.next();
                            p.quote(c);
                            res = new Parser(p.text.subSequence(start + 1, p.current - 1)).unescape();
                            start = p.current;
                            continue;
                        case '\"':
                            p.next();
                            p.quote(c);
                            res = eval(p.text.subSequence(start + 1, p.current - 1));
                            start = p.current;
                            continue;
                        case '[':
                            p.next();
                            res = array(seq.subSequence(start + 1, p.find(']', '[') - 1));
                            start = p.current;
                            continue;
                        case '(':
                            p.next();
                            Closure cl = new Closure(session, this, p.text.subSequence(start + 1, p.find(')', '(') - 1));
                            res = cl.execute(session, parms);
                            start = p.current;
                            continue;
                        case '{':
                            p.next();
                            res = new Closure(session, this, p.text.subSequence(start + 1, p.find('}', '{') - 1));
                            start = p.current;
                            continue;
                        case '$':
                            p.next();
                            res = var(p.findVar());
                            start = p.current;
                            continue;
                    }
                }
            }
            p.next();
        }
        if (start != p.current)
        {
            if (sb == null)
            {
                sb = new StringBuilder();
            }
            if (res != null)
            {
                if (res == parms)
                {
                    for (Object v : parms)
                    {
                        sb.append(v);
                    }
                }
                else
                {
                    sb.append(res);
                }
                res = null;
            }
            sb.append(new Parser(p.text.subSequence(start, p.current)).unescape());
        }
        if (sb != null)
        {
            if (res != null)
            {
                if (res == parms)
                {
                    for (int i = 0; i < parms.size(); i++)
                    {
                        if (i > 0)
                        {
                            sb.append(' ');
                        }
                        sb.append(parms.get(i));
                    }
                }
                else
                {
                    sb.append(res);
                }
            }
            res = sb;
        }
        if (res instanceof CharSequence)
        {
            String r = res.toString();
            if ("null".equals(r))
            {
                return null;
            }
            else if ("false".equals(r))
            {
                return false;
            }
            else if ("true".equals(r))
            {
                return true;
            }
            return r;
        }

        return res;
    }

    private Object array(CharSequence array) throws Exception
    {
        List<Object> list = new ArrayList<Object>();
        Map<Object, Object> map = new LinkedHashMap<Object, Object>();
        Parser p = new Parser(array);

        while (!p.eof())
        {
            CharSequence token = p.value();

            p.ws();
            if (p.peek() == '=')
            {
                p.next();
                p.ws();
                if (!p.eof())
                {
                    CharSequence value = p.messy();
                    map.put(eval(token), eval(value));
                }
            }
            else
            {
                list.add(eval(token));
            }

            if (p.peek() == ',')
            {
                p.next();
            }
            p.ws();
        }
        p.ws();
        if (!p.eof())
        {
            throw new IllegalArgumentException("Invalid array syntax: " + array);
        }

        if (map.size() != 0 && list.size() != 0)
        {
            throw new IllegalArgumentException("You can not mix maps and arrays: " + array);
        }

        if (map.size() > 0)
        {
            return map;
        }
        else
        {
            return list;
        }
    }

    private Object var(CharSequence var) throws Exception
    {
        if (var.charAt(0) == '{')
        {
            var = var.subSequence(1, var.length() - 1);
        }
        Object v = eval(var);
        String name = v.toString();
        return get(name);
    }

    /**
     * @param name
     * @return
     */
    private Object get(String name)
    {
        if (parms != null)
        {
            if ("it".equals(name))
            {
                return parms.get(0);
            }
            if ("args".equals(name))
            {
                return parms;
            }

            if (name.length() == 1 && Character.isDigit(name.charAt(0)))
            {
                return parms.get(name.charAt(0) - '0');
            }
        }
        return session.get(name);
    }
}
