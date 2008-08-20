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


import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.osgi.framework.BundleContext;


/**
 * Implements a simple listener that will accept a single connection.
 */
class Listener
{

    private int m_Port;
    private Thread m_ListenerThread;
    private boolean m_Stop = false;
    private ServerSocket m_ServerSocket;
    private AtomicInteger m_UseCounter;
    private int m_MaxConnections;


    /**
     * Activates this listener on a listener thread (telnetconsole.Listener).
     */
    public void activate( BundleContext bundleContext )
    {
        //configure from framework property
        m_Port = getProperty( bundleContext, "osgi.shell.telnet.port", 6666 );
        m_MaxConnections = getProperty( bundleContext, "osgi.shell.telnet.maxconn", 2 );
        m_UseCounter = new AtomicInteger( 0 );
        m_ListenerThread = new Thread( new Acceptor(), "telnetconsole.Listener" );
        m_ListenerThread.start();
    }//activate


    /**
     * Deactivates this listener.
     * <p/>
     * The listener's socket will be closed, which should cause an interrupt in the
     * listener thread and allow for it to return. The calling thread joins the listener
     * thread until it returns (to ensure a clean stop).
     */
    public void deactivate()
    {
        try
        {
            m_Stop = true;
            //wait for the listener thread
            m_ServerSocket.close();
            m_ListenerThread.join();
        }
        catch ( Exception ex )
        {
            Activator.getServices().error( "Listener::deactivate()", ex );
        }
    }//deactivate

    /**
     * Class that implements the listener's accept logic as a <tt>Runnable</tt>.
     */
    private class Acceptor implements Runnable
    {

        /**
         * Listens constantly to a server socket and handles incoming connections.
         * One connection will be accepted and routed into the shell, all others will
         * be notified and closed.
         * <p/>
         * The mechanism that should allow the thread to unblock from the ServerSocket.accept() call
         * is currently closing the ServerSocket from another thread. When the stop flag is set,
         * this should cause the thread to return and stop.
         */
        public void run()
        {
            try
            {
                /*
                    A server socket is opened with a connectivity queue of a size specified
                    in int floodProtection.  Concurrent login handling under normal circumstances
                    should be handled properly, but denial of service attacks via massive parallel
                    program logins should be prevented with this.
                */
                m_ServerSocket = new ServerSocket( m_Port, 1 );
                do
                {
                    try
                    {
                        Socket s = m_ServerSocket.accept();
                        if ( m_UseCounter.get() >= m_MaxConnections )
                        {
                            //reject with message
                            PrintStream out = new PrintStream( s.getOutputStream() );
                            out.print( INUSE_MESSAGE );
                            out.flush();
                            //close
                            out.close();
                            s.close();
                        }
                        else
                        {
                            m_UseCounter.increment();
                            //run on the connection thread
                            Thread connectionThread = new Thread( new Shell( s, m_UseCounter ) );
                            connectionThread.start();
                        }
                    }
                    catch ( SocketException ex )
                    {
                    }
                }
                while ( !m_Stop );

            }
            catch ( IOException e )
            {
                Activator.getServices().error( "Listener.Acceptor::activate()", e );
            }
        }//run

    }//inner class Acceptor

    private static final String INUSE_MESSAGE = "Connection refused.\r\n"
        + "All possible connections are currently being used.\r\n";


    private int getProperty( BundleContext bundleContext, String propName, int defaultValue )
    {
        String propValue = bundleContext.getProperty( propName );
        if ( propValue != null )
        {
            try
            {
                return Integer.parseInt( propValue );
            }
            catch ( NumberFormatException ex )
            {
                Activator.getServices().error( "Listener::activate()", ex );
            }
        }

        return defaultValue;
    }

}//class Listener
