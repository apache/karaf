/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.felix.shell.remote;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

import org.apache.felix.shell.ShellService;


/**
 * Implements the shell.
 */
class Shell implements Runnable
{

    private Socket m_Socket;
    private AtomicInteger m_UseCounter;


    public Shell( Socket s, AtomicInteger counter )
    {
        m_Socket = s;
        m_UseCounter = counter;
    }//constructor


    /**
     * Runs the shell.
     */
    public void run()
    {
        try
        {
            PrintStream out = new TerminalPrintStream( m_Socket.getOutputStream() );
            BufferedReader in = new BufferedReader( new TerminalReader( m_Socket.getInputStream(), out ) );
            ReentrantLock lock = new ReentrantLock();

            // Print welcome banner.
            out.println();
            out.println( "Felix Shell Console:" );
            out.println( "=====================" );
            out.println( "" );

            do
            {
                out.print( "-> " );
                String line = "";
                try
                {
                    line = in.readLine();
                    //make sure to capture end of stream
                    if ( line == null )
                    {
                        exit();
                        return;
                    }
                }
                catch ( Exception ex )
                {
                    exit();
                    return;
                }

                line = line.trim();
                if ( line.equalsIgnoreCase( "exit" ) || line.equalsIgnoreCase( "disconnect" ) )
                {
                    in.close();
                    out.close();
                    exit();
                    return;
                }

                ShellService shs = Activator.getServices().getFelixShellService( ServiceMediator.NO_WAIT );
                try
                {
                    lock.acquire();
                    shs.executeCommand( line, out, out );
                }
                catch ( Exception ex )
                {
                    Activator.getServices().error( "Shell::run()", ex );
                }
                finally
                {
                    lock.release();
                }
            }
            while ( true );
        }
        catch ( IOException ex )
        {
            Activator.getServices().error( "Shell::run()", ex );
        }
    }//run


    private void exit()
    {
        try
        {
            m_Socket.close();
        }
        catch ( IOException ex )
        {
            Activator.getServices().error( "Shell::exit()", ex );
        }
        m_UseCounter.decrement();
    }//exit

}//class Shell
