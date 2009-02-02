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
 * <p>
 * This class is instantiated by the {@link Listener} thread to handle a single
 * remote connection in its own thread. The connection handler thread either
 * terminates on request by the remote end or by the Remote Shell bundle being
 * stopped. In the latter case, the {@link #terminate()} method is called, which
 * closes the Socket used to handle the remote console. This causes a
 * <code>SocketException</code> in the handler thread reading from the socket
 * which in turn causes the {@link #run()} method to terminate and thus to
 * end the handler thread. 
 */
class Shell implements Runnable
{

    private Listener m_owner;
    private Socket m_Socket;
    private AtomicInteger m_UseCounter;


    public Shell( Listener owner, Socket s, AtomicInteger counter )
    {
        m_owner = owner;
        m_Socket = s;
        m_UseCounter = counter;
    }//constructor

    void terminate()
    {
        // called by Listener.deactivate() to terminate this session
        exit( "\r\nFelix Remote Shell Console Terminating" );
    }//terminate
    
    /**
     * Runs the shell.
     */
    public void run()
    {
        m_owner.registerConnection( this );
        
        try
        {
            PrintStream out = new TerminalPrintStream( m_Socket.getOutputStream() );
            BufferedReader in = new BufferedReader( new TerminalReader( m_Socket.getInputStream(), out ) );
            ReentrantLock lock = new ReentrantLock();

            // Print welcome banner.
            out.println();
            out.println( "Felix Remote Shell Console:" );
            out.println( "============================" );
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
                        out.println( "exit" );
                        return;
                    }
                }
                catch ( Exception ex )
                {
                    return;
                }

                line = line.trim();
                if ( line.equalsIgnoreCase( "exit" ) || line.equalsIgnoreCase( "disconnect" ) )
                {
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
        finally
        {
            // no need to clean up in/out, since exit does it all
            exit( null );
        }
    }//run


    private void exit(String message)
    {
        // farewell message
        try
        {
            PrintStream out = new TerminalPrintStream( m_Socket.getOutputStream() );
            if ( message != null )
            {
                out.println( message );
            }
            out.println( "Good Bye!" );
            out.close();
        }
        catch ( IOException ioe )
        {
            // ignore
        }

        try
        {
            m_Socket.close();
        }
        catch ( IOException ex )
        {
            Activator.getServices().error( "Shell::exit()", ex );
        }
        m_owner.unregisterConnection( this );
        m_UseCounter.decrement();
    }//exit

}//class Shell
