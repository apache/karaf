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
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

import org.apache.felix.sigil.common.runtime.BundleForm.BundleStatus;
import org.apache.felix.sigil.common.runtime.io.InstallAction;
import org.apache.felix.sigil.common.runtime.io.RefreshAction;
import org.apache.felix.sigil.common.runtime.io.StartAction;
import org.apache.felix.sigil.common.runtime.io.StatusAction;
import org.apache.felix.sigil.common.runtime.io.StopAction;
import org.apache.felix.sigil.common.runtime.io.UninstallAction;
import org.apache.felix.sigil.common.runtime.io.UpdateAction;
import org.apache.felix.sigil.common.runtime.io.UpdateAction.Update;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.apache.felix.sigil.common.runtime.Runtime.PORT_PROPERTY;
import static org.apache.felix.sigil.common.runtime.Runtime.ADDRESS_PROPERTY;


/**
 * @author dave
 *
 */
public class Client
{
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;


    public Client()
    {
    }
    
    public void connect(Properties props) throws IOException
    {
        String v = props.getProperty( ADDRESS_PROPERTY );
        InetAddress address = v == null ? null : InetAddress.getByName( v );
        int port = Integer.parseInt( props.getProperty( PORT_PROPERTY, "0" ) );
        
        if ( port < 1 ) {
            throw new IOException( "Missing or invalid port" );
        }
        
        InetSocketAddress endpoint = new InetSocketAddress(address, port);
        
        socket = new Socket();
        socket.connect( endpoint );
        
        Main.log( "Connected to " + endpoint );
        
        in = new DataInputStream( socket.getInputStream() );
        out = new DataOutputStream( socket.getOutputStream() );        
    }

    public boolean isConnected() {
        return socket != null;
    }
    
    public void disconnect() throws IOException
    {
        socket.close();
        socket = null;
        in = null;
        out = null;
    }

    public void apply(BundleForm form) throws IOException, BundleException {
        BundleStatus[] newStatus = form.toStatus();

        BundleStatus[] currentStatus = status();
        
        stopOldBundles(currentStatus, newStatus);
        
        boolean change = uninstallOldBundles(currentStatus, newStatus);
        change |= installNewBundles(currentStatus, newStatus);
        
        if ( change )
            refresh();
        
        startNewBundles(newStatus);
    }

    public void refresh() throws IOException, BundleException
    {
        if ( socket == null ) throw new IllegalStateException( "Not connected" );
        new RefreshAction( in, out ).client();
    }

    public long install( String url ) throws IOException, BundleException
    {
        if ( socket == null ) throw new IllegalStateException( "Not connected" );
        return new InstallAction( in, out ).client( url );
    }


    public void start( long bundle ) throws IOException, BundleException
    {
        if ( socket == null ) throw new IllegalStateException( "Not connected" );
        new StartAction( in, out ).client( bundle );
    }


    public void stop( long bundle ) throws IOException, BundleException
    {
        if ( socket == null ) throw new IllegalStateException( "Not connected" );
        new StopAction( in, out ).client( bundle );
    }


    public void uninstall( long bundle ) throws IOException, BundleException
    {
        if ( socket == null ) throw new IllegalStateException( "Not connected" );
        new UninstallAction( in, out ).client( bundle );
    }


    public void update( long bundle ) throws IOException, BundleException
    {
        if ( socket == null ) throw new IllegalStateException( "Not connected" );
        Update update = new UpdateAction.Update(bundle, null);
        new UpdateAction( in, out ).client(update);
    }


    public void update( long bundle, String url ) throws IOException, BundleException
    {
        if ( socket == null ) throw new IllegalStateException( "Not connected" );
        Update update = new UpdateAction.Update(bundle, url);
        new UpdateAction( in, out ).client(update);
    }


    public BundleStatus[] status() throws IOException, BundleException
    {
        if ( socket == null ) throw new IllegalStateException( "Not connected" );
        return new StatusAction( in, out ).client();
    }
    
    private boolean installNewBundles(BundleStatus[] status, BundleStatus[] newStatus) throws IOException, BundleException
    {
        boolean change = false;
        for (BundleStatus n : newStatus) {
            boolean found = false;
            for ( BundleStatus o : status ) {
                if ( o.isMatch(n) ) {
                    update(o.getId(), n.getLocation());
                    found = true;
                    change = true;
                    break;
                }
            }
            
            if ( !found ) {
                install(n.getLocation());
                change = true;
            }
        }
        
        return change;
    }

    private void startNewBundles(BundleStatus[] newStatus) throws IOException, BundleException
    {
        BundleStatus[] status = status();
        for (BundleStatus n : newStatus) {
            if ( n.getStatus() == Bundle.ACTIVE ) {
                for ( BundleStatus o : status ) {
                    if ( o.isMatch(n) ) {
                        start(o.getId());
                    }
                }
            }            
        }
    }

    private void stopOldBundles(BundleStatus[] status, BundleStatus[] newStatus) throws IOException, BundleException
    {
        for (BundleStatus n : newStatus) {
            if ( n.getStatus() == Bundle.INSTALLED ) {
                for ( BundleStatus o : status ) {
                    if ( o.getId() != 0 ) {
                        if ( o.isMatch(n) ) {
                            stop(o.getId());
                        }                        
                    }
                }
            }            
        }
    }

    private boolean uninstallOldBundles(BundleStatus[] status, BundleStatus[] newStatus) throws IOException, BundleException
    {
        boolean change = false;
        for ( BundleStatus o : status ) {
            if ( o.getId() != 0 ) {
                boolean found = false;
                
                for (BundleStatus n : newStatus) {
                    if ( o.isMatch(n) ) {
                        found = true;
                        break;
                    }
                }
                
                if ( !found ) {
                    change = true;
                    uninstall(o.getId());
                }
            }
        }
        return change;
    }    
}
