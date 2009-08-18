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
package org.apache.felix.cm.integration;


import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanDir;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import junit.framework.TestCase;

import org.apache.felix.cm.integration.helper.MyTinyBundle;
import org.apache.felix.cm.integration.helper.TestActivator;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;


public abstract class ConfigurationTestBase
{

    @Inject
    protected BundleContext bundleContext;

    protected Bundle bundle;

    protected ServiceTracker configAdminTracker;

    protected static final String PROP_NAME = "theValue";
    protected static final Dictionary<String, String> theConfig;

    static
    {
        theConfig = new Hashtable<String, String>();
        theConfig.put( PROP_NAME, PROP_NAME );
    }


    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration()
    {
        return options(
            provision(
                scanDir( "target" ).filter( "*.jar" ),
                mavenBundle( "org.ops4j.pax.swissbox", "pax-swissbox-tinybundles", "1.0.0" )
            )
//          , PaxRunnerOptions.vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=30303" )
        );
    }


    @Before
    public void setUp()
    {
        configAdminTracker = new ServiceTracker( bundleContext, ConfigurationAdmin.class.getName(), null );
        configAdminTracker.open();
    }


    @After
    public void tearDown() throws BundleException
    {
        if ( bundle != null )
        {
            bundle.uninstall();
        }

        configAdminTracker.close();
        configAdminTracker = null;
    }


    protected Bundle installBundle( final String pid ) throws BundleException
    {
        return installBundle( pid, TestActivator.class );
    }


    protected Bundle installBundle( final String pid, final Class<?> activatorClass ) throws BundleException
    {
        final InputStream bundleStream = new MyTinyBundle().prepare(
            withBnd().set( Constants.BUNDLE_SYMBOLICNAME, "simpleconfiguration" ).set( Constants.BUNDLE_VERSION,
                "0.0.11" ).set( Constants.IMPORT_PACKAGE, "org.apache.felix.cm.integration.helper" ).set(
                Constants.BUNDLE_ACTIVATOR, activatorClass.getName() ).set( TestActivator.HEADER_PID, pid ) ).build(
            TinyBundles.asStream() );

        try
        {
            return bundleContext.installBundle( "test:SimpleComponent", bundleStream );
        }
        finally
        {
            try
            {
                bundleStream.close();
            }
            catch ( IOException ioe )
            {
            }
        }
    }


    protected static void delay()
    {
        try
        {
            Thread.sleep( 300 );
        }
        catch ( InterruptedException ie )
        {
            // dont care
        }
    }


    protected Bundle getCmBundle()
    {
        final ServiceReference caref = configAdminTracker.getServiceReference();
        return ( caref == null ) ? null : caref.getBundle();
    }


    protected ConfigurationAdmin getConfigurationAdmin()
    {
        ConfigurationAdmin ca = ( ConfigurationAdmin ) configAdminTracker.getService();
        if ( ca == null )
        {
            TestCase.fail( "Missing ConfigurationAdmin service" );
        }
        return ca;
    }


    protected void configure( String pid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            org.osgi.service.cm.Configuration config = ca.getConfiguration( pid, null );
            config.update( theConfig );
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed updating configuration " + pid + ": " + ioe.toString() );
        }
    }


    protected Configuration getConfiguration( final String pid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            final String filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
            org.osgi.service.cm.Configuration[] configs = ca.listConfigurations( filter );
            if ( configs != null && configs.length > 0 )
            {
                return configs[0];
            }
        }
        catch ( InvalidSyntaxException ise )
        {
            // unexpected
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed listing configurations " + pid + ": " + ioe.toString() );
        }

        TestCase.fail( "No Configuration " + pid + " found" );
        return null;
    }


    protected void deleteConfig( String pid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            org.osgi.service.cm.Configuration config = ca.getConfiguration( pid );
            config.delete();
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed deleting configuration " + pid + ": " + ioe.toString() );
        }
    }


    protected String createFactoryConfiguration( String factoryPid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            org.osgi.service.cm.Configuration config = ca.createFactoryConfiguration( factoryPid, null );
            config.update( theConfig );
            return config.getPid();
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed updating factory configuration " + factoryPid + ": " + ioe.toString() );
            return null;
        }
    }


    protected void deleteFactoryConfigurations( String factoryPid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            final String filter = "(service.factoryPid=" + factoryPid + ")";
            org.osgi.service.cm.Configuration[] configs = ca.listConfigurations( filter );
            if ( configs != null )
            {
                for ( org.osgi.service.cm.Configuration configuration : configs )
                {
                    configuration.delete();
                }
            }
        }
        catch ( InvalidSyntaxException ise )
        {
            // unexpected
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed deleting configurations " + factoryPid + ": " + ioe.toString() );
        }
    }
}
