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
import java.util.ArrayList;
import java.util.Dictionary;

import junit.framework.TestCase;

import org.apache.felix.cm.integration.helper.ConfigureThread;
import org.apache.felix.cm.integration.helper.ManagedServiceFactoryThread;
import org.apache.felix.cm.integration.helper.ManagedServiceThread;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;


/**
 * The <code>ConfigUpdateStressTest</code> class tests the issues related to
 * concurrency between configuration update (Configuration.update(Dictionary))
 * and ManagedService[Factory] registration.
 * <p>
 * @see <a href="https://issues.apache.org/jira/browse/FELIX-1545">FELIX-1545</a>
 */
@RunWith(JUnit4TestRunner.class)
public class ConfigUpdateStressTest extends ConfigurationTestBase
{

    @Test
    public void test_ManagedService_race_condition_test()
    {
        int counterMax = 30;
        int failures = 0;

        for ( int counter = 0; counter < counterMax; counter++ )
        {
            try
            {
                single_test_ManagedService_race_condition_test( counter );
            }
            catch ( Throwable ae )
            {
                System.out.println( "single_test_ManagedService_race_condition_test#" + counter + " failed: " + ae );
                ae.printStackTrace( System.out );
                failures++;
            }
        }

        // fail the test if there is at least one failure
        if ( failures != 0 )
        {
            TestCase.fail( failures + "/" + counterMax + " iterations failed" );
        }
    }


    @Test
    public void test_ManagedServiceFactory_race_condition_test()
    {
        int counterMax = 30;
        int failures = 0;

        for ( int counter = 0; counter < counterMax; counter++ )
        {
            try
            {
                single_test_ManagedServiceFactory_race_condition_test( counter );
            }
            catch ( Throwable ae )
            {
                System.out.println( "single_test_ManagedServiceFactory_race_condition_test#" + counter + " failed: "
                    + ae );
                ae.printStackTrace( System.out );
                failures++;
            }
        }

        // fail the test if there is at least one failure
        if ( failures != 0 )
        {
            TestCase.fail( failures + "/" + counterMax + " iterations failed" );
        }
    }


    // runs a single test to encounter the race condition between ManagedService
    // registration and Configuration.update(Dictionary)
    // This test creates/updates configuration and registers a ManagedService
    // almost at the same time. The ManagedService must receive the
    // configuration
    // properties exactly once.
    private void single_test_ManagedService_race_condition_test( final int counter ) throws IOException,
        InterruptedException
    {

        final String pid = "single_test_ManagedService_race_condition_test." + counter;

        final ConfigureThread ct = new ConfigureThread( getConfigurationAdmin(), pid, false );
        final ManagedServiceThread mt = new ManagedServiceThread( bundleContext, pid );

        try
        {
            // start threads -- both are waiting to be triggered
            ct.start();
            mt.start();

            // trigger for action
            ct.trigger();
            mt.trigger();

            // wait for threads to terminate
            ct.join();
            mt.join();

            // wait for all tasks to terminate
            delay();

            final ArrayList<Dictionary> configs = mt.getConfigs();

            // terminate mt to ensure no further config updates
            mt.cleanup();

            if ( configs.size() == 0 )
            {
                TestCase.fail( "No configuration provided to ManagedService at all" );
            }
            else if ( configs.size() == 2 )
            {
                final Dictionary props0 = configs.get( 0 );
                final Dictionary props1 = configs.get( 1 );

                TestCase.assertNull( "Expected first (of two) updates without configuration", props0 );
                TestCase.assertNotNull( "Expected second (of two) updates with configuration", props1 );
            }
            else if ( configs.size() == 1 )
            {
                final Dictionary props = configs.get( 0 );
                TestCase.assertNotNull( "Expected non-null configuration: " + props, props );
            }
            else
            {
                TestCase.fail( "Unexpectedly got " + configs.size() + " updated" );
            }
        }
        finally
        {
            mt.cleanup();
            ct.cleanup();
        }
    }


    // runs a single test to encounter the race condition between
    // ManagedServiceFactory registration and Configuration.update(Dictionary)
    // This test creates/updates configuration and registers a
    // ManagedServiceFactory almost at the same time. The ManagedServiceFactory
    // must receive the configuration properties exactly once.
    private void single_test_ManagedServiceFactory_race_condition_test( final int counter ) throws IOException,
        InterruptedException
    {

        final String factoryPid = "single_test_ManagedServiceFactory_race_condition_test." + counter;

        final ConfigureThread ct = new ConfigureThread( getConfigurationAdmin(), factoryPid, true );
        final ManagedServiceFactoryThread mt = new ManagedServiceFactoryThread( bundleContext, factoryPid );

        try
        {
            // start threads -- both are waiting to be triggered
            ct.start();
            mt.start();

            // trigger for action
            ct.trigger();
            mt.trigger();

            // wait for threads to terminate
            ct.join();
            mt.join();

            // wait for all tasks to terminate
            delay();

            final ArrayList<Dictionary> configs = mt.getConfigs();

            // terminate mt to ensure no further config updates
            mt.cleanup();

            if ( configs.size() == 0 )
            {
                TestCase.fail( "No configuration provided to ManagedServiceFactory at all" );
            }
            else if ( configs.size() == 1 )
            {
                final Dictionary props = configs.get( 0 );
                TestCase.assertNotNull( "Expected non-null configuration: " + props, props );
            }
            else
            {
                TestCase.fail( "Unexpectedly got " + configs.size() + " updated" );
            }
        }
        finally
        {
            mt.cleanup();
            ct.cleanup();
        }
    }
}
