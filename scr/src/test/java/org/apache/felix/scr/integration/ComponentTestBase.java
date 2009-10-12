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
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
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

    // the name of the system property providing the bundle file to be installed and tested
    protected static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    // the default bundle jar file name
    protected static final String BUNDLE_JAR_DEFAULT = "target/scr.jar";

    protected static final String PROP_NAME = "theValue";
    protected static final Dictionary<String, String> theConfig;

    // the JVM option to set to enable remote debugging
    protected static final String DEBUG_VM_OPTION = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=30303";

    // the actual JVM option set, extensions may implement a static
    // initializer overwriting this value to have the configuration()
    // method include it when starting the OSGi framework JVM
    protected static String paxRunnerVmOption = null;

    // the descriptor file to use for the installed test bundle
    protected static String descriptorFile = "/integration_test_simple_components.xml";

    static
    {
        theConfig = new Hashtable<String, String>();
        theConfig.put( PROP_NAME, PROP_NAME );
    }


    @Configuration
    public static Option[] configuration()
    {
        final String bundleFileName = System.getProperty( BUNDLE_JAR_SYS_PROP, BUNDLE_JAR_DEFAULT );
        final File bundleFile = new File( bundleFileName );
        if ( !bundleFile.canRead() )
        {
            throw new IllegalArgumentException( "Cannot read from bundle file " + bundleFileName + " specified in the "
                + BUNDLE_JAR_SYS_PROP + " system property" );
        }

        final Option[] base = options(
            provision(
                CoreOptions.bundle( bundleFile.toURI().toString() ),
                mavenBundle( "org.ops4j.pax.swissbox", "pax-swissbox-tinybundles", "1.0.0" ),
                mavenBundle( "org.apache.felix", "org.apache.felix.configadmin", "1.0.10" )
             )
        );
        final Option vmOption = ( paxRunnerVmOption != null ) ? PaxRunnerOptions.vmOption( paxRunnerVmOption ) : null;
        return OptionUtils.combine( base, vmOption );
    }


    @Before
    public void setUp() throws BundleException
    {
        scrTracker = new ServiceTracker( bundleContext, ScrService.class.getName(), null );
        scrTracker.open();
        configAdminTracker = new ServiceTracker( bundleContext, ConfigurationAdmin.class.getName(), null );
        configAdminTracker.open();

        bundle = installBundle( descriptorFile );
        bundle.start();
    }


    @After
    public void tearDown() throws BundleException
    {
        if ( bundle != null && bundle.getState() != Bundle.UNINSTALLED )
        {
            bundle.uninstall();
            bundle = null;
        }

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


    protected static Class<?> getType( Object object, String desiredName )
    {
        Class<?> ccImpl = object.getClass();
        while ( ccImpl != null && !desiredName.equals( ccImpl.getSimpleName() ) )
        {
            ccImpl = ccImpl.getSuperclass();
        }
        if ( ccImpl == null )
        {
            TestCase.fail( "ComponentContext " + object + " is not a " + desiredName );
        }

        return ccImpl;
    }


    protected static Object getFieldValue( Object object, String fieldName )
    {
        try
        {
            final Field m_componentsField = getField( object.getClass(), fieldName );
            return m_componentsField.get( object );
        }
        catch ( Throwable t )
        {
            TestCase.fail( "Cannot get " + fieldName + " from " + object + ": " + t );
            return null; // keep the compiler happy
        }
    }


    protected static Field getField( Class<?> type, String fieldName ) throws NoSuchFieldException
    {
        Field field = type.getDeclaredField( fieldName );
        field.setAccessible( true );
        return field;
    }


    protected Bundle installBundle( final String descriptorFile ) throws BundleException
    {
        final InputStream bundleStream = new MyTinyBundle()
            .addResource( "OSGI-INF/components.xml", getClass().getResource( descriptorFile ) )
            .prepare(
                withBnd()
                .set( Constants.BUNDLE_SYMBOLICNAME, "simplecomponent" )
                .set( Constants.BUNDLE_VERSION, "0.0.11" )
                .set( Constants.IMPORT_PACKAGE, "org.apache.felix.scr.integration.components" )
                .set( "Service-Component", "OSGI-INF/components.xml" )
            )
            .build( TinyBundles.asStream() );

        try
        {
            final String location = "test:SimpleComponent/" + System.currentTimeMillis();
            return bundleContext.installBundle( location, bundleStream );
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

}
