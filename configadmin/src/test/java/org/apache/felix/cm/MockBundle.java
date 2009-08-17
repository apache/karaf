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
package org.apache.felix.cm;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;


public class MockBundle implements Bundle
{

    private final BundleContext context;
    private final String location;


    public MockBundle( BundleContext context, String location )
    {
        this.context = context;
        this.location = location;
    }


    public Enumeration findEntries( String arg0, String arg1, boolean arg2 )
    {
        // TODO Auto-generated method stub
        return null;
    }


    public BundleContext getBundleContext()
    {
        return context;
    }


    public long getBundleId()
    {
        return 0;
    }


    public URL getEntry( String arg0 )
    {
        // TODO Auto-generated method stub
        return null;
    }


    public Enumeration getEntryPaths( String arg0 )
    {
        // TODO Auto-generated method stub
        return null;
    }


    public Dictionary getHeaders()
    {
        // TODO Auto-generated method stub
        return null;
    }


    public Dictionary getHeaders( String arg0 )
    {
        // TODO Auto-generated method stub
        return null;
    }


    public long getLastModified()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    public String getLocation()
    {
        return location;
    }


    public ServiceReference[] getRegisteredServices()
    {
        // TODO Auto-generated method stub
        return null;
    }


    public URL getResource( String arg0 )
    {
        // TODO Auto-generated method stub
        return null;
    }


    public Enumeration getResources( String arg0 ) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }


    public ServiceReference[] getServicesInUse()
    {
        // TODO Auto-generated method stub
        return null;
    }


    public int getState()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    public String getSymbolicName()
    {
        // TODO Auto-generated method stub
        return null;
    }


    public boolean hasPermission( Object arg0 )
    {
        // TODO Auto-generated method stub
        return false;
    }


    public Class loadClass( String arg0 ) throws ClassNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }


    public void start() throws BundleException
    {
        // TODO Auto-generated method stub

    }


    public void stop() throws BundleException
    {
        // TODO Auto-generated method stub

    }


    public void uninstall() throws BundleException
    {
        // TODO Auto-generated method stub

    }


    public void update() throws BundleException
    {
        // TODO Auto-generated method stub

    }


    public void update( InputStream arg0 ) throws BundleException
    {
        // TODO Auto-generated method stub

    }

}
