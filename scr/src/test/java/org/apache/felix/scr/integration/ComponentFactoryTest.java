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


import java.util.Hashtable;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;


@RunWith(JUnit4TestRunner.class)
public class ComponentFactoryTest extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test_component_factory() throws InvalidSyntaxException
    {
        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME, PROP_NAME );
        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instance.getInstance() );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        final Map<?, ?> instanceMap = ( Map<?, ?> ) getFieldValue( component, "m_componentInstances" );
        TestCase.assertNotNull( instanceMap );
        TestCase.assertEquals( 1, instanceMap.size() );

        final Object instanceManager = getFieldValue( instance, "m_componentManager" );
        TestCase.assertTrue( instanceMap.containsValue( instanceManager ) );

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );

        TestCase.assertEquals( 0, instanceMap.size() );
        TestCase.assertFalse( instanceMap.containsValue( instanceManager ) );
    }


    @Test
    public void test_component_factory_disable_factory() throws InvalidSyntaxException
    {
        // tests components remain alive after factory has been disabled

        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME, PROP_NAME );
        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instance.getInstance() );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        final Map<?, ?> instanceMap = ( Map<?, ?> ) getFieldValue( component, "m_componentInstances" );
        TestCase.assertNotNull( instanceMap );
        TestCase.assertEquals( 1, instanceMap.size() );

        final Object instanceManager = getFieldValue( instance, "m_componentManager" );
        TestCase.assertTrue( instanceMap.containsValue( instanceManager ) );

        // disable the factory
        component.disable();
        delay();

        // factory is disabled but the instance is still alive
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( 1, instanceMap.size() );
        TestCase.assertTrue( instanceMap.containsValue( instanceManager ) );

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );

        TestCase.assertEquals( 0, instanceMap.size() );
        TestCase.assertFalse( instanceMap.containsValue( instanceManager ) );
    }


    @Test
    public void test_component_factory_newInstance_failure() throws InvalidSyntaxException
    {
        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME, PROP_NAME );
        props.put( SimpleComponent.PROP_ACTIVATE_FAILURE, "Requested Failure" );
        try {
            final ComponentInstance instance = factory.newInstance( props );
            TestCase.assertNotNull( instance );
            TestCase.fail("Expected newInstance method to fail with ComponentException");
        } catch (ComponentException ce) {
            // this is expected !
        }

        final Map<?, ?> instanceMap = ( Map<?, ?> ) getFieldValue( component, "m_componentInstances" );
        TestCase.assertNotNull( instanceMap );
        TestCase.assertTrue( instanceMap.isEmpty() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }
}
