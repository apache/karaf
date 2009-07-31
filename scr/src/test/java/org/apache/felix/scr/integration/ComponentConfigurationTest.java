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
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;


@RunWith(JUnit4TestRunner.class)
public class ComponentConfigurationTest
{

    @Inject
    private BundleContext bundleContext;

    private Bundle bundle;

    private ServiceTracker scrTracker;

    private ServiceTracker configAdminTracker;

    private static final String PROP_NAME = "theValue";
    private static final Dictionary<String, String> theConfig;

    static
    {
        theConfig = new Hashtable<String, String>();
        theConfig.put( PROP_NAME, PROP_NAME );
    }


    @Configuration
    public static Option[] configuration()
    {
        return options( provision( scanDir( "target" ).filter( "*.jar" ), mavenBundle( "org.ops4j.pax.swissbox",
            "pax-swissbox-tinybundles", "1.0.0" ), mavenBundle( "org.apache.felix", "org.apache.felix.configadmin",
            "1.0.10" ) )
        //            , PaxRunnerOptions.vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=30303" )
        //            PaxRunnerOptions.timeout( 0 )

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


    @Test
    public void test_SimpleComponent_configuration_ignore()
    {
        test_configuration_ignore( "SimpleComponent" );
    }


    @Test
    public void test_SimpleComponent_configuration_optional()
    {
        test_configuration_optional( "SimpleComponent" );
    }


    @Test
    public void test_SimpleComponent_configuration_require()
    {
        test_configuration_require( "SimpleComponent" );
    }


    @Test
    public void test_SimpleComponent_dynamic_configuration()
    {
        test_dynamic_configuration( "DynamicConfigurationComponent" );
    }


    @Test
    public void test_SimpleComponent_factory_configuration()
    {
        test_factory_configuration( "FactoryConfigurationComponent" );
    }


    @Test
    public void test_SimpleComponent_service()
    {
        test_service( "ServiceComponent" );
    }


    private void test_configuration_ignore( final String componentName )
    {
        final String pid = componentName + ".configuration.ignore";
        final Component component = findComponentByName( pid );

        deleteConfig( pid );
        delay();

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        configure( pid );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        deleteConfig( pid );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        component.disable();
        delay();

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }


    private void test_configuration_optional( final String componentName )
    {
        final String pid = componentName + ".configuration.optional";
        final Component component = findComponentByName( pid );

        deleteConfig( pid );
        delay();

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        configure( pid );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        deleteConfig( pid );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        component.disable();
        delay();

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }


    private void test_configuration_require( final String componentName )
    {
        final String pid = componentName + ".configuration.require";
        final Component component = findComponentByName( pid );

        deleteConfig( pid );
        delay();

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        configure( pid );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        deleteConfig( pid );
        delay();

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.disable();
        delay();

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }


    private void test_dynamic_configuration( final String componentName )
    {
        final String pid = componentName;
        final Component component = findComponentByName( pid );

        deleteConfig( pid );
        delay();

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        final SimpleComponent instance = SimpleComponent.INSTANCE;

        configure( pid );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertEquals( instance, SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        deleteConfig( pid );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertEquals( instance, SimpleComponent.INSTANCE );
        TestCase.assertNull( SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        component.disable();
        delay();

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }


    private void test_factory_configuration( final String componentName )
    {
        final String factoryPid = componentName;

        deleteFactoryConfigurations( factoryPid );
        delay();

        // one single component exists without configuration
        final Component[] noConfigurations = findComponentsByName( factoryPid );
        TestCase.assertNotNull( noConfigurations );
        TestCase.assertEquals( 1, noConfigurations.length );
        TestCase.assertEquals( Component.STATE_DISABLED, noConfigurations[0].getState() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.isEmpty() );

        // enable the component, configuration required, hence unsatisfied
        noConfigurations[0].enable();
        delay();

        final Component[] enabledNoConfigs = findComponentsByName( factoryPid );
        TestCase.assertNotNull( enabledNoConfigs );
        TestCase.assertEquals( 1, enabledNoConfigs.length );
        TestCase.assertEquals( Component.STATE_UNSATISFIED, enabledNoConfigs[0].getState() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.isEmpty() );

        // create two factory configurations expecting two components
        final String pid0 = createFactoryConfiguration( factoryPid );
        final String pid1 = createFactoryConfiguration( factoryPid );
        delay();

        // expect two components, only first is active, second is disabled
        final Component[] twoConfigs = findComponentsByName( factoryPid );
        TestCase.assertNotNull( twoConfigs );
        TestCase.assertEquals( 2, twoConfigs.length );
        TestCase.assertEquals( Component.STATE_ACTIVE, twoConfigs[0].getState() );
        TestCase.assertEquals( Component.STATE_DISABLED, twoConfigs[1].getState() );
        TestCase.assertEquals( 1, SimpleComponent.INSTANCES.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( pid0 ) );
        TestCase.assertFalse( SimpleComponent.INSTANCES.containsKey( pid1 ) );

        // enable second component
        twoConfigs[1].enable();
        delay();

        // ensure both components active
        TestCase.assertEquals( Component.STATE_ACTIVE, twoConfigs[0].getState() );
        TestCase.assertEquals( Component.STATE_ACTIVE, twoConfigs[1].getState() );
        TestCase.assertEquals( 2, SimpleComponent.INSTANCES.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( pid0 ) );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( pid1 ) );

        // delete a configuration
        deleteConfig( pid0 );
        delay();

        // expect one component
        final Component[] oneConfig = findComponentsByName( factoryPid );
        TestCase.assertNotNull( oneConfig );
        TestCase.assertEquals( 1, oneConfig.length );
        TestCase.assertEquals( Component.STATE_ACTIVE, oneConfig[0].getState() );
        TestCase.assertEquals( 1, SimpleComponent.INSTANCES.size() );
        TestCase.assertFalse( SimpleComponent.INSTANCES.containsKey( pid0 ) );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( pid1 ) );

        // delete second configuration
        deleteConfig( pid1 );
        delay();

        // expect a single unsatisifed component
        final Component[] configsDeleted = findComponentsByName( factoryPid );
        TestCase.assertNotNull( configsDeleted );
        TestCase.assertEquals( 1, configsDeleted.length );
        TestCase.assertEquals( Component.STATE_UNSATISFIED, configsDeleted[0].getState() );
        TestCase.assertEquals( 0, SimpleComponent.INSTANCES.size() );
        TestCase.assertFalse( SimpleComponent.INSTANCES.containsKey( pid0 ) );
        TestCase.assertFalse( SimpleComponent.INSTANCES.containsKey( pid1 ) );

    }


    private void test_service( final String componentName )
    {
        final String pid = componentName;

        // one single component exists without configuration
        final Component component = findComponentByName( pid );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        final SimpleComponent instance = SimpleComponent.INSTANCE;
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( instance );

        // assert component properties (all !)
        TestCase.assertEquals( "required", instance.getProperty( "prop.public" ) );
        TestCase.assertEquals( "private", instance.getProperty( ".prop.private" ) );

        // get the service
        ServiceReference reference = bundleContext.getServiceReference( "java.lang.Object" );
        TestCase.assertNotNull( reference );
        try
        {
            TestCase.assertEquals( instance, bundleContext.getService( reference ) );
        }
        finally
        {
            bundleContext.ungetService( reference );
        }

        // check service properties
        TestCase.assertEquals( "required", reference.getProperty( "prop.public" ) );
        TestCase.assertNull( reference.getProperty( ".prop.private" ) );

        // check property keys do not contain private keys
        for ( String propKey : reference.getPropertyKeys() )
        {
            TestCase.assertTrue( "Property key [" + propKey
                + "] must have at least one character and not start with a dot", propKey.length() > 0
                && !propKey.startsWith( "." ) );
        }
    }


    private Component findComponentByName( String name )
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


    private Component[] findComponentsByName( String name )
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


    private static void delay()
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


    private ConfigurationAdmin getConfigurationAdmin()
    {
        ConfigurationAdmin ca = ( ConfigurationAdmin ) configAdminTracker.getService();
        if ( ca == null )
        {
            TestCase.fail( "Missing ConfigurationAdmin service" );
        }
        return ca;
    }


    private void configure( String pid )
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


    private void deleteConfig( String pid )
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


    private String createFactoryConfiguration( String factoryPid )
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


    private void deleteFactoryConfigurations( String factoryPid )
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
