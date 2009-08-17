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
import org.osgi.service.log.LogService;


/**
 * The <code>Factory</code> class is used to manage mappings between factory
 * PIDs the configuration PID belonging to it.
 */
class Factory
{

    public static final String FACTORY_PID = "factory.pid";

    public static final String FACTORY_PID_LIST = "factory.pidList";

    /**
     * The {@link ConfigurationManager configuration manager} instance which
     * caused this configuration object to be created.
     */
    private final ConfigurationManager configurationManager;

    // the persistence manager storing this factory mapping
    private final PersistenceManager persistenceManager;

    // the factory PID of this factory
    private final String factoryPid;

    // the bundle location to which factory PID mapping is bound
    private volatile String bundleLocation;

    // whether the factory is statically bound to a bundle or not
    private volatile boolean staticallyBound;

    // the set of configuration PIDs belonging to this factory
    private final Set pids;


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
        this.configurationManager = configurationManager;
        this.persistenceManager = persistenceManager;
        this.factoryPid = factoryPid;
        this.pids = new HashSet();
        this.bundleLocation = null;
        this.staticallyBound = false;
    }


    Factory( ConfigurationManager configurationManager, PersistenceManager persistenceManager, String factoryPid, Dictionary props )
    {
        this( configurationManager, persistenceManager, factoryPid );

        // set bundle location from persistence and/or check for dynamic binding
        this.bundleLocation = ( String ) props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION );
        if ( bundleLocation != null )
        {
            this.staticallyBound = true;
        }
        else
        {
            this.staticallyBound = false;
            this.bundleLocation = configurationManager.getDynamicBundleLocation( getFactoryPid() );
        }

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


    PersistenceManager getPersistenceManager()
    {
        return persistenceManager;
    }


    String getFactoryPid()
    {
        return factoryPid;
    }


    String getBundleLocation()
    {
        return bundleLocation;
    }


    void setBundleLocation( String bundleLocation, boolean staticBinding )
    {
        if ( staticBinding )
        {
            this.bundleLocation = bundleLocation;
            this.staticallyBound = true;

            // 104.15.2.8 The bundle location will be set persistently
            storeSilently();
        }
        else if ( !this.staticallyBound )
        {
            // dynamic binding
            this.bundleLocation = bundleLocation;

            // keep the dynamic binding
            this.configurationManager.setDynamicBundleLocation( this.getFactoryPid(), bundleLocation );
        }
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

        if ( bundleLocation != null && staticallyBound )
        {
            props.put( ConfigurationAdmin.SERVICE_BUNDLELOCATION, this.getBundleLocation() );
        }

        if ( !pids.isEmpty() )
        {
            props.put( FACTORY_PID_LIST, pids.toArray( new String[pids.size()] ) );
        }

        String id = factoryPidToIdentifier( this.getFactoryPid() );
        if ( props.isEmpty() )
        {
            persistenceManager.delete( id );
        }
        else
        {
            props.put( FACTORY_PID, this.getFactoryPid() );
            persistenceManager.store( id, props );
        }
    }


    void storeSilently()
    {
        try
        {
            this.store();
        }
        catch ( IOException ioe )
        {
            configurationManager.log( LogService.LOG_ERROR, "Persisting new bundle location failed", ioe );
        }
    }
}
