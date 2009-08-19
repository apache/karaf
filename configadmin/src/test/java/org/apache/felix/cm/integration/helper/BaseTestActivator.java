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
package org.apache.felix.cm.integration.helper;


import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;


public abstract class BaseTestActivator implements BundleActivator, ManagedService, ManagedServiceFactory
{

    // the bundle manifest header naming a pid of configurations we require
    public static final String HEADER_PID = "The-Test-PID";

    public int numManagedServiceUpdatedCalls = 0;
    public int numManagedServiceFactoryUpdatedCalls = 0;
    public int numManagedServiceFactoryDeleteCalls = 0;

    public Dictionary props = null;

    public Map<String, Dictionary> configs = new HashMap<String, Dictionary>();


    // ---------- ManagedService

    public void updated( Dictionary props )
    {
        numManagedServiceUpdatedCalls++;
        this.props = props;
    }


    // ---------- ManagedServiceFactory

    public String getName()
    {
        return getClass().getName();
    }


    public void deleted( String pid )
    {
        numManagedServiceFactoryDeleteCalls++;
        this.configs.remove( pid );
    }


    public void updated( String pid, Dictionary props )
    {
        numManagedServiceFactoryUpdatedCalls++;
        this.configs.put( pid, props );
    }


    protected Dictionary getServiceProperties( BundleContext bundleContext ) throws Exception
    {
        final Object prop = bundleContext.getBundle().getHeaders().get( HEADER_PID );
        if ( prop instanceof String )
        {
            final Hashtable props = new Hashtable();

            // multi-value PID support
            final String pid = ( String ) prop;
            if ( pid.indexOf( ',' ) > 0 )
            {
                final String[] pids = pid.split( "," );
                props.put( Constants.SERVICE_PID, pids );
            }
            else if ( pid.indexOf( ';' ) > 0 )
            {
                final String[] pids = pid.split( ";" );
                props.put( Constants.SERVICE_PID, Arrays.asList( pids ) );
            }
            else
            {
                props.put( Constants.SERVICE_PID, pid );
            }

            return props;
        }

        // missing pid, fail
        throw new Exception( "Missing " + HEADER_PID + " manifest header, cannot start" );
    }

}
