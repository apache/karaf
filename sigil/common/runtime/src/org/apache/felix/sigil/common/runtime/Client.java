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
import java.util.Map;
import java.util.Properties;

import org.apache.felix.sigil.common.runtime.io.InstallAction;
import org.apache.felix.sigil.common.runtime.io.StartAction;
import org.apache.felix.sigil.common.runtime.io.StatusAction;
import org.apache.felix.sigil.common.runtime.io.StopAction;
import org.apache.felix.sigil.common.runtime.io.UninstallAction;
import org.apache.felix.sigil.common.runtime.io.UpdateAction;
import org.apache.felix.sigil.common.runtime.io.UpdateAction.Update;
import org.osgi.framework.BundleException;


/**
 * @author dave
 *
 */
public class Client
{
    public static final String PORT_PROPERTY = "port";
    public static final String ADDRESS_PROPERTY = "address";
    
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;


    public Client()
    {
    }
    
    public static void main(String[] args) throws IOException, BundleException {
        Client cl = new Client();
        Properties props = new Properties();
        props.put( ADDRESS_PROPERTY, "localhost" );
        props.put( PORT_PROPERTY, "9090" );
        cl.connect(  props );
        System.out.println( cl.status() );
        long id = cl.install( "file:/Users/dave/.m2/repository/org/apache/felix/org.apache.felix.log/1.1.0-SNAPSHOT/org.apache.felix.log-1.1.0-SNAPSHOT.jar" );
        System.out.println( cl.status() );
        cl.start( id );
        System.out.println( cl.status() );
        
        id = cl.install( "file:/Users/dave/.m2/repository/org/apache/felix/org.apache.felix.shell/1.3.0-SNAPSHOT/org.apache.felix.shell-1.3.0-SNAPSHOT.jar" );
        cl.start( id );
        
        id = cl.install( "file:/Users/dave/.m2/repository/org/apache/felix/org.apache.felix.shell.tui/1.3.0-SNAPSHOT/org.apache.felix.shell.tui-1.3.0-SNAPSHOT.jar" );
        cl.start( id );
        
        System.out.println( cl.status() );
    }


    public void connect(Properties props) throws IOException
    {
        String v = props.getProperty( ADDRESS_PROPERTY );
        InetAddress address = v == null ? null : InetAddress.getByName( v );
        int port = Integer.parseInt( props.getProperty( PORT_PROPERTY, "0" ) );
        InetSocketAddress endpoint = new InetSocketAddress(address, port);
        
        socket = new Socket();
        socket.connect( endpoint );
        
        Main.log( "Connected to " + endpoint );
        
        in = new DataInputStream( socket.getInputStream() );
        out = new DataOutputStream( socket.getOutputStream() );
    }


    public void close() throws IOException
    {
        socket.close();
    }


    public long install( String url ) throws IOException, BundleException
    {
        return new InstallAction( in, out ).client( url );
    }


    public void start( long bundle ) throws IOException, BundleException
    {
        new StartAction( in, out ).client( bundle );
    }


    public void stop( long bundle ) throws IOException, BundleException
    {
        new StopAction( in, out ).client( bundle );
    }


    public void uninstall( long bundle ) throws IOException, BundleException
    {
        new UninstallAction( in, out ).client( bundle );
    }


    public void update( long bundle ) throws IOException, BundleException
    {
        Update update = new UpdateAction.Update(bundle, null);
        new UpdateAction( in, out ).client(update);
    }


    public void update( long bundle, String url ) throws IOException, BundleException
    {
        Update update = new UpdateAction.Update(bundle, url);
        new UpdateAction( in, out ).client(update);
    }


    public Map<Long, String> status() throws IOException, BundleException
    {
        return new StatusAction( in, out ).client();
    }
}
