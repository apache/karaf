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
package org.apache.felix.gogo.console.stdio;

import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Converter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class Console implements Runnable
{
    StringBuilder sb;
    CommandSession session;
    List<CharSequence> history = new ArrayList<CharSequence>();
    int current = 0;
    boolean quit;

    public void setSession(CommandSession session)
    {
        this.session = session;
    }

    public void run()
    {
        try
        {
            while (!quit)
            {
                try
                {
                    CharSequence line = getLine(session.getKeyboard());
                    if (line != null)
                    {
                        history.add(line);
                        if (history.size() > 40)
                        {
                            history.remove(0);
                        }
                        Object result = session.execute(line);
                        if (result != null)
                        {
                            session.getConsole().println(session.format(result, Converter.INSPECT));
                        }
                    }
                    else
                    {
                        quit = true;
                    }

                }
                catch (InvocationTargetException ite)
                {
                    session.getConsole().println("E: " + ite.getTargetException());
                    session.put("exception", ite.getTargetException());
                }
                catch (Throwable e)
                {
                    if (!quit)
                    {
                        session.getConsole().println("E: " + e.getMessage());
                        session.put("exception", e);
                    }
                }
            }
        }
        catch (Exception e)
        {
            if (!quit)
            {
                e.printStackTrace();
            }
        }
    }

    CharSequence getLine(InputStream in) throws IOException
    {
        sb = new StringBuilder();
        session.getConsole().print("$ ");
        int outer = 0;
        while (!quit)
        {
            session.getConsole().flush();
            int c = in.read();
            if (c < 0)
            {
                quit = true;
            }
            else
            {
                switch (c)
                {
                    case '\r':
                        break;
                    case '\n':
                        if (outer == 0 && sb.length() > 0)
                        {
                            return sb;
                        }
                        else
                        {
                            session.getConsole().print("$ ");
                        }
                        break;

                    case '\u001b':
                        c = in.read();
                        if (c == '[')
                        {
                            c = in.read();
                            session.getConsole().print("\b\b\b");
                            switch (c)
                            {
                                case 'A':
                                    history(current - 1);
                                    break;
                                case 'B':
                                    history(current + 1);
                                    break;
                                case 'C': // right(); break;
                                case 'D': // left(); break;
                            }
                        }
                        break;

                    case '\b':
                        if (sb.length() > 0)
                        {
                            session.getConsole().print("\b \b");
                            sb.deleteCharAt(sb.length() - 1);
                        }
                        break;

                    default:
                        sb.append((char) c);
                        break;
                }
            }
        }
        return null;
    }

    void history(int n)
    {
        if (n < 0 || n > history.size())
        {
            return;
        }
        current = n;
        for (int i = 0; i < sb.length(); i++)
        {
            session.getConsole().print("\b \b");
        }

        sb = new StringBuilder(history.get(current));
        session.getConsole().print(sb);
    }

    public void close()
    {
        quit = true;
    }

    public void open()
    {
    }
}
