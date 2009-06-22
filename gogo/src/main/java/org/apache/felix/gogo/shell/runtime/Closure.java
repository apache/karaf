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
// DWB5: session.err is not redirected when creating pipeline
// DWB6: add 'set -x' trace feature if echo is set
// DWB7: removing variable via 'execute("name=") throws OutOfBoundsException
package org.apache.felix.gogo.shell.runtime;

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
                current.setIn(session.in);
                current.setOut(session.out);
                current.setErr(session.err);    // XXX: derek.baum@paremus.com
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

        Pipe last = pipes.get(pipes.size() - 1);
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
        Object result;
        List<Object> values = new ArrayList<Object>();
        CharSequence statement0 = statement.remove(0);

        // derek: FEATURE: add set -x facility if echo is set
        StringBuilder buf = new StringBuilder("+ ");
        buf.append(statement0);

        Object cmd = eval(statement0);
        for (CharSequence token : statement)
        {
            buf.append(' ');
            buf.append(token);
            values.add(eval(token));
        }

        if (Boolean.TRUE.equals(session.get("echo")))
        {
            System.err.println(buf);
        }

        result = execute(cmd, values);
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
                //if (values.size() == 0)
                if (values.size() == 1)            // derek: BUGFIX
                {
                    return session.variables.remove(scmd);
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
                        if (values.isEmpty())
                        {
                            return scmd;
                        }
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

    private Object assignment(Object name, Object value)
    {
        session.variables.put(name, value);
        return value;
    }

    private Object eval(CharSequence seq) throws Exception
    {
        int end = seq.length();
        switch (seq.charAt(0))
        {
            case '$':
                return var(seq);
            case '<':
                Closure c = new Closure(session, this, seq.subSequence(1, end - 1));
                return c.execute(session, parms);
            case '[':
                return array(seq.subSequence(1, end - 1));

            case '{':
                return new Closure(session, this, seq.subSequence(1, end - 1));

            default:
                String result = new Parser(seq).unescape();
                if ("null".equals(result))
                {
                    return null;
                }
                if ("true".equalsIgnoreCase(result))
                {
                    return true;
                }
                if ("false".equalsIgnoreCase(result))
                {
                    return false;
                }
                return seq;
        }
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
        String name = eval(var.subSequence(1, var.length())).toString();
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
