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
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.apache.felix.gogo.options.Option;
import org.apache.felix.gogo.options.Options;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;

/*
 * a very simple Telnet server.
 * real remote access should be via ssh.
 */
public class Telnet implements Runnable
{
    static final String[] functions = { "telnetd" };
    
    private static final int defaultPort = 2019;
    private final CommandProcessor processor;
    private ServerSocket server;
    private Thread thread;
    private boolean quit;
    private int port;

    public Telnet(CommandProcessor procesor)
    {
        this.processor = procesor;
    }

    public void telnetd(String[] argv) throws IOException
    {
        final String[] usage = { "telnetd - start simple telnet server",
                "Usage: telnetd [-p port] start | stop | status",
                "  -p --port=PORT           listen port (default=" + defaultPort + ")",
                "  -? --help                show help" };

        Option opt = Options.compile(usage).parse(argv);
        List<String> args = opt.args();

        if (opt.isSet("help") || args.isEmpty())
        {
            opt.usage();
            return;
        }

        String command = args.get(0);

        if ("start".equals(command))
        {
            if (server != null)
            {
                throw new IllegalStateException("telnetd is already running on port "
                    + port);
            }
            port = opt.getNumber("port");
            start();
            status();
        }
        else if ("stop".equals(command))
        {
            if (server == null)
            {
                throw new IllegalStateException("telnetd is not running.");
            }
            stop();
        }
        else if ("status".equals(command))
        {
            status();
        }
        else
        {
            throw opt.usageError("bad command: " + command);
        }
    }

    private void status()
    {
        if (server != null)
        {
            System.out.println("telnetd is running on port " + port);
        }
        else
        {
            System.out.println("telnetd is not running.");
        }
    }

    private void start() throws IOException
    {
        quit = false;
        server = new ServerSocket(port);
        thread = new Thread(this, "gogo telnet");
        thread.start();
    }

    private void stop() throws IOException
    {
        quit = true;
        server.close();
        server = null;
        thread.interrupt();
    }

    public void run()
    {
        try
        {
            while (!quit)
            {
                final Socket socket = server.accept();
                PrintStream out = new PrintStream(socket.getOutputStream());
                final CommandSession session = processor.createSession(
                    socket.getInputStream(), out, out);

                Thread handler = new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            session.execute("gosh --login --noshutdown");
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        finally
                        {
                            session.close();
                            try
                            {
                                socket.close();
                            }
                            catch (IOException e)
                            {
                            }
                        }
                    }
                };
                handler.start();
            }
        }
        catch (IOException e)
        {
            if (!quit)
            {
                e.printStackTrace();
            }
        }
        finally
        {
            try
            {
                if (server != null)
                {
                    server.close();
                }
            }
            catch (IOException e)
            {
            }
        }
    }
}
