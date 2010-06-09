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
package org.apache.felix.scr.impl.config;


import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeProvider;


/**
 * The <code>ScrConfiguration</code> class conveys configuration for the
 * Felix DS implementation bundle. The basic configuration is retrieved from
 * bundle context properties. In addition, this class registers a ManagedService
 * service to receive configuration supplied from the Configuration Admin
 * service overlaying the static context properties.
 */
public class ScrConfiguration
{

    private static final String VALUE_TRUE = "true";

    static final String PROP_FACTORY_ENABLED = "ds.factory.enabled";

    static final String PROP_LOGLEVEL = "ds.loglevel";

    private static final String LOG_LEVEL_DEBUG = "debug";

    private static final String LOG_LEVEL_INFO = "info";

    private static final String LOG_LEVEL_WARN = "warn";

    private static final String LOG_LEVEL_ERROR = "error";

    private static final String PROP_SHOWTRACE = "ds.showtrace";

    private static final String PROP_SHOWERRORS = "ds.showerrors";

    private final BundleContext bundleContext;

    private int logLevel;

    private boolean factoryEnabled;

    static final String PID = "org.apache.felix.scr.ScrService";

    public ScrConfiguration( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;

        // default configuration
        configure( null );

        // listen for Configuration Admin configuration
        try
        {
            Object service = new ManagedService()
            {
                public void updated( Dictionary properties ) throws ConfigurationException
                {
                    configure( properties );
                }
            };
            // add meta type provider if interfaces are available
            Object enhancedService = tryToCreateMetaTypeProvider(service);
            final String[] interfaceNames;
            if ( enhancedService == null )
            {
                interfaceNames = new String[] {ManagedService.class.getName()};
            }
            else
            {
                interfaceNames = new String[] {ManagedService.class.getName(), MetaTypeProvider.class.getName()};
                service = enhancedService;
            }
            Dictionary props = new Hashtable();
            props.put( Constants.SERVICE_PID, PID );
            bundleContext.registerService( interfaceNames, service, props );
        }
        catch ( Throwable t )
        {
            // don't care
        }
    }

    void configure( Dictionary config )
    {
        if ( config == null )
        {

            logLevel = getDefaultLogLevel();
            factoryEnabled = getDefaultFactoryEnabled();
        }
        else
        {
            logLevel = getLogLevel( config.get( PROP_LOGLEVEL ) );
            factoryEnabled = VALUE_TRUE.equals( String.valueOf( config.get( PROP_FACTORY_ENABLED ) ) );
        }
    }

    public int getLogLevel()
    {
        return logLevel;
    }


    public boolean isFactoryEnabled()
    {
        return factoryEnabled;
    }


    private boolean getDefaultFactoryEnabled() {
        return VALUE_TRUE.equals( bundleContext.getProperty( PROP_FACTORY_ENABLED ) );
    }


    private int getDefaultLogLevel()
    {
        return getLogLevel( bundleContext.getProperty( PROP_LOGLEVEL ) );
    }


    private int getLogLevel( final Object levelObject )
    {
        if ( levelObject != null )
        {
            if ( levelObject instanceof Number )
            {
                return ( ( Number ) levelObject ).intValue();
            }

            String levelString = levelObject.toString();
            try
            {
                return Integer.parseInt( levelString );
            }
            catch ( NumberFormatException nfe )
            {
                // might be a descriptive name
            }

            if ( LOG_LEVEL_DEBUG.equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_DEBUG;
            }
            else if ( LOG_LEVEL_INFO.equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_INFO;
            }
            else if ( LOG_LEVEL_WARN.equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_WARNING;
            }
            else if ( LOG_LEVEL_ERROR.equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_ERROR;
            }
        }

        // check ds.showtrace property
        if ( VALUE_TRUE.equalsIgnoreCase( bundleContext.getProperty( PROP_SHOWTRACE ) ) )
        {
            return LogService.LOG_DEBUG;
        }

        // next check ds.showerrors property
        if ( "false".equalsIgnoreCase( bundleContext.getProperty( PROP_SHOWERRORS ) ) )
        {
            return -1; // no logging at all !!
        }

        // default log level (errors only)
        return LogService.LOG_ERROR;
    }


    private Object tryToCreateMetaTypeProvider( final Object managedService )
    {
        try
        {
            return new MetaTypeProviderImpl( getDefaultLogLevel(), getDefaultFactoryEnabled(),
                ( ManagedService ) managedService );
        } catch (Throwable t)
        {
            // we simply ignore this
        }
        return null;
    }
}
