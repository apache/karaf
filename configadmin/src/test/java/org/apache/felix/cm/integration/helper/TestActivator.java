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
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;


public class TestActivator implements BundleActivator, ManagedService
{

    // the bundle manifest header naming a pid of configurations we require
    public static final String HEADER_PID = "The-Test-PID";

    public static TestActivator INSTANCE = null;

    public int numUpdatedCalls = 0;

    public Dictionary props = null;


    public void start( BundleContext context ) throws Exception
    {

        Object prop = context.getBundle().getHeaders().get( HEADER_PID );
        if ( prop instanceof String )
        {
            Hashtable props = new Hashtable();

            // multi-value PID support
            String pid = ( String ) prop;
            if ( pid.indexOf( ',' ) > 0 )
            {
                String[] pids = pid.split( "," );
                props.put( Constants.SERVICE_PID, pids );
            }
            else if ( pid.indexOf( ';' ) > 0 )
            {
                String[] pids = pid.split( ";" );
                props.put( Constants.SERVICE_PID, Arrays.asList( pids ) );
            }
            else
            {
                props.put( Constants.SERVICE_PID, pid );
            }

            context.registerService( ManagedService.class.getName(), this, props );
        }
        else
        {
            // missing pid, fail
            throw new Exception( "Missing " + HEADER_PID + " manifest header, cannot start" );
        }

        INSTANCE = this;
    }


    public void stop( BundleContext context ) throws Exception
    {
        INSTANCE = null;
    }


    public void updated( Dictionary props )
    {
        numUpdatedCalls++;
        this.props = props;
    }

}
