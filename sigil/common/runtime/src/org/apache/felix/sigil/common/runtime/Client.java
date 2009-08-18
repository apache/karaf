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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
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
    private InputStream in;
    private OutputStream out;


    public Client()
    {
    }


    public void connect(Properties props) throws IOException
    {
        InetAddress address = InetAddress.getByName( props.getProperty( ADDRESS_PROPERTY ) );
        int port = Integer.parseInt( props.getProperty( PORT_PROPERTY, "0" ) );
        socket = new Socket( address, port );
        in = socket.getInputStream();
        out = socket.getOutputStream();
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
        new StartAction( in, out ).client();
    }


    public void stop( long bundle ) throws IOException, BundleException
    {
        new StopAction( in, out ).client();
    }


    public void uninstall( long bundle ) throws IOException, BundleException
    {
        new UninstallAction( in, out ).client();
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
