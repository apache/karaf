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
import org.osgi.framework.Constants;


@RunWith(JUnit4TestRunner.class)
public class ComponentConfigurationTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test_SimpleComponent_configuration_ignore()
    {
        final String pid = "SimpleComponent.configuration.ignore";
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


    @Test
    public void test_SimpleComponent_configuration_optional()
    {
        final String pid = "SimpleComponent.configuration.optional";
        final Component component = findComponentByName( pid );

        deleteConfig( pid );
        delay();

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        final SimpleComponent firstInstance = SimpleComponent.INSTANCE;
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( firstInstance );
        TestCase.assertNull( firstInstance.getProperty( PROP_NAME ) );

        configure( pid );
        delay();

        final SimpleComponent secondInstance = SimpleComponent.INSTANCE;
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( secondInstance );
        TestCase.assertEquals( PROP_NAME, secondInstance.getProperty( PROP_NAME ) );

        deleteConfig( pid );
        delay();

        final SimpleComponent thirdInstance = SimpleComponent.INSTANCE;
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( thirdInstance );
        TestCase.assertNull( thirdInstance.getProperty( PROP_NAME ) );

        TestCase.assertNotSame( "Expect new instance object after reconfiguration", firstInstance, secondInstance );
        TestCase.assertNotSame( "Expect new instance object after configuration deletion (1)", firstInstance,
            thirdInstance );
        TestCase.assertNotSame( "Expect new instance object after configuration deletion (2)", secondInstance,
            thirdInstance );

        component.disable();
        delay();

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }


    @Test
    public void test_SimpleComponent_configuration_require()
    {
        final String pid = "SimpleComponent.configuration.require";
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


    @Test
    public void test_SimpleComponent_dynamic_configuration()
    {
        final String pid = "DynamicConfigurationComponent";
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
        TestCase.assertEquals( pid, SimpleComponent.INSTANCE.getProperty( Constants.SERVICE_PID ) );

        final SimpleComponent instance = SimpleComponent.INSTANCE;

        configure( pid );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertEquals( instance, SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );
        TestCase.assertEquals( pid, SimpleComponent.INSTANCE.getProperty( Constants.SERVICE_PID ) );

        deleteConfig( pid );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertEquals( instance, SimpleComponent.INSTANCE );
        TestCase.assertNull( SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );
        TestCase.assertEquals( pid, SimpleComponent.INSTANCE.getProperty( Constants.SERVICE_PID ) );

        component.disable();
        delay();

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
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

        // delete a configuration
        deleteConfig( pid0 );
        delay();

        // expect one component
        final Component[] oneConfig = findComponentsByName( factoryPid );
        TestCase.assertNotNull( oneConfig );
        TestCase.assertEquals( 1, oneConfig.length );
        TestCase.assertEquals( Component.STATE_ACTIVE, oneConfig[0].getState() );
        TestCase.assertEquals( 1, SimpleComponent.INSTANCES.size() );
        TestCase.assertFalse( SimpleComponent.INSTANCES.containsKey( twoConfigs[0].getId() ) );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( twoConfigs[1].getId() ) );

        // delete second configuration
        deleteConfig( pid1 );
        delay();

        // expect a single unsatisfied component
        final Component[] configsDeleted = findComponentsByName( factoryPid );
        TestCase.assertNotNull( configsDeleted );
        TestCase.assertEquals( 1, configsDeleted.length );
        TestCase.assertEquals( Component.STATE_UNSATISFIED, configsDeleted[0].getState() );
        TestCase.assertEquals( 0, SimpleComponent.INSTANCES.size() );
        TestCase.assertFalse( SimpleComponent.INSTANCES.containsKey( twoConfigs[0].getId() ) );
        TestCase.assertFalse( SimpleComponent.INSTANCES.containsKey( twoConfigs[1].getId() ) );
    }

}
