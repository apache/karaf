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
package org.apache.felix.gogo.console.telnet;

import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.CommandSession;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.io.PrintStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TelnetShell extends Thread
{
    boolean quit;
    CommandProcessor processor;
    ServerSocket server;
    int port = 2019;
    List<Handler> handlers = new ArrayList<Handler>();

    protected void activate(ComponentContext context)
    {
        String s = (String) context.getProperties().get("port");
        if (s != null)
        {
            port = Integer.parseInt(s);
        }
        System.out.println("Telnet Listener at port " + port);
        start();
    }

    protected void deactivate(ComponentContext ctx) throws Exception
    {
        try
        {
            quit = true;
            server.close();
            interrupt();
        }
        catch (Exception e)
        {
            // Ignore
        }
    }

    public void run()
    {
        int delay = 0;
        try
        {
            while (!quit)
            {
                try
                {
                    server = new ServerSocket(port);
                    delay = 5;
                    while (!quit)
                    {
                        Socket socket = server.accept();
                        CommandSession session = processor.createSession(socket.getInputStream(), new PrintStream(socket.getOutputStream()), System.err);
                        Handler handler = new Handler(this, session, socket);
                        handlers.add(handler);
                        handler.start();
                    }
                }
                catch (BindException be)
                {
                    delay += 5;
                    System.err.println("Can not bind to port " + port);
                    try
                    {
                        Thread.sleep(delay * 1000);
                    }
                    catch (InterruptedException e)
                    {
                        // who cares?
                    }
                }
                catch (Exception e)
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
                        server.close();
                        Thread.sleep(2000);
                    }
                    catch (Exception ie)
                    {
                        //
                    }
                }
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
                //
            }
            for (Handler handler : handlers)
            {
                handler.close();
            }
        }
    }

    public void setProcessor(CommandProcessor processor)
    {
        this.processor = processor;
    }
}
