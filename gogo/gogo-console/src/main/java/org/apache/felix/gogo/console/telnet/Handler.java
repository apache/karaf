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

import org.apache.felix.gogo.console.stdio.Console;
import org.osgi.service.command.CommandSession;

import java.io.IOException;
import java.net.Socket;

public class Handler extends Thread
{
    TelnetShell master;
    Socket socket;
    CommandSession session;
    Console console;

    public Handler(TelnetShell master, CommandSession session, Socket socket) throws IOException
    {
        this.master = master;
        this.socket = socket;
        this.session = session;
    }

    public void run()
    {
        try
        {
            console = new Console();
            console.setSession(session);
            console.run();
        }
        finally
        {
            close();
            master.handlers.remove(this);
        }
    }

    public void close()
    {
        session.close();
        try
        {
            socket.close();
        }
        catch (IOException e)
        {
            // Ignore, this is close
        }
    }

}
