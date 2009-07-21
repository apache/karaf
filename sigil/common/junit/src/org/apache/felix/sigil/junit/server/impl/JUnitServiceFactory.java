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

package org.apache.felix.sigil.junit.server.impl;


import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;


public class JUnitServiceFactory implements ServiceFactory
{

    private HashMap<String, Class<? extends TestCase>> tests = new HashMap<String, Class<? extends TestCase>>();
    private TestClassListener listener;


    public void start( BundleContext ctx )
    {
        listener = new TestClassListener( this );
        ctx.addBundleListener( listener );
        //listener.index(ctx.getBundle());
        for ( Bundle b : ctx.getBundles() )
        {
            if ( b.getState() == Bundle.RESOLVED )
            {
                listener.index( b );
            }
        }
    }


    public void stop( BundleContext ctx )
    {
        ctx.removeBundleListener( listener );
        listener = null;
    }


    public Object getService( Bundle bundle, ServiceRegistration reg )
    {
        return new JUnitServiceImpl( this, bundle.getBundleContext() );
    }


    public void ungetService( Bundle bundle, ServiceRegistration reg, Object service )
    {
    }


    public void registerTest( Class<? extends TestCase> clazz )
    {
        tests.put( clazz.getName(), clazz );
    }


    public void unregister( Class<? extends TestCase> clazz )
    {
        tests.remove( clazz.getName() );
    }


    public Set<String> getTests()
    {
        return new TreeSet<String>( tests.keySet() );
    }


    public TestSuite getTest( String test )
    {
        Class<? extends TestCase> tc = tests.get( test );
        return tc == null ? null : new TestSuite( tc );
    }

}
