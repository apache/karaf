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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;

/**
 * Implements a simple listener that will accept a single connection.
 */
class Listener
{
    private final int m_port;
    private final String m_ip;
    private final Thread m_listenerThread;
    private final Acceptor m_acceptor;
    private final AtomicInteger m_useCounter;
    private final int m_maxConnections;
    private final int m_soTimeout;
    private final Set m_connections;
    private final ServiceMediator m_services;

    /**
     * Activates this listener on a listener thread (telnetconsole.Listener).
     */
    public Listener(BundleContext context, ServiceMediator services) throws IOException
    {
        m_services = services;
        //configure from framework property
        m_ip = getProperty(context, "osgi.shell.telnet.ip", "127.0.0.1");
        m_port = getProperty(context, "osgi.shell.telnet.port", 6666);
        m_soTimeout = getProperty(context, "osgi.shell.telnet.socketTimeout", 0);
        m_maxConnections = getProperty(context, "osgi.shell.telnet.maxconn", 2);
        m_useCounter = new AtomicInteger(0);
        m_connections = new HashSet();
        m_acceptor = new Acceptor();
        m_listenerThread = new Thread(m_acceptor, "telnetconsole.Listener");
        m_listenerThread.start();
    }//activate
    public ServiceMediator getServices()
    {
        return m_services;
    }

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
            //wait for the listener thread
            m_acceptor.close();
            m_listenerThread.join();
        }
        catch (Exception ex)
        {
            m_services.error("Listener::deactivate()", ex);
        }

        // get the active connections (and clear the list)
        // we have to work on a copy, since stopping any active connection
        // will try to remove itself from the set, which might cause a
        // ConcurrentModificationException if we would iterate over the list
        Shell[] connections;
        synchronized (m_connections)
        {
            connections = (Shell[]) m_connections.toArray(new Shell[m_connections.size()]);
            m_connections.clear();
        }

        // now terminate all active connections
        for (int i = 0; i < connections.length; i++)
        {
            connections[i].terminate();
        }
    }//deactivate

    /**
     * Class that implements the listener's accept logic as a <tt>Runnable</tt>.
     */
    private class Acceptor implements Runnable
    {
        private volatile boolean m_stop = false;
        private final ServerSocket m_serverSocket;

        Acceptor() throws IOException
        {
            m_serverSocket = new ServerSocket(m_port, 1, InetAddress.getByName(m_ip));
            m_serverSocket.setSoTimeout(m_soTimeout);
        }

        public void close() throws IOException
        {
            m_stop = true;
            m_serverSocket.close();
        }

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
                do
                {
                    try
                    {
                        Socket s = m_serverSocket.accept();
                        if (m_useCounter.get() >= m_maxConnections)
                        {
                            //reject with message
                            PrintStream out = new PrintStream(s.getOutputStream());
                            out.print(INUSE_MESSAGE);
                            out.flush();
                            //close
                            out.close();
                            s.close();
                        }
                        else
                        {
                            m_useCounter.increment();
                            //run on the connection thread
                            Thread connectionThread = new Thread(new Shell(Listener.this, s, m_useCounter));
                            connectionThread.setName("telnetconsole.shell remote=" + s.getRemoteSocketAddress());
                            connectionThread.start();
                        }
                    }
                    catch (SocketException ex)
                    {
                    }
                    catch (SocketTimeoutException ste)
                    {
                        // Caught socket timeout exception. continue
                    }
                }
                while (!m_stop);

            }
            catch (IOException e)
            {
                m_services.error("Listener.Acceptor::run()", e);
            }
        }//run
    }//inner class Acceptor

    private static final String INUSE_MESSAGE = "Connection refused.\r\n" + "All possible connections are currently being used.\r\n";

    private int getProperty(BundleContext bundleContext, String propName, int defaultValue)
    {
        String propValue = bundleContext.getProperty(propName);
        if (propValue != null)
        {
            try
            {
                return Integer.parseInt(propValue);
            }
            catch (NumberFormatException ex)
            {
                m_services.error("Listener::activate()", ex);
            }
        }

        return defaultValue;
    }

    private String getProperty(BundleContext bundleContext, String propName, String defaultValue)
    {
        String propValue = bundleContext.getProperty(propName);
        if (propValue != null)
        {
            return propValue;
        }

        return defaultValue;
    }

    /**
     * Registers the given {@link Shell} instance handling a remote connection
     * to this listener.
     * <p>
     * This method is called by the {@link Shell#run()} method to register the
     * remote connection for it to be terminated in case this listener is
     * {@link #deactivate() deactivated} before the remote connection is
     * terminated.
     * 
     * @param connection The {@link Shell} connection to register
     */
    void registerConnection(Shell connection)
    {
        synchronized (m_connections)
        {
            m_connections.add(connection);
        }
    }

    /**
     * Unregisters the given {@link Shell} instance handling a remote connection
     * from this listener.
     * <p>
     * This method is called when the {@link Shell#run()} method terminates to
     * inform this listener instance that the remote connection has ended and
     * thus does not need to be cleaned up when this listener terminates.
     * 
     * @param connection The {@link Shell} connection to unregister
     */
    void unregisterConnection(Shell connection)
    {
        synchronized (m_connections)
        {
            m_connections.remove(connection);
        }
    }
}//class Listener