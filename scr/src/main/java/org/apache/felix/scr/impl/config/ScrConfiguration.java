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


/**
 * The <code>ScrConfiguration</code> class conveys configuration for the
 * Felix DS implementation bundle. The basic configuration is retrieved from
 * bundle context properties. In addition, this class registers a ManagedService
 * service to receive configuration supplied from the Configuration Admin
 * service overlaying the static context properties.
 *
 * @scr.component ds="false" name="org.apache.felix.scr.ScrService"
 */
public class ScrConfiguration
{

    private static final String VALUE_TRUE = "true";

    private static final String PROP_FACTORY_ENABLED = "ds.factory.enabled";

    private static final String PROP_LOGLEVEL = "ds.loglevel";

    private static final String LOG_LEVEL_DEBUG = "debug";

    private static final String LOG_LEVEL_INFO = "info";

    private static final String LOG_LEVEL_WARN = "warn";

    private static final String LOG_LEVEL_ERROR = "error";

    private static final String PROP_SHOWTRACE = "ds.showtrace";

    private static final String PROP_SHOWERRORS = "ds.showerrors";

    private final BundleContext bundleContext;

    /**
     * @scr.property nameRef="PROP_LOGLEVEL" valueRef="LogService.LOG_ERROR"
     *      type="Integer"
     *      options 4="Debug" 3="Information" 2="Warnings" 1="Error"
     */
    private int logLevel;

    /**
     * @scr.property nameRef="PROP_FACTORY_ENABLED" value="false" type="Boolean"
     */
    private boolean factoryEnabled;


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
            Dictionary props = new Hashtable();
            props.put( Constants.SERVICE_PID, "org.apache.felix.scr.ScrService" );
            bundleContext.registerService( ManagedService.class.getName(), service, props );
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

            logLevel = getLogLevel( bundleContext );
            factoryEnabled = VALUE_TRUE.equals( bundleContext.getProperty( PROP_FACTORY_ENABLED ) );
        }
        else
        {
            logLevel = ( ( Integer ) config.get( PROP_LOGLEVEL ) ).intValue();
            factoryEnabled = ( ( Boolean ) config.get( PROP_FACTORY_ENABLED ) ).booleanValue();
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


    private static int getLogLevel( BundleContext bundleContext )
    {
        String levelString = bundleContext.getProperty( PROP_LOGLEVEL );
        if ( levelString != null )
        {
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
}
