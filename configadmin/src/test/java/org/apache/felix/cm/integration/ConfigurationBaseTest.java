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


import junit.framework.TestCase;

import org.apache.felix.cm.integration.helper.ManagedServiceTestActivator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleException;


@RunWith(JUnit4TestRunner.class)
public class ConfigurationBaseTest extends ConfigurationTestBase
{

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
}
