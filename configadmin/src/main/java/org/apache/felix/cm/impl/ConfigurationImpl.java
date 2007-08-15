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
import java.util.Hashtable;

import org.apache.felix.cm.PersistenceManager;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
implementation of the Configuration
 * Admin Service Specification <i>Configuration object</i> (section 104.4).
 *
 * @author fmeschbe
 */
class ConfigurationImpl
{

    /**
     * The {@link ConfigurationManager configuration manager} instance which
     * caused this configuration object to be created.
     */
    private ConfigurationManager configurationManager;

    /**
     * The {@link PersistenceManager persistence manager} which loaded this
     * configuration instance and which is used to store and delete configuration
     * data.
     */
    private PersistenceManager persistenceManager;

    /**
     * The serviceReference PID of this configuration.
     */
    private String pid;

    /**
     * The factory serviceReference PID of this configuration or <code>null</code> if this
     * is not a factory configuration.
     */
    private String factoryPID;

    /**
     * The location of the bundle to which this configuration instance is bound.
     * This is not necessarily the same as the bundle of the
     * {@link #configurationAdmin}. If this configuration is not bound to a
     * bundle, this field is <code>null</code>.
     */
    private String bundleLocation;

    /**
     * The <code>ServiceReference</code> of the serviceReference which first asked for
     * this configuration. This field is <code>null</code> if the configuration
     * has not been handed to a serviceReference by way of the <code>ManagedService.update(Dictionary)</code>
     * or <code>ManagedServiceFactory.updated(String, Dictionary)</code>
     * method.
     */
    private ServiceReference serviceReference;

    /**
     * The configuration data of this configuration instance. This is a private
     * copy of the properties of which a copy is made when the
     * {@link #getProperties()} method is called. This field is <code>null</code> if
     * the configuration has been created and never stored to persistence.
     */
    private CaseInsensitiveDictionary properties;


    ConfigurationImpl( ConfigurationManager configurationManager, PersistenceManager persistenceManager,
        Dictionary properties )
    {
        this.configurationManager = configurationManager;
        this.persistenceManager = persistenceManager;

        this.pid = ( String ) properties.remove( Constants.SERVICE_PID );
        this.factoryPID = ( String ) properties.remove( ConfigurationAdmin.SERVICE_FACTORYPID );
        this.bundleLocation = ( String ) properties.remove( ConfigurationAdmin.SERVICE_BUNDLELOCATION );

        this.configure( properties );
    }


    ConfigurationImpl( ConfigurationManager configurationManager, PersistenceManager persistenceManager, String pid,
        String factoryPid )
    {
        this.configurationManager = configurationManager;
        this.persistenceManager = persistenceManager;
        this.pid = pid;
        this.factoryPID = factoryPid;
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#delete()
     */
    public void delete() throws IOException
    {
        if ( !this.isDeleted() )
        {
            this.persistenceManager.delete( this.pid );
            this.persistenceManager = null;

            this.configurationManager.deleted( this );
        }
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#getPid()
     */
    public String getPid()
    {
        return this.pid;
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#getFactoryPid()
     */
    public String getFactoryPid()
    {
        return this.factoryPID;
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#getBundleLocation()
     */
    public String getBundleLocation()
    {
        return this.bundleLocation;
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#getProperties()
     */
    public Dictionary getProperties()
    {
        // no properties yet
        if ( this.properties == null )
        {
            return null;
        }

        CaseInsensitiveDictionary props = new CaseInsensitiveDictionary( this.properties );

        // fix special properties (pid, factory PID, bundle location)
        this.setAutoProperties( props, false );

        return props;
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#setBundleLocation(java.lang.String)
     */
    public void setBundleLocation( String bundleLocation )
    {
        if ( !this.isDeleted() )
        {
            this.bundleLocation = bundleLocation;

            // 104.15.2.8 The bundle location will be set persistently
            try
            {
                this.store();
            }
            catch ( IOException ioe )
            {
                this.configurationManager.log( LogService.LOG_ERROR, "Persisting new bundle location failed", ioe );
            }
        }
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#update()
     */
    public void update() throws IOException
    {
        if ( !this.isDeleted() )
        {
            // read configuration from persistence (again)
            Dictionary properties = this.persistenceManager.load( this.pid );

            // ensure serviceReference pid
            String servicePid = ( String ) properties.get( Constants.SERVICE_PID );
            if ( servicePid != null && !this.pid.equals( servicePid ) )
            {
                throw new IOException( "PID of configuration file does match requested PID; expected " + this.pid + ", got "
                    + servicePid );
            }

            this.configure( properties );

            this.configurationManager.updated( this );
        }
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#update(java.util.Dictionary)
     */
    public void update( Dictionary properties ) throws IOException
    {
        if ( !this.isDeleted() )
        {
            CaseInsensitiveDictionary newProperties = new CaseInsensitiveDictionary( properties );

            this.setAutoProperties( newProperties, true );

            this.persistenceManager.store( this.pid, newProperties );

            this.configure( newProperties );

            this.configurationManager.updated( this );
        }
    }


    //---------- Object overwrites --------------------------------------------

    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( obj instanceof Configuration )
        {
            return this.pid.equals( ( ( Configuration ) obj ).getPid() );
        }

        return false;
    }


    public int hashCode()
    {
        return this.pid.hashCode();
    }


    public String toString()
    {
        return "Configuration PID=" + this.pid + ", factoryPID=" + this.factoryPID + ", bundleLocation=" + this.bundleLocation;
    }


    //---------- private helper -----------------------------------------------

    void setServiceReference( ServiceReference serviceReference )
    {
        this.serviceReference = serviceReference;
    }


    ServiceReference getServiceReference()
    {
        return this.serviceReference;
    }


    void store() throws IOException
    {
        Dictionary props = this.getProperties();

        // if this is a new configuration, we just use an empty Dictionary
        if ( props == null )
        {
            props = new Hashtable();

            // add automatic properties including the bundle location (if set)
            this.setAutoProperties( props, true );
        }
        else if ( this.getBundleLocation() != null )
        {
            props.put( ConfigurationAdmin.SERVICE_BUNDLELOCATION, this.getBundleLocation() );
        }

        // only store now, if this is not a new configuration
        this.persistenceManager.store( this.pid, props );
    }


    boolean isDeleted()
    {
        if ( this.persistenceManager != null )
        {
            if ( this.properties == null || this.persistenceManager.exists( this.pid ) )
            {
                return false;
            }

            this.persistenceManager = null;
        }

        return true;
    }


    private void configure( Dictionary properties )
    {
        // remove predefined properties
        this.clearAutoProperties( properties );

        // ensure CaseInsensitiveDictionary
        if ( properties instanceof CaseInsensitiveDictionary )
        {
            this.properties = ( CaseInsensitiveDictionary ) properties;
        }
        else
        {
            this.properties = new CaseInsensitiveDictionary( properties );
        }
    }


    void setAutoProperties( Dictionary properties, boolean withBundleLocation )
    {
        // set pid and factory pid in the properties
        this.replaceProperty( properties, Constants.SERVICE_PID, this.pid );
        this.replaceProperty( properties, ConfigurationAdmin.SERVICE_FACTORYPID, this.factoryPID );

        // bundle location is not set here
        if ( withBundleLocation )
        {
            this.replaceProperty( properties, ConfigurationAdmin.SERVICE_BUNDLELOCATION, this.getBundleLocation() );
        }
        else
        {
            properties.remove( ConfigurationAdmin.SERVICE_BUNDLELOCATION );
        }
    }


    void clearAutoProperties( Dictionary properties )
    {
        properties.remove( Constants.SERVICE_PID );
        properties.remove( ConfigurationAdmin.SERVICE_FACTORYPID );
        properties.remove( ConfigurationAdmin.SERVICE_BUNDLELOCATION );
    }


    private void replaceProperty( Dictionary properties, String key, String value )
    {
        if ( value == null )
        {
            properties.remove( key );
        }
        else
        {
            properties.put( key, value );
        }
    }
}
