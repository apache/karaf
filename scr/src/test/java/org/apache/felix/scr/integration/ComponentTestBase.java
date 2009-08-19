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
package org.apache.felix.scr.integration;


import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanDir;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.integration.components.MyTinyBundle;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;


public abstract class ComponentTestBase
{

    @Inject
    protected BundleContext bundleContext;

    protected Bundle bundle;

    protected ServiceTracker scrTracker;

    protected ServiceTracker configAdminTracker;

    protected static final String PROP_NAME = "theValue";
    protected static final Dictionary<String, String> theConfig;

    static
    {
        theConfig = new Hashtable<String, String>();
        theConfig.put( PROP_NAME, PROP_NAME );
    }


    @Configuration
    public static Option[] configuration()
    {
        return options(
            provision(
                scanDir( "target" ).filter( "*.jar" ),
                mavenBundle( "org.ops4j.pax.swissbox", "pax-swissbox-tinybundles", "1.0.0" ),
                mavenBundle( "org.apache.felix", "org.apache.felix.configadmin", "1.0.10" )
             )
//             , PaxRunnerOptions.vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=30303" )
        );
    }


    @Before
    public void setUp() throws BundleException
    {
        scrTracker = new ServiceTracker( bundleContext, ScrService.class.getName(), null );
        scrTracker.open();
        configAdminTracker = new ServiceTracker( bundleContext, ConfigurationAdmin.class.getName(), null );
        configAdminTracker.open();

        //        final InputStream bundleStream = newBundle()
        final InputStream bundleStream = new MyTinyBundle().addResource( "OSGI-INF/components.xml",
            getClass().getResource( "/integration_test_simple_components.xml" ) ).prepare(
            withBnd().set( Constants.BUNDLE_SYMBOLICNAME, "simplecomponent" ).set( Constants.BUNDLE_VERSION, "0.0.11" )
                .set( Constants.IMPORT_PACKAGE, "org.apache.felix.scr.integration.components" ).set(
                    "Service-Component", "OSGI-INF/components.xml" ) ).build( TinyBundles.asStream() );

        bundle = bundleContext.installBundle( "test:SimpleComponent", bundleStream );

        bundle.start();
    }


    @After
    public void tearDown() throws BundleException
    {
        bundle.stop();
        bundle.uninstall();

        configAdminTracker.close();
        configAdminTracker = null;
        scrTracker.close();
        scrTracker = null;
    }


    protected Component findComponentByName( String name )
    {
        ScrService scr = ( ScrService ) scrTracker.getService();
        if ( scr != null )
        {
            Component[] components = scr.getComponents();
            if ( components != null )
            {
                for ( Component component : components )
                {
                    if ( name.equals( component.getName() ) )
                    {
                        return component;
                    }
                }
            }
        }

        return null;
    }


    protected Component[] findComponentsByName( String name )
    {
        ScrService scr = ( ScrService ) scrTracker.getService();
        if ( scr != null )
        {
            List<Component> cList = new ArrayList<Component>();
            Component[] components = scr.getComponents();
            if ( components != null )
            {
                for ( Component component : components )
                {
                    if ( name.equals( component.getName() ) )
                    {
                        cList.add( component );
                    }
                }
            }

            if ( !cList.isEmpty() )
            {
                return cList.toArray( new Component[cList.size()] );
            }
        }

        return null;
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
