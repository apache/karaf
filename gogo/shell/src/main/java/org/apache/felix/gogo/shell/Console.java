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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;

public class Console implements Runnable
{
    private final CommandSession session;
    private final InputStream in;
    private final PrintStream out;
    private boolean quit;

    public Console(CommandSession session)
    {
        this.session = session;
        in = session.getKeyboard();
        out = session.getConsole();
    }

    public void run()
    {
        try
        {
            while (!quit)
            {
                try
                {
                    Object prompt = session.get("prompt");
                    if (prompt == null)
                    {
                        prompt = "g! ";
                    }

                    CharSequence line = getLine(prompt.toString());

                    if (line == null)
                    {
                        break;
                    }

                    Object result = session.execute(line);
                    session.put("_", result);    // set $_ to last result

                    if (result != null && !Boolean.FALSE.equals(session.get(".Gogo.format")))
                    {
                        out.println(session.format(result, Converter.INSPECT));
                    }
                }
                catch (Throwable e)
                {
                    if (!quit)
                    {
                        session.put("exception", e);
                        Object loc = session.get(".location");

                        if (null == loc || !loc.toString().contains(":"))
                        {
                            loc = "gogo";
                        }

                        out.println(loc + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
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

    private CharSequence getLine(String prompt) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        out.print(prompt);

        while (!quit)
        {
            out.flush();
            int c = in.read();

            switch (c)
            {
                case -1:
                case 4:    // EOT, ^D from telnet
                    quit = true;
                    break;

                case '\r':
                    break;

                case '\n':
                    if (sb.length() > 0)
                    {
                        return sb;
                    }
                    out.print(prompt);
                    break;

                case '\b':
                    if (sb.length() > 0)
                    {
                        out.print("\b \b");
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    break;

                default:
                    sb.append((char) c);
                    break;
            }
        }

        return null;
    }

    public void close()
    {
        quit = true;
    }

}
