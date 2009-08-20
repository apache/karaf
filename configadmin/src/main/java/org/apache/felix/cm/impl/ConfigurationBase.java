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
import org.apache.felix.cm.PersistenceManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;


abstract class ConfigurationBase
{

    /**
     * The {@link ConfigurationManager configuration manager} instance which
     * caused this configuration object to be created.
     */
    private final ConfigurationManager configurationManager;

    // the persistence manager storing this factory mapping
    private final PersistenceManager persistenceManager;

    // the basic ID of this instance
    private final String baseId;

    /**
     * The statically bound bundle location, which is set explicitly by calling
     * the Configuration.setBundleLocation(String) method.
     */
    private volatile String staticBundleLocation;

    /**
     * The bundle location from dynamic binding. This value is set as the
     * configuration or factory is assigned to a ManagedService[Factory].
     */
    private volatile String dynamicBundleLocation;

    /**
     * The <code>ServiceReference</code> of the serviceReference which first
     * asked for this instance. This field is <code>null</code> if the instance
     * has not been handed to a serviceReference by way of the
     * <code>ManagedService.update(Dictionary)</code> or
     * <code>ManagedServiceFactory.updated(String, Dictionary)</code> method.
     */
    private volatile ServiceReference serviceReference;


    protected ConfigurationBase( final ConfigurationManager configurationManager,
        final PersistenceManager persistenceManager, final String baseId, final String bundleLocation )
    {
        if ( configurationManager == null )
        {
            throw new IllegalArgumentException( "ConfigurationManager must not be null" );
        }

        if ( persistenceManager == null )
        {
            throw new IllegalArgumentException( "PersistenceManager must not be null" );
        }

        this.configurationManager = configurationManager;
        this.persistenceManager = persistenceManager;
        this.baseId = baseId;

        // set bundle location from persistence and/or check for dynamic binding
        this.staticBundleLocation = bundleLocation;
        this.dynamicBundleLocation = configurationManager.getDynamicBundleLocation( baseId );
    }


    ConfigurationManager getConfigurationManager()
    {
        return configurationManager;
    }


    PersistenceManager getPersistenceManager()
    {
        return persistenceManager;
    }


    String getBaseId()
    {
        return baseId;
    }


    /**
     * Returns the "official" bundle location as visible from the outside
     * world of code calling into the Configuration.getBundleLocation() method.
     * <p>
     * In other words: The {@link #getStaticBundleLocation()} is returned if
     * not <code>null</code>. Otherwise the {@link #getDynamicBundleLocation()}
     * is returned (which may also be <code>null</code>).
     */
    String getBundleLocation()
    {
        if ( staticBundleLocation != null )
        {
            return staticBundleLocation;
        }

        return dynamicBundleLocation;
    }


    /**
     * Returns the bundle location according to actual configuration binding.
     * <p>
     * This may be different from the {@link #getBundleLocation()} result if
     * the <code>Configuration.setBundleLocation(String)</code> method has been
     * called after the configuration has been dynamically bound to a bundle.
     * <p>
     * In other words: The {@link #getDynamicBundleLocation()} is returned if
     * not <code>null</code>. Otherwise the {@link #getStaticBundleLocation()}
     * is returned (which may also be <code>null</code>).
     */
    String getBoundBundleLocation()
    {
        if ( dynamicBundleLocation != null )
        {
            return dynamicBundleLocation;
        }

        return staticBundleLocation;
    }


    String getDynamicBundleLocation()
    {
        return dynamicBundleLocation;
    }


    String getStaticBundleLocation()
    {
        return staticBundleLocation;
    }


    void setServiceReference( ServiceReference serviceReference )
    {
        this.serviceReference = serviceReference;
    }


    ServiceReference getServiceReference()
    {
        return serviceReference;
    }


    void setStaticBundleLocation( final String bundleLocation )
    {
        // 104.15.2.8 The bundle location will be set persistently
        this.staticBundleLocation = bundleLocation;
        storeSilently();

        // FELIX-1488: If a configuration is bound to a location and a new
        // location is statically set, the old binding must be removed
        // by removing the configuration from the targets and the new binding
        // must be setup by updating the configuration for new targets
        /*
         * According to BJ Hargrave configuration is not re-dispatched
         * due to setting the static bundle location.
         * The following code is therefore disabled for now
        if ( ( this instanceof ConfigurationImpl ) && ( bundleLocation != null ) )
        {
            // remove configuration from current managed service [factory]
            if ( getDynamicBundleLocation() != null && !bundleLocation.equals( getDynamicBundleLocation() ) )
            {
                getConfigurationManager().revokeConfiguration( ( ConfigurationImpl ) this );
            }

            // check whether we have to assign the configuration to new targets
            getConfigurationManager().reassignConfiguration( ( ConfigurationImpl ) this );
        }
         *
         */

        // corrollary to not reassigning configurations: we have to check
        // whether the dynamic bundle location is different from the static
        // now. If so, the dynamic bundle location has to be removed.
        // also set the service reference to null because we assume to not
        // be bound to a service anymore
        if ( bundleLocation != null && getDynamicBundleLocation() != null
            && !bundleLocation.equals( getDynamicBundleLocation() ) )
        {
            setDynamicBundleLocation( null );
            setServiceReference( null );
        }
    }


    void setDynamicBundleLocation( final String bundleLocation )
    {
        this.dynamicBundleLocation = bundleLocation;
        this.configurationManager.setDynamicBundleLocation( this.getBaseId(), bundleLocation );

        // FELIX-1488: If a dynamically bound configuration is unbound and not
        // statically bound, it may be rebound to another bundle asking for it
        // (unless the dynamic unbind happens due to configuration deletion)
        /*
         * According to BJ Hargrave configuration is not re-dispatched
         * due to setting the static bundle location.
         * The following code is therefore disabled for now
        if ( bundleLocation == null && getStaticBundleLocation() == null && ( this instanceof ConfigurationImpl ) )
        {
            getConfigurationManager().reassignConfiguration( ( ConfigurationImpl ) this );
        }
         *
         */
    }


    /**
     * Tries to bind this configuration or factory to the given bundle location:
     * <ul>
     * <li>If already dynamically bound, <code>true</code> is returned if the
     * dynamic binding equals the desired binding. Otherwise <code>false</code>
     * is returned.</li>
     * <li>If not dynamically bound but statically bound and the static binding
     * is not equal to the desired binding, <code>false</code> is returned.</li>
     * <li>Otherwise this configuration or factory is dynamically bound to the
     * desired location and <code>true</code> is returned.</li>
     * </ul>
     *
     * @param bundleLocation
     *            The desired bundle location to which this configuration or
     *            factory should be dynamically bound.
     * @return <code>true</code> if this configuration or factory is dynamically
     *         bound to the desired location.
     */
    boolean tryBindLocation( final String bundleLocation )
    {
        if ( this.dynamicBundleLocation != null )
        {
            return this.dynamicBundleLocation.equals( bundleLocation );
        }
        else if ( this.staticBundleLocation != null && !this.staticBundleLocation.equals( bundleLocation ) )
        {
            return false;
        }

        setDynamicBundleLocation( bundleLocation );
        return true;
    }


    abstract void store() throws IOException;


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


    static protected void replaceProperty( Dictionary properties, String key, String value )
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
