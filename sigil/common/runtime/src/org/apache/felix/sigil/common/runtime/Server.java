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

package org.apache.felix.sigil.common.runtime;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.sigil.common.runtime.io.Action;
import org.apache.felix.sigil.common.runtime.io.InstallAction;
import org.apache.felix.sigil.common.runtime.io.RefreshAction;
import org.apache.felix.sigil.common.runtime.io.StartAction;
import org.apache.felix.sigil.common.runtime.io.StatusAction;
import org.apache.felix.sigil.common.runtime.io.StopAction;
import org.apache.felix.sigil.common.runtime.io.UninstallAction;
import org.apache.felix.sigil.common.runtime.io.UpdateAction;
import org.osgi.framework.launch.Framework;

import static org.apache.felix.sigil.common.runtime.Runtime.ADDRESS_PROPERTY;
import static org.apache.felix.sigil.common.runtime.Runtime.PORT_PROPERTY;
import static org.apache.felix.sigil.common.runtime.io.Constants.*;


/**
 * @author dave
 *
 */
public class Server
{
    private final Framework fw;
    private Thread accept;
    private ExecutorService read = Executors.newCachedThreadPool();

    private AtomicBoolean stopped = new AtomicBoolean();


    public Server( Framework fw )
    {
        this.fw = fw;
    }


    public void start( Properties props ) throws IOException
    {
        final ServerSocket socket = new ServerSocket();
        
        String v = props.getProperty( ADDRESS_PROPERTY );
        InetAddress address = v == null ? null : InetAddress.getByName( v );
        int port = Integer.parseInt( props.getProperty( PORT_PROPERTY, "0" ) );
        
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);        
        socket.bind( socketAddress );

        Main.log( "Started server listening on " + socket.getLocalSocketAddress() + ":" + socket.getLocalPort() );
        
        accept = new Thread( new Runnable()
        {
            public void run()
            {
                while ( !stopped.get() )
                {
                    try
                    {
                        read.execute( new Reader( socket.accept() ) );
                    }
                    catch ( IOException e )
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        } );
        
        accept.start();
        
        Main.log( "Started thread!" );
    }


    public void stop()
    {
        stopped.set( true );
        accept.interrupt();
        accept = null;
    }

    public class Reader implements Runnable
    {

        private final Socket socket;


        /**
         * @param accept
         */
        public Reader( Socket socket )
        {
            this.socket = socket;
        }


        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            Main.log( "Accepted " + socket.getInetAddress() + ":" + socket.getLocalPort() );
            
            try
            {
                DataInputStream in = new DataInputStream( socket.getInputStream() );
                DataOutputStream out = new DataOutputStream( socket.getOutputStream() );
                
                while ( !stopped.get() )
                {
                    int action = in.readInt();
                    
                    Action<?, ?> task = null;
                    
                    switch ( action )
                    {
                        case INSTALL:
                            task = new InstallAction( in, out );
                            break;
                        case START:
                            task = new StartAction( in, out );
                            break;
                        case STOP:
                            task = new StopAction( in, out );
                            break;
                        case UNINSTALL:
                            task = new UninstallAction( in, out );
                            break;
                        case UPDATE:
                            task = new UpdateAction( in, out );
                            break;
                        case STATUS:
                            task = new StatusAction( in, out );
                            break;
                        case REFRESH:
                            task = new RefreshAction(in, out);
                            break;
                    }
                    
                    if ( task == null ) {
                        Main.log( "Invalid action " + action );
                    }
                    else {
                        task.server( fw );
                    }
                }
            }
            catch ( EOFException e ) {
                // fine
            }
            catch ( IOException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finally {
                try
                {
                    socket.close();
                }
                catch ( IOException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
