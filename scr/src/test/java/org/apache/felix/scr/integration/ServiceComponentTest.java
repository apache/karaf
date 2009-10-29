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


import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


@RunWith(JUnit4TestRunner.class)
public class ServiceComponentTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test_SimpleComponent_service()
    {
        final String pid = "ServiceComponent";

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


    @Test
    public void test_DelayedSimpleComponent_service_single_use()
    {
        final String pid = "DelayedServiceComponent";

        // one single component exists without configuration
        final Component component = findComponentByName( pid );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        // the delayed service is expected to only be registered before use
        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        // get the service
        ServiceReference reference = bundleContext.getServiceReference( "java.lang.Object" );
        TestCase.assertNotNull( reference );
        try
        {
            final Object theService = bundleContext.getService( reference );

            // service must now be active
            TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

            // and of course we expect the instance
            TestCase.assertEquals( SimpleComponent.INSTANCE, theService );
        }
        finally
        {
            bundleContext.ungetService( reference );
        }

        // service is not used anymore, ensure REGISTERED state and INSTANCE==null
        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }


    @Test
    public void test_DelayedSimpleComponent_service_multi_use()
    {
        final String pid = "DelayedServiceComponent";

        // one single component exists without configuration
        final Component component = findComponentByName( pid );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        // the delayed service is expected to only be registered before use
        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        // get the service once
        final ServiceReference reference1 = bundleContext.getServiceReference( "java.lang.Object" );
        TestCase.assertNotNull( reference1 );
        bundleContext.getService( reference1 );
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );

        // get the service a second time
        final BundleContext bundleContext2 = bundle.getBundleContext();
        final ServiceReference reference2 = bundleContext2.getServiceReference( "java.lang.Object" );
        TestCase.assertNotNull( reference2 );
        bundleContext2.getService( reference2 );
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );

        // unget the service once -- must still be active !
        bundleContext2.ungetService( reference2 );
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );

        // unget the service second time -- must be registered and null now
        bundleContext.ungetService( reference1 );
        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }
}
