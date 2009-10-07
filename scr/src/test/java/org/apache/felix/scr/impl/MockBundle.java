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
package org.apache.felix.scr.impl;


import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


public class MockBundle implements Bundle
{

    public Enumeration findEntries( String arg0, String arg1, boolean arg2 )
    {
        return null;
    }


    public BundleContext getBundleContext()
    {
        return null;
    }


    public long getBundleId()
    {
        return 0;
    }


    public URL getEntry( String name )
    {
        return getClass().getClassLoader().getResource( name );
    }


    public Enumeration getEntryPaths( String arg0 )
    {
        return null;
    }


    public Dictionary getHeaders()
    {
        return null;
    }


    public Dictionary getHeaders( String arg0 )
    {
        return null;
    }


    public long getLastModified()
    {
        return 0;
    }


    public String getLocation()
    {
        return "test:mockbundle";
    }


    public ServiceReference[] getRegisteredServices()
    {
        return null;
    }


    public URL getResource( String arg0 )
    {
        return null;
    }


    public Enumeration getResources( String arg0 )
    {
        return null;
    }


    public ServiceReference[] getServicesInUse()
    {
        return null;
    }


    public int getState()
    {
        return 0;
    }


    public String getSymbolicName()
    {
        return null;
    }


    public boolean hasPermission( Object arg0 )
    {
        return false;
    }


    public Class loadClass( String arg0 )
    {
        return null;
    }


    public void start()
    {

    }


    public void start( int options )
    {

    }


    public void stop()
    {

    }


    public void stop( int options )
    {

    }


    public void uninstall()
    {

    }


    public void update()
    {

    }


    public void update( InputStream arg0 )
    {

    }

}
