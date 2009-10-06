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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;


@RunWith(JUnit4TestRunner.class)
public class ComponentActivationTest extends ComponentTestBase
{

    static
    {
        // use different components
        descriptorFile = "/integration_test_activation_components.xml";

        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test_activator_not_declared()
    {
        final String componentname = "ActivatorComponent.no.decl";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
    }


    @Test
    public void test_activate_missing()
    {
        final String componentname = "ActivatorComponent.activate.missing";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        // activate must fail
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
    }


    @Test
    public void test_deactivate_missing()
    {
        final String componentname = "ActivatorComponent.deactivate.missing";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
    }


    @Test
    public void test_activator_declared()
    {
        final String componentname = "ActivatorComponent.decl";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
    }


    @Test
    public void test_activate_fail()
    {
        final String componentname = "ActivatorComponent.activate.fail";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        // activate has failed
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
    }


    @Test
    public void test_deactivate_fail()
    {
        final String componentname = "ActivatorComponent.deactivate.fail";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
    }
}
