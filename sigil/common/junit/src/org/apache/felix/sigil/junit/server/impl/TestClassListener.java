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


import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;


public class TestClassListener implements SynchronousBundleListener
{
    private static final Logger log = Logger.getLogger( TestClassListener.class.getName() );

    private final JUnitServiceFactory service;

    private HashMap<Long, Class<TestCase>[]> registrations = new HashMap<Long, Class<TestCase>[]>();


    public TestClassListener( JUnitServiceFactory service )
    {
        this.service = service;
    }


    public void bundleChanged( BundleEvent event )
    {
        switch ( event.getType() )
        {
            case BundleEvent.RESOLVED:
                index( event.getBundle() );
                break;
            case BundleEvent.UNRESOLVED:
                unindex( event.getBundle() );
                break;
        }
    }


    void index( Bundle bundle )
    {
        if ( isTestBundle( bundle ) )
        {
            List<String> tests = findTests( bundle );

            if ( !tests.isEmpty() )
            {
                LinkedList<Class<? extends TestCase>> regs = new LinkedList<Class<? extends TestCase>>();

                for ( String jc : tests )
                {
                    try
                    {
                        Class<?> clazz = bundle.loadClass( jc );
                        if ( isTestCase( clazz ) )
                        {
                            Class<? extends TestCase> tc = clazz.asSubclass( TestCase.class );
                            regs.add( tc );
                            service.registerTest( tc );
                        }
                    }
                    catch ( ClassNotFoundException e )
                    {
                        log.log( Level.WARNING, "Failed to load class " + jc, e );
                    }
                    catch ( NoClassDefFoundError e )
                    {
                        log.log( Level.WARNING, "Failed to load class " + jc, e );
                    }
                }

                registrations.put( bundle.getBundleId(), toArray( regs ) );
            }
        }
    }


    private boolean isTestBundle( Bundle bundle )
    {
        try
        {
            bundle.loadClass( TestCase.class.getName() );
            return true;
        }
        catch ( ClassNotFoundException e )
        {
            return false;
        }
    }


    @SuppressWarnings("unchecked")
    private Class<TestCase>[] toArray( LinkedList<Class<? extends TestCase>> regs )
    {
        return regs.toArray( new Class[regs.size()] );
    }


    private boolean isTestCase( Class<?> clazz )
    {
        return TestCase.class.isAssignableFrom( clazz ) && !Modifier.isAbstract( clazz.getModifiers() )
            && !clazz.getPackage().getName().startsWith( "junit" );
    }


    void unindex( Bundle bundle )
    {
        Class<TestCase>[] classes = registrations.remove( bundle.getBundleId() );
        if ( classes != null )
        {
            for ( Class<TestCase> tc : classes )
            {
                service.unregister( tc );
            }
        }
    }


    private List<String> findTests( Bundle bundle )
    {
        @SuppressWarnings("unchecked")
        Enumeration<URL> urls = bundle.findEntries( "", "*.class", true );

        LinkedList<String> tests = new LinkedList<String>();
        while ( urls.hasMoreElements() )
        {
            URL url = urls.nextElement();
            tests.add( toClassName( url ) );
        }

        return tests;
    }


    private String toClassName( URL url )
    {
        String f = url.getFile();
        String cn = f.substring( 1, f.length() - 6 );
        return cn.replace( '/', '.' );
    }

}
