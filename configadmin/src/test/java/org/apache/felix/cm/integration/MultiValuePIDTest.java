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
import org.osgi.service.cm.Configuration;


@RunWith(JUnit4TestRunner.class)
public class MultiValuePIDTest extends ConfigurationTestBase
{

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

        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 2, tester.numManagedServiceUpdatedCalls );

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

        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 2, tester.numManagedServiceUpdatedCalls );

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
}
