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

/**
-* The <code>ConfigurationImpl</code> is the implementation of the Configuration * Admin Service Specification <i>Configuration object</i> (section 104.4).
 *
 * @author fmeschbe
 */
class ConfigurationImpl
{

    /**
     * The name of a synthetic property stored in the persisted configuration
     * data to indicate that the configuration data is new, that is created but
     * never updated (value is "_felix_.cm.newConfiguration").
     * <p>
     * This special property is stored by the
     * {@link #ConfigurationImpl(ConfigurationManager, PersistenceManager, String, String, String)}
     * constructor, when the configuration is first created and persisted and is
     * interpreted by the
     * {@link #ConfigurationImpl(ConfigurationManager, PersistenceManager, Dictionary)}
     * method when the configuration data is loaded in a new object.
     * <p>
     * The goal of this property is to keep the information on whether
     * configuration data is new (but persisted as per the spec) or has already
     * been assigned with possible no data.
     */
    private static final String CONFIGURATION_NEW = "_felix_.cm.newConfiguration";
    
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
     * the configuration has been created and never been updated with acutal
     * configuration properties.
     */
    private CaseInsensitiveDictionary properties;

    /**
     * Flag indicating that this configuration object has been delivered to the
     * owning ManagedService[Factory] after the last update or after being
     * deleted. This flag is set by the {@link #delete()}, {@link #update()}
     * and {@link #update(Dictionary)} method and must be checked when this
     * instance is about to be delivered and reset after the configuration has
     * been delivered.
     * @see #setDelivered(boolean)
     * @see #isDelivered()
     */
    private boolean delivered;

    ConfigurationImpl( ConfigurationManager configurationManager, PersistenceManager persistenceManager,
        Dictionary properties )
    {
        this.configurationManager = configurationManager;
        this.persistenceManager = persistenceManager;

        pid = ( String ) properties.remove( Constants.SERVICE_PID );
        factoryPID = ( String ) properties.remove( ConfigurationAdmin.SERVICE_FACTORYPID );
        bundleLocation = ( String ) properties.remove( ConfigurationAdmin.SERVICE_BUNDLELOCATION );

        // set the properties internally
        configureFromPersistence( properties );
    }


    ConfigurationImpl( ConfigurationManager configurationManager, PersistenceManager persistenceManager, String pid,
        String factoryPid, String bundleLocation ) throws IOException
    {
        this.configurationManager = configurationManager;
        this.persistenceManager = persistenceManager;
        this.pid = pid;
        this.factoryPID = factoryPid;
        this.bundleLocation = bundleLocation;
        this.properties = null;

        // this is a new configuration object, store immediately unless
        // the new configuration object is created from a factory, in which
        // case the configuration is only stored when first updated
        if (factoryPid == null) {
            Dictionary props = new Hashtable();
            setAutoProperties( props, true );
            props.put( CONFIGURATION_NEW, Boolean.TRUE );
            persistenceManager.store( pid, props );
        }
    }


    /**
     * Sets whether the last change (update, delete) to this instance should be
     * assumed as being delivered to the owning ManagedService[Factory] or not.
     */
    void setDelivered( boolean delivered )
    {
        this.delivered = delivered;
    }


    /**
     * Returns whether the last change (update, delete) to this instance should
     * be assumed as being delivered to the owning ManagedService[Factory] or
     * not.
     */
    boolean isDelivered()
    {
        return delivered;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.Configuration#delete()
     */
    public void delete() throws IOException
    {
        if ( !isDeleted() )
        {
            persistenceManager.delete( pid );
            persistenceManager = null;

            // ensure configuration is being delivered
            setDelivered( false );
            
            configurationManager.deleted( this );
        }
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#getPid()
     */
    public String getPid()
    {
        return pid;
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#getFactoryPid()
     */
    public String getFactoryPid()
    {
        return factoryPID;
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#getBundleLocation()
     */
    public String getBundleLocation()
    {
        return bundleLocation;
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#getProperties()
     */
    public Dictionary getProperties()
    {
        // no properties yet
        if ( properties == null )
        {
            return null;
        }

        CaseInsensitiveDictionary props = new CaseInsensitiveDictionary( properties );

        // fix special properties (pid, factory PID, bundle location)
        setAutoProperties( props, false );

        return props;
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#setBundleLocation(java.lang.String)
     */
    public void setBundleLocation( String bundleLocation )
    {
        if ( !isDeleted() )
        {
            this.bundleLocation = bundleLocation;

            // 104.15.2.8 The bundle location will be set persistently
            try
            {
                store();
            }
            catch ( IOException ioe )
            {
                configurationManager.log( LogService.LOG_ERROR, "Persisting new bundle location failed", ioe );
            }
        }
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#update()
     */
    public void update() throws IOException
    {
        if ( !isDeleted() )
        {
            // read configuration from persistence (again)
            Dictionary properties = persistenceManager.load( pid );

            // ensure serviceReference pid
            String servicePid = ( String ) properties.get( Constants.SERVICE_PID );
            if ( servicePid != null && !pid.equals( servicePid ) )
            {
                throw new IOException( "PID of configuration file does match requested PID; expected " + pid + ", got "
                    + servicePid );
            }

            configureFromPersistence( properties );

            // ensure configuration is being delivered
            setDelivered( false );
            
            configurationManager.updated( this );
        }
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.Configuration#update(java.util.Dictionary)
     */
    public void update( Dictionary properties ) throws IOException
    {
        if ( !isDeleted() )
        {
            CaseInsensitiveDictionary newProperties = new CaseInsensitiveDictionary( properties );

            setAutoProperties( newProperties, true );

            persistenceManager.store( pid, newProperties );

            configure( newProperties );
            
            // if this is a factory configuration, update the factory with
            String factoryPid = getFactoryPid();
            if ( factoryPid != null )
            {
                Factory factory = configurationManager.getFactory( factoryPid );
                if ( factory.addPID( getPid() ) )
                {
                    // only write back if the pid was not already registered
                    // with the factory
                    factory.store();
                }
            }

            // ensure configuration is being delivered
            setDelivered( false );
            
            configurationManager.updated( this );
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
            return pid.equals( ( ( Configuration ) obj ).getPid() );
        }

        return false;
    }


    public int hashCode()
    {
        return pid.hashCode();
    }


    public String toString()
    {
        return "Configuration PID=" + pid + ", factoryPID=" + factoryPID + ", bundleLocation=" + bundleLocation;
    }


    //---------- private helper -----------------------------------------------

    void setServiceReference( ServiceReference serviceReference )
    {
        this.serviceReference = serviceReference;
    }


    ServiceReference getServiceReference()
    {
        return serviceReference;
    }


    void store() throws IOException
    {
        Dictionary props = getProperties();

        // if this is a new configuration, we just use an empty Dictionary
        if ( props == null )
        {
            props = new Hashtable();

            // add automatic properties including the bundle location (if set)
            setAutoProperties( props, true );
        }
        else if ( getBundleLocation() != null )
        {
            props.put( ConfigurationAdmin.SERVICE_BUNDLELOCATION, getBundleLocation() );
        }

        // only store now, if this is not a new configuration
        persistenceManager.store( pid, props );
    }


    /**
     * Returns <code>false</code> if this configuration contains configuration
     * properties. Otherwise <code>true</code> is returned and this is a
     * newly creted configuration object whose {@link #update(Dictionary)}
     * method has never been called.
     */
    boolean isNew()
    {
        return properties == null;
    }


    boolean isDeleted()
    {
        if ( persistenceManager != null )
        {
            if ( properties == null || persistenceManager.exists( pid ) )
            {
                return false;
            }

            persistenceManager = null;
        }

        return true;
    }


    private void configureFromPersistence( Dictionary properties )
    {
        // if the this is not an empty/new configuration, accept the properties
        // otherwise just set the properties field to null
        if ( properties.get( CONFIGURATION_NEW ) == null )
        {
            configure( properties );
        }
        else
        {
            this.properties = null;
        }
    }
    
    private void configure( Dictionary properties )
    {
        // remove predefined properties
        clearAutoProperties( properties );

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
        replaceProperty( properties, Constants.SERVICE_PID, pid );
        replaceProperty( properties, ConfigurationAdmin.SERVICE_FACTORYPID, factoryPID );

        // bundle location is not set here
        if ( withBundleLocation )
        {
            replaceProperty( properties, ConfigurationAdmin.SERVICE_BUNDLELOCATION, getBundleLocation() );
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
