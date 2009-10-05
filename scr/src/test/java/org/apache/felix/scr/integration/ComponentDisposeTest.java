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


import java.lang.reflect.Field;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.component.ComponentContext;


@RunWith(JUnit4TestRunner.class)
public class ComponentDisposeTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test_SimpleComponent_factory_configuration()
    {
        final String factoryPid = "FactoryConfigurationComponent";

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
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( twoConfigs[0].getId() ) );
        TestCase.assertFalse( SimpleComponent.INSTANCES.containsKey( twoConfigs[1].getId() ) );

        // enable second component
        twoConfigs[1].enable();
        delay();

        // ensure both components active
        TestCase.assertEquals( Component.STATE_ACTIVE, twoConfigs[0].getState() );
        TestCase.assertEquals( Component.STATE_ACTIVE, twoConfigs[1].getState() );
        TestCase.assertEquals( 2, SimpleComponent.INSTANCES.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( twoConfigs[0].getId() ) );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( twoConfigs[1].getId() ) );

        // dispose an instance
        final SimpleComponent anInstance = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( anInstance );
        TestCase.assertNotNull( anInstance.m_activateContext );
        anInstance.m_activateContext.getComponentInstance().dispose();
        delay();

        // expect one component
        final Component[] oneConfig = findComponentsByName( factoryPid );
        TestCase.assertNotNull( oneConfig );
        TestCase.assertEquals( 1, oneConfig.length );
        TestCase.assertEquals( Component.STATE_ACTIVE, oneConfig[0].getState() );
        TestCase.assertEquals( 1, SimpleComponent.INSTANCES.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( twoConfigs[0].getId() ) );
        TestCase.assertFalse( SimpleComponent.INSTANCES.containsKey( anInstance.m_id ) );

        final SimpleComponent instance = SimpleComponent.INSTANCES.values().iterator().next();

        final Object holder = getComponentHolder( instance.m_activateContext );
        TestCase.assertNotNull( holder );

        Map<?, ?> m_components = ( Map<?, ?> ) getFieldValue( holder, "m_components" );
        TestCase.assertNotNull( m_components );
        TestCase.assertEquals( 1, m_components.size() );
    }


    private static Object getComponentHolder( ComponentContext ctx )
    {
        try
        {
            final Class<?> ccImpl = getType( ctx, "ComponentContextImpl" );
            final Field m_componentManager = getField( ccImpl, "m_componentManager" );
            final Object acm = m_componentManager.get( ctx );

            final Class<?> cmImpl = getType( acm, "ImmediateComponentManager" );
            final Field m_componentHolder = getField( cmImpl, "m_componentHolder" );
            return m_componentHolder.get( acm );
        }
        catch ( Throwable t )
        {
            TestCase.fail( "Cannot get ComponentHolder for " + ctx + ": " + t );
            return null; // keep the compiler happy
        }
    }
}
