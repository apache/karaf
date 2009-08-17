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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
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


@RunWith(JUnit4TestRunner.class)
public class ConfigurationTest
{

    @Inject
    private BundleContext bundleContext;

    private Bundle bundle;

    private ServiceTracker configAdminTracker;

    private static final String PROP_NAME = "theValue";
    private static final Dictionary<String, String> theConfig;

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
        if (bundle != null) {
            bundle.uninstall();
        }

        configAdminTracker.close();
        configAdminTracker = null;
    }


    @Test
    public void test_configuration_unbound_on_uninstall() throws BundleException
    {
        String pid = "test.pid";
        configure( pid );

        // ensure configuration is unbound
        final Configuration beforeInstall = getConfiguration( pid );
        TestCase.assertNull( beforeInstall.getBundleLocation() );

        bundle = installBundle( pid );

        // ensure no configuration bound before start
        final Configuration beforeStart = getConfiguration( pid );
        TestCase.assertNull( beforeInstall.getBundleLocation() );
        TestCase.assertNull( beforeStart.getBundleLocation() );

        bundle.start();
        final TestActivator tester = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numUpdatedCalls );

        // ensure a freshly retrieved object also has the location
        final Configuration beforeStop = getConfiguration( pid );
        TestCase.assertEquals( beforeStop.getBundleLocation(), bundle.getLocation() );

        // check whether bundle context is set on first configuration
        TestCase.assertEquals( beforeInstall.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStart.getBundleLocation(), bundle.getLocation() );

        bundle.stop();

        delay();

        // ensure configuration still bound
        TestCase.assertEquals( beforeInstall.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStart.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStop.getBundleLocation(), bundle.getLocation() );

        // ensure a freshly retrieved object also has the location
        final Configuration beforeUninstall = getConfiguration( pid );
        TestCase.assertEquals( beforeUninstall.getBundleLocation(), bundle.getLocation() );

        bundle.uninstall();
        bundle = null;

        delay();

        // ensure configuration is not bound any more
        TestCase.assertNull( beforeInstall.getBundleLocation() );
        TestCase.assertNull( beforeStart.getBundleLocation() );
        TestCase.assertNull( beforeStop.getBundleLocation() );
        TestCase.assertNull( beforeUninstall.getBundleLocation() );

        // ensure a freshly retrieved object also does not have the location
        final Configuration atEnd = getConfiguration( pid );
        TestCase.assertNull( atEnd.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_multi_value_pid_array() throws BundleException
    {
        final String pid1 = "test.pid.1";
        final String pid2 = "test.pid.2";

        configure( pid1 );
        configure( pid2 );

        final Configuration config1 = getConfiguration( pid1 );
        TestCase.assertEquals( pid1, config1.getPid() );
        TestCase.assertNull( config1.getBundleLocation() );

        final Configuration config2 = getConfiguration( pid2 );
        TestCase.assertEquals( pid2, config2.getPid() );
        TestCase.assertNull( config2.getBundleLocation() );

        // multi-pid with array
        bundle = installBundle( pid1 + "," + pid2 );
        bundle.start();

        // give cm time for distribution
        delay();

        final TestActivator tester = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 2, tester.numUpdatedCalls );

        TestCase.assertEquals( bundle.getLocation(), config1.getBundleLocation() );
        TestCase.assertEquals( bundle.getLocation(), config2.getBundleLocation() );

        bundle.uninstall();
        bundle = null;

        delay();

        TestCase.assertNull( config1.getBundleLocation() );
        TestCase.assertNull( config2.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid1 );
        deleteConfig( pid2 );
    }


    @Test
    public void test_multi_value_pid_collection() throws BundleException
    {
        String pid1 = "test.pid.1";
        String pid2 = "test.pid.2";

        configure( pid1 );
        configure( pid2 );

        final Configuration config1 = getConfiguration( pid1 );
        TestCase.assertEquals( pid1, config1.getPid() );
        TestCase.assertNull( config1.getBundleLocation() );

        final Configuration config2 = getConfiguration( pid2 );
        TestCase.assertEquals( pid2, config2.getPid() );
        TestCase.assertNull( config2.getBundleLocation() );

        // multi-pid with collection
        bundle = installBundle( pid1 + ";" + pid2 );
        bundle.start();

        // give cm time for distribution
        delay();

        final TestActivator tester = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 2, tester.numUpdatedCalls );

        TestCase.assertEquals( bundle.getLocation(), config1.getBundleLocation() );
        TestCase.assertEquals( bundle.getLocation(), config2.getBundleLocation() );

        bundle.uninstall();
        bundle = null;

        delay();

        TestCase.assertNull( config1.getBundleLocation() );
        TestCase.assertNull( config2.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid1 );
        deleteConfig( pid2 );
    }


    @Test
    public void test_configuration_unbound_on_uninstall_with_cm_restart() throws BundleException
    {
        final String pid = "test.pid";
        configure( pid );
        final Bundle cmBundle = getCmBundle();

        // ensure configuration is unbound
        final Configuration beforeInstall = getConfiguration( pid );
        TestCase.assertNull( beforeInstall.getBundleLocation() );

        bundle = installBundle( pid );

        // ensure no configuration bound before start
        final Configuration beforeStart = getConfiguration( pid );
        TestCase.assertNull( beforeInstall.getBundleLocation() );
        TestCase.assertNull( beforeStart.getBundleLocation() );

        bundle.start();
        final TestActivator tester = TestActivator.INSTANCE;
        TestCase.assertNotNull( "IOActivator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numUpdatedCalls );

        // ensure a freshly retrieved object also has the location
        final Configuration beforeStop = getConfiguration( pid );
        TestCase.assertEquals( beforeStop.getBundleLocation(), bundle.getLocation() );

        // check whether bundle context is set on first configuration
        TestCase.assertEquals( beforeInstall.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStart.getBundleLocation(), bundle.getLocation() );

        bundle.stop();

        // ensure configuration still bound
        TestCase.assertEquals( beforeInstall.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStart.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStop.getBundleLocation(), bundle.getLocation() );

        // ensure a freshly retrieved object also has the location
        final Configuration beforeUninstall = getConfiguration( pid );
        TestCase.assertEquals( beforeUninstall.getBundleLocation(), bundle.getLocation() );

        // stop cm bundle now before uninstalling configured bundle
        cmBundle.stop();
        delay();

        // assert configuration admin service is gone
        TestCase.assertNull( configAdminTracker.getService() );

        // uninstall bundle while configuration admin is stopped
        bundle.uninstall();
        bundle = null;

        // start cm bundle again after uninstallation
        cmBundle.start();
        delay();

        // ensure a freshly retrieved object also does not have the location
        // FELIX-1484: this test fails due to bundle location not verified
        //    at first configuration access
        final Configuration atEnd = getConfiguration( pid );
        TestCase.assertNull( atEnd.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_not_updated_new_configuration_not_bound_after_bundle_uninstall() throws IOException, BundleException
    {
        final String pid = "test_not_updated_new_configuration_not_bound_after_bundle_uninstall";

        // create a configuration but do not update with properties
        final ConfigurationAdmin ca = getConfigurationAdmin();
        final Configuration newConfig = ca.getConfiguration( pid, null );
        TestCase.assertNull( newConfig.getProperties() );
        TestCase.assertNull( newConfig.getBundleLocation() );

        // start and settle bundle
        bundle = installBundle( pid );
        bundle.start();
        delay();

        // ensure no properties provided to bundle
        final TestActivator tester = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );
        TestCase.assertNull( "Expect no properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numUpdatedCalls );

        // assert configuration is still unset but bound
        TestCase.assertNull( newConfig.getProperties() );
        TestCase.assertEquals( bundle.getLocation(), newConfig.getBundleLocation() );

        // uninstall bundle, should unbind configuration
        bundle.uninstall();
        bundle = null;

        delay();

        // assert configuration is still unset and unbound
        TestCase.assertNull( newConfig.getProperties() );
        TestCase.assertNull( newConfig.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_start_bundle_configure_stop_start_bundle() throws BundleException
    {
        String pid = "test_start_bundle_configure_stop_start_bundle";

        // start the bundle and assert this
        bundle = installBundle( pid );
        bundle.start();
        final TestActivator tester = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has no configuration
        TestCase.assertNull( "Expect no Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect no update call", 1, tester.numUpdatedCalls );

        // configure after ManagedServiceRegistration --> configure via update
        configure( pid );
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 2, tester.numUpdatedCalls );

        // stop the bundle now
        bundle.stop();

        // assert INSTANCE is null
        TestCase.assertNull( TestActivator.INSTANCE );

        delay();

        // start the bundle again (and check)
        bundle.start();
        final TestActivator tester2 = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started the second time!!", tester2 );
        TestCase.assertNotSame( "Instances must not be the same", tester, tester2 );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester2.props );
        TestCase.assertEquals( "Expect a second update call", 1, tester2.numUpdatedCalls );

        // cleanup
        bundle.uninstall();
        bundle = null;

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_configure_start_bundle_stop_start_bundle() throws BundleException
    {
        String pid = "test_configure_start_bundle_stop_start_bundle";
        configure( pid );

        // start the bundle and assert this
        bundle = installBundle( pid );
        bundle.start();
        final TestActivator tester = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect no update call", 1, tester.numUpdatedCalls );

        // stop the bundle now
        bundle.stop();

        // assert INSTANCE is null
        TestCase.assertNull( TestActivator.INSTANCE );

        delay();

        // start the bundle again (and check)
        bundle.start();
        final TestActivator tester2 = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started the second time!!", tester2 );
        TestCase.assertNotSame( "Instances must not be the same", tester, tester2 );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester2.props );
        TestCase.assertEquals( "Expect a second update call", 1, tester2.numUpdatedCalls );

        // cleanup
        bundle.uninstall();
        bundle = null;

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_create_with_location_unbind_before_service_supply() throws BundleException, IOException
    {

        /*
         * 1. create Configuration with pid and non-null location.
         * 2. update the configuration with non-null props.
         * 3. set location of the configuration to null.
         * 4. bundleA registers a ManagedService service with the pid.
         */

        final String pid = "test_create_with_location_unbind_before_service_supply";
        final String dummyLocation = "http://some/dummy/location";

        // 1. create and statically bind the configuration
        final ConfigurationAdmin ca = getConfigurationAdmin();
        final Configuration config = ca.getConfiguration( pid, dummyLocation );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertEquals( dummyLocation, config.getBundleLocation() );

        // 2. update configuration
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME, PROP_NAME );
        config.update(props);
        TestCase.assertEquals( PROP_NAME, config.getProperties().get( PROP_NAME ) );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertEquals( dummyLocation, config.getBundleLocation() );

        // 3. (statically) set location to null
        config.setBundleLocation( null );
        TestCase.assertNull( config.getBundleLocation() );

        // 4. install bundle with service
        bundle = installBundle( pid);
        bundle.start();
        delay();

        final TestActivator tester = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numUpdatedCalls );

        TestCase.assertEquals( bundle.getLocation(), config.getBundleLocation() );

        bundle.uninstall();
        bundle = null;

        delay();

        // statically bound configurations must remain bound after bundle uninstall
        TestCase.assertNull( config.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_statically_bound() throws BundleException
    {
        final String pid = "test_statically_bound";

        // install the bundle (we need the location)
        bundle = installBundle( pid);
        final String location = bundle.getLocation();

        // create and statically bind the configuration
        configure( pid );
        final Configuration config = getConfiguration( pid );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertNull( config.getBundleLocation() );
        config.setBundleLocation( location );
        TestCase.assertEquals( location, config.getBundleLocation() );

        bundle.start();

        // give cm time for distribution
        delay();

        final TestActivator tester = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numUpdatedCalls );

        TestCase.assertEquals( location, config.getBundleLocation() );

        bundle.uninstall();
        bundle = null;

        delay();

        // statically bound configurations must remain bound after bundle uninstall
        TestCase.assertEquals( location, config.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_static_binding_and_unbinding() throws BundleException
    {
        final String pid = "test_static_binding_and_unbinding";
        final String location = bundleContext.getBundle().getLocation();

        // create and statically bind the configuration
        configure( pid );
        final Configuration config = getConfiguration( pid );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertNull( config.getBundleLocation() );

        // bind the configuration
        config.setBundleLocation( location );
        TestCase.assertEquals( location, config.getBundleLocation() );

        // restart CM bundle
        final Bundle cmBundle = getCmBundle();
        cmBundle.stop();
        delay();
        cmBundle.start();

        // assert configuration still bound
        final Configuration configAfterRestart = getConfiguration( pid );
        TestCase.assertEquals( pid, configAfterRestart.getPid() );
        TestCase.assertEquals( location, configAfterRestart.getBundleLocation() );

        // unbind the configuration
        configAfterRestart.setBundleLocation( null );
        TestCase.assertNull( configAfterRestart.getBundleLocation() );

        // restart CM bundle
        cmBundle.stop();
        delay();
        cmBundle.start();

        // assert configuration unbound
        final Configuration configUnboundAfterRestart = getConfiguration( pid );
        TestCase.assertEquals( pid, configUnboundAfterRestart.getPid() );
        TestCase.assertNull( configUnboundAfterRestart.getBundleLocation() );
    }


    @Test
    public void test_dynamic_binding_and_unbinding() throws BundleException
    {
        final String pid = "test_dynamic_binding_and_unbinding";

        // create and statically bind the configuration
        configure( pid );
        final Configuration config = getConfiguration( pid );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertNull( config.getBundleLocation() );

        // dynamically bind the configuration
        bundle = installBundle( pid );
        final String location = bundle.getLocation();
        bundle.start();
        delay();
        TestCase.assertEquals( location, config.getBundleLocation() );

        // restart CM bundle
        final Bundle cmBundle = getCmBundle();
        cmBundle.stop();
        delay();
        cmBundle.start();

        // assert configuration still bound
        final Configuration configAfterRestart = getConfiguration( pid );
        TestCase.assertEquals( pid, configAfterRestart.getPid() );
        TestCase.assertEquals( location, configAfterRestart.getBundleLocation() );

        // stop bundle (configuration remains bound !!)
        bundle.stop();
        delay();
        TestCase.assertEquals( location, configAfterRestart.getBundleLocation() );

        // restart CM bundle
        cmBundle.stop();
        delay();
        cmBundle.start();

        // assert configuration still bound
        final Configuration configBoundAfterRestart = getConfiguration( pid );
        TestCase.assertEquals( pid, configBoundAfterRestart.getPid() );
        TestCase.assertEquals( location, configBoundAfterRestart.getBundleLocation() );
    }


    /*
    @Test
    public void test_() throws BundleException
    {
        final int count = 2;
        for (int i=0; i < count; i++) {
            final Bundle bundle = installBundle( "dummy", FailureActivator.class );
            bundle.start();
            delay();
            bundle.uninstall();
            delay();
        }
    }
    */


    private Bundle installBundle( final String pid ) throws BundleException
    {
        return installBundle( pid, TestActivator.class );
    }


    private Bundle installBundle( final String pid, final Class<?> activatorClass )
        throws BundleException
    {
        final InputStream bundleStream = new MyTinyBundle()
            .prepare(
                withBnd()
                .set( Constants.BUNDLE_SYMBOLICNAME, "simpleconfiguration" )
                .set( Constants.BUNDLE_VERSION, "0.0.11" )
                .set( Constants.IMPORT_PACKAGE, "org.apache.felix.cm.integration.helper" )
                .set( Constants.BUNDLE_ACTIVATOR, activatorClass.getName() )
                .set( TestActivator.HEADER_PID, pid )
            )
            .build( TinyBundles.asStream() );

        try {
            return bundleContext.installBundle( "test:SimpleComponent", bundleStream );
        } finally {
            try {
                bundleStream.close();
            } catch (IOException ioe) {
            }
        }
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


    private Bundle getCmBundle()
    {
        final ServiceReference caref = configAdminTracker.getServiceReference();
        return ( caref == null ) ? null : caref.getBundle();
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

    private Configuration getConfiguration( final String pid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            final String filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
            org.osgi.service.cm.Configuration[] configs = ca.listConfigurations( filter );
            if ( configs != null && configs.length > 0) {
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

        TestCase.fail("No Configuration " + pid + " found");
        return null;
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
