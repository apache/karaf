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
package org.apache.felix.cm.impl;


import java.io.IOException;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.cm.PersistenceManager;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 * The <code>Factory</code> class is used to manage mappings between factory
 * PIDs the configuration PID belonging to it.
 */
class Factory extends ConfigurationBase
{

    public static final String FACTORY_PID = "factory.pid";

    public static final String FACTORY_PID_LIST = "factory.pidList";

    // the set of configuration PIDs belonging to this factory
    private final Set pids = new HashSet();;


    static boolean exists( PersistenceManager persistenceManager, String factoryPid )
    {
        return persistenceManager.exists( factoryPidToIdentifier( factoryPid ) );
    }


    static Factory load( ConfigurationManager configurationManager, PersistenceManager persistenceManager,
        String factoryPid ) throws IOException
    {
        Dictionary dict = persistenceManager.load( factoryPidToIdentifier( factoryPid ) );
        return new Factory( configurationManager, persistenceManager, factoryPid, dict );
    }


    private static String factoryPidToIdentifier( String factoryPid )
    {
        return factoryPid + ".factory";
    }


    Factory( ConfigurationManager configurationManager, PersistenceManager persistenceManager, String factoryPid )
    {
        super(configurationManager, persistenceManager, factoryPid, null);
    }


    Factory( ConfigurationManager configurationManager, PersistenceManager persistenceManager, String factoryPid, Dictionary props )
    {
        super( configurationManager, persistenceManager, factoryPid, ( String ) props
            .get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );

        // set pids
        String[] pidList = ( String[] ) props.get( FACTORY_PID_LIST );
        if ( pidList != null )
        {
            for ( int i = 0; i < pidList.length; i++ )
            {
                pids.add( pidList[i] );
            }
        }
    }


    String getFactoryPid()
    {
        return getBaseId();
    }


    Set getPIDs()
    {
        return new HashSet( pids );
    }


    boolean addPID( String pid )
    {
        return pids.add( pid );
    }


    boolean removePID( String pid )
    {
        return pids.remove( pid );
    }


    void store() throws IOException
    {
        Hashtable props = new Hashtable();

        replaceProperty( props, ConfigurationAdmin.SERVICE_BUNDLELOCATION, getStaticBundleLocation() );

        if ( !pids.isEmpty() )
        {
            props.put( FACTORY_PID_LIST, pids.toArray( new String[pids.size()] ) );
        }

        String id = factoryPidToIdentifier( this.getFactoryPid() );
        if ( props.isEmpty() )
        {
            getPersistenceManager().delete( id );
        }
        else
        {
            props.put( FACTORY_PID, this.getFactoryPid() );
            getPersistenceManager().store( id, props );
        }
    }
}
