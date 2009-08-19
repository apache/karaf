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
import java.util.Dictionary;

import junit.framework.TestCase;

import org.apache.felix.cm.integration.helper.ManagedServiceFactoryTestActivator;
import org.apache.felix.cm.integration.helper.ManagedServiceTestActivator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


@RunWith(JUnit4TestRunner.class)
public class ConfigurationBaseTest extends ConfigurationTestBase
{

    @Test
    public void test_basic_configuration_configure_then_start() throws BundleException, IOException
    {
        // 1. create config with pid and locationA
        // 2. update config with properties
        final String pid = "test_basic_configuration_configure_then_start";
        final Configuration config = configure( pid, null, true );

        // 3. register ManagedService ms1 with pid from said locationA
        bundle = installBundle( pid, ManagedServiceTestActivator.class );
        bundle.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( pid, tester.props.get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.props.get( PROP_NAME ) );
        TestCase.assertEquals( 1, tester.numManagedServiceUpdatedCalls );

        // delete
        config.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.props );
        TestCase.assertEquals( 2, tester.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_basic_configuration_start_then_configure() throws BundleException, IOException
    {
        final String pid = "test_basic_configuration_start_then_configure";

        // 1. register ManagedService ms1 with pid from said locationA
        bundle = installBundle( pid, ManagedServiceTestActivator.class );
        bundle.start();
        delay();

        // 1. create config with pid and locationA
        // 2. update config with properties
        final Configuration config = configure( pid, null, true );
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( pid, tester.props.get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.props.get( PROP_NAME ) );
        TestCase.assertEquals( 2, tester.numManagedServiceUpdatedCalls );

        // delete
        config.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.props );
        TestCase.assertEquals( 3, tester.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_basic_configuration_factory_configure_then_start() throws BundleException, IOException
    {
        final String factoryPid = "test_basic_configuration_factory_configure_then_start";
        bundle = installBundle( factoryPid, ManagedServiceFactoryTestActivator.class );
        bundle.start();
        delay();

        final Configuration config = createFactoryConfiguration( factoryPid, null, true );
        final String pid = config.getPid();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceFactoryTestActivator tester = ManagedServiceFactoryTestActivator.INSTANCE;
        Dictionary<?, ?> props = tester.configs.get( pid );
        TestCase.assertNotNull( props );
        TestCase.assertEquals( pid, props.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid, props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props.get( PROP_NAME ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 0, tester.numManagedServiceFactoryDeleteCalls );

        // delete
        config.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.configs.get( pid ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryDeleteCalls );
    }


    @Test
    public void test_basic_configuration_factory_start_then_configure() throws BundleException, IOException
    {
        // 1. create config with pid and locationA
        // 2. update config with properties
        final String factoryPid = "test_basic_configuration_factory_start_then_configure";
        final Configuration config = createFactoryConfiguration( factoryPid, null, true );
        final String pid = config.getPid();

        // 3. register ManagedService ms1 with pid from said locationA
        bundle = installBundle( factoryPid, ManagedServiceFactoryTestActivator.class );
        bundle.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceFactoryTestActivator tester = ManagedServiceFactoryTestActivator.INSTANCE;
        Dictionary<?, ?> props = tester.configs.get( pid );
        TestCase.assertNotNull( props );
        TestCase.assertEquals( pid, props.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid, props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props.get( PROP_NAME ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 0, tester.numManagedServiceFactoryDeleteCalls );

        // delete
        config.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.configs.get( pid ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryDeleteCalls );
    }


    @Test
    public void test_start_bundle_configure_stop_start_bundle() throws BundleException
    {
        String pid = "test_start_bundle_configure_stop_start_bundle";

        // start the bundle and assert this
        bundle = installBundle( pid );
        bundle.start();
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has no configuration
        TestCase.assertNull( "Expect no Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect no update call", 1, tester.numManagedServiceUpdatedCalls );

        // configure after ManagedServiceRegistration --> configure via update
        configure( pid );
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 2, tester.numManagedServiceUpdatedCalls );

        // stop the bundle now
        bundle.stop();

        // assert INSTANCE is null
        TestCase.assertNull( ManagedServiceTestActivator.INSTANCE );

        delay();

        // start the bundle again (and check)
        bundle.start();
        final ManagedServiceTestActivator tester2 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started the second time!!", tester2 );
        TestCase.assertNotSame( "Instances must not be the same", tester, tester2 );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester2.props );
        TestCase.assertEquals( "Expect a second update call", 1, tester2.numManagedServiceUpdatedCalls );

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
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect no update call", 1, tester.numManagedServiceUpdatedCalls );

        // stop the bundle now
        bundle.stop();

        // assert INSTANCE is null
        TestCase.assertNull( ManagedServiceTestActivator.INSTANCE );

        delay();

        // start the bundle again (and check)
        bundle.start();
        final ManagedServiceTestActivator tester2 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started the second time!!", tester2 );
        TestCase.assertNotSame( "Instances must not be the same", tester, tester2 );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester2.props );
        TestCase.assertEquals( "Expect a second update call", 1, tester2.numManagedServiceUpdatedCalls );

        // cleanup
        bundle.uninstall();
        bundle = null;

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_listConfiguration() throws BundleException, IOException
    {
        // 1. create a new Conf1 with pid1 and null location.
        // 2. Conf1#update(props) is called.
        final String pid = "test_listConfiguration";
        final Configuration config = configure( pid, null, true );

        // 3. bundleA will locationA registers ManagedServiceA with pid1.
        bundle = installBundle( pid );
        bundle.start();
        delay();

        // ==> ManagedServiceA is called back.
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester );
        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( 1, tester.numManagedServiceUpdatedCalls );

        // 4. bundleA is stopped but *NOT uninstalled*.
        bundle.stop();
        delay();

        // 5. test bundle calls cm.listConfigurations(null).
        final Configuration listed = getConfiguration( pid );

        // ==> Conf1 is included in the returned list and
        // it has locationA.
        // (In debug mode, dynamicBundleLocation==locationA
        // and staticBundleLocation==null)
        TestCase.assertNotNull( listed );
        TestCase.assertEquals( bundle.getLocation(), listed.getBundleLocation() );

        // 6. test bundle calls cm.getConfiguration(pid1)
        final Configuration get = getConfigurationAdmin().getConfiguration( pid );
        TestCase.assertEquals( bundle.getLocation(), get.getBundleLocation() );

        final Bundle cmBundle = getCmBundle();
        cmBundle.stop();
        delay();
        cmBundle.start();
        delay();

        // 5. test bundle calls cm.listConfigurations(null).
        final Configuration listed2 = getConfiguration( pid );

        // ==> Conf1 is included in the returned list and
        // it has locationA.
        // (In debug mode, dynamicBundleLocation==locationA
        // and staticBundleLocation==null)
        TestCase.assertNotNull( listed2 );
        TestCase.assertEquals( bundle.getLocation(), listed2.getBundleLocation() );

        // 6. test bundle calls cm.getConfiguration(pid1)
        final Configuration get2 = getConfigurationAdmin().getConfiguration( pid );
        TestCase.assertEquals( bundle.getLocation(), get2.getBundleLocation() );
}
}
