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


import java.io.IOException;
import java.util.Hashtable;
import junit.framework.TestCase;

import org.apache.felix.cm.integration.helper.ManagedServiceTestActivator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;


@RunWith(JUnit4TestRunner.class)
public class ConfigurationBindingTest extends ConfigurationTestBase
{

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
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

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
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "IOActivator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

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
        // at first configuration access
        final Configuration atEnd = getConfiguration( pid );
        TestCase.assertNull( atEnd.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_not_updated_new_configuration_not_bound_after_bundle_uninstall() throws IOException,
        BundleException
    {
        final String pid = "test_not_updated_new_configuration_not_bound_after_bundle_uninstall";

        // create a configuration but do not update with properties
        final Configuration newConfig = configure( pid, null, false );
        TestCase.assertNull( newConfig.getProperties() );
        TestCase.assertNull( newConfig.getBundleLocation() );

        // start and settle bundle
        bundle = installBundle( pid );
        bundle.start();
        delay();

        // ensure no properties provided to bundle
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );
        TestCase.assertNull( "Expect no properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

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
    public void test_create_with_location_unbind_before_service_supply() throws BundleException, IOException
    {

        final String pid = "test_create_with_location_unbind_before_service_supply";
        final String dummyLocation = "http://some/dummy/location";

        // 1. create and statically bind the configuration
        final Configuration config = configure( pid, dummyLocation, false );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertEquals( dummyLocation, config.getBundleLocation() );

        // 2. update configuration
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME, PROP_NAME );
        config.update( props );
        TestCase.assertEquals( PROP_NAME, config.getProperties().get( PROP_NAME ) );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertEquals( dummyLocation, config.getBundleLocation() );

        // 3. (statically) set location to null
        config.setBundleLocation( null );
        TestCase.assertNull( config.getBundleLocation() );

        // 4. install bundle with service
        bundle = installBundle( pid );
        bundle.start();
        delay();

        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

        TestCase.assertEquals( bundle.getLocation(), config.getBundleLocation() );

        bundle.uninstall();
        bundle = null;

        delay();

        // statically bound configurations must remain bound after bundle
        // uninstall
        TestCase.assertNull( config.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_statically_bound() throws BundleException
    {
        final String pid = "test_statically_bound";

        // install the bundle (we need the location)
        bundle = installBundle( pid );
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

        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

        TestCase.assertEquals( location, config.getBundleLocation() );

        bundle.uninstall();
        bundle = null;

        delay();

        // statically bound configurations must remain bound after bundle
        // uninstall
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


    @Test
    public void test_static_binding() throws BundleException
    {
        final String pid = "test_static_binding";

        // install a bundle to get a location for binding
        bundle = installBundle( pid );
        final String location = bundle.getLocation();

        // create and statically bind the configuration
        configure( pid );
        final Configuration config = getConfiguration( pid );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertNull( config.getBundleLocation() );
        config.setBundleLocation( location );
        TestCase.assertEquals( location, config.getBundleLocation() );

        // start the bundle
        bundle.start();
        delay();
        TestCase.assertEquals( location, config.getBundleLocation() );

        // assert the configuration is supplied
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

        // remove the static binding and assert still bound
        config.setBundleLocation( null );
        TestCase.assertEquals( location, config.getBundleLocation() );

        // uninstall bundle and assert configuration unbound
        bundle.uninstall();
        bundle = null;
        delay();
        TestCase.assertNull( config.getBundleLocation() );
    }


    @Test
    public void test_two_bundles_one_pid() throws BundleException, IOException
    {
        // 1. Bundle registers service with pid1
        final String pid = "test_two_bundles_one_pid";
        final Bundle bundleA = installBundle( pid, ManagedServiceTestActivator.class );
        final String locationA = bundleA.getLocation();
        bundleA.start();
        delay();

        // call back with null
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNull( tester.props );
        TestCase.assertEquals( 1, tester.numManagedServiceUpdatedCalls );

        // 2. create new Conf with pid1 and locationA.
        final Configuration config = configure( pid, locationA, false );
        delay();

        // ==> No call back.
        TestCase.assertNull( tester.props );
        TestCase.assertEquals( 1, tester.numManagedServiceUpdatedCalls );

        // 3. Configuration#update(prop) is called.
        config.update( theConfig );
        delay();

        // ==> call back with the prop.
        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( 2, tester.numManagedServiceUpdatedCalls );

        // 4. Stop BundleA
        bundleA.stop();
        delay();

        // 5. Start BundleA
        bundleA.start();
        delay();

        // ==> call back with the prop.
        final ManagedServiceTestActivator tester2 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester2.props );
        TestCase.assertEquals( 1, tester2.numManagedServiceUpdatedCalls );

        // 6. Configuration#deleted() is called.
        config.delete();
        delay();

        // ==> call back with null.
        TestCase.assertNull( tester2.props );
        TestCase.assertEquals( 2, tester2.numManagedServiceUpdatedCalls );

        // 7. uninstall Bundle A for cleanup.
        bundleA.uninstall();
        delay();

        // Test 2

        // 8. BundleA registers ManagedService with pid1.
        final Bundle bundleA2 = installBundle( pid, ManagedServiceTestActivator.class );
        final String locationA2 = bundleA.getLocation();
        bundleA2.start();
        delay();

        // call back with null
        final ManagedServiceTestActivator tester21 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNull( tester21.props );
        TestCase.assertEquals( 1, tester21.numManagedServiceUpdatedCalls );

        // 9. create new Conf with pid1 and locationB.
        final String locationB = "test:locationB/" + pid;
        final Configuration configB = configure( pid, locationB, false );
        delay();

        // ==> No call back.
        TestCase.assertNull( tester21.props );
        TestCase.assertEquals( 1, tester21.numManagedServiceUpdatedCalls );

        // 10. Configuration#update(prop) is called.
        configB.update( theConfig );
        delay();

        // ==> No call back because the Conf is not bound to locationA.
        TestCase.assertNull( tester21.props );
        TestCase.assertEquals( 1, tester21.numManagedServiceUpdatedCalls );
    }

}
