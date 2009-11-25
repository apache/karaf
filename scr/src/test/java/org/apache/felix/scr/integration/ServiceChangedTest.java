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
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;


@RunWith(JUnit4TestRunner.class)
public class ServiceChangedTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_simple_components_service_binding.xml";
    }


    @Test
    public void test_optional_single_dynamic()
    {
        final Component component = findComponentByName( "test_optional_single_dynamic_target" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );

        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertEquals( srv1, comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 0, comp10.m_singleRefUnbind);

        // update a service property
        srv1.update( "srv1-modified" );

        // no changes in bindings expected
        TestCase.assertEquals( srv1, comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 0, comp10.m_singleRefUnbind);

        // set target to not match any more
        srv1.setFilterProperty( "don't match" );

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp10, comp11 );
        TestCase.assertNull( comp11.m_singleRef );
        TestCase.assertTrue( comp11.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 1, comp10.m_singleRefUnbind);

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp10, comp12 );
        TestCase.assertEquals( srv2, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );
        TestCase.assertEquals( 2, comp10.m_singleRefBind );
        TestCase.assertEquals( 1, comp10.m_singleRefUnbind);

        // make srv1 match again, expect not changes in bindings
        srv1.setFilterProperty( "match" );
        TestCase.assertEquals( srv2, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );
        TestCase.assertEquals( 2, comp10.m_singleRefBind );
        TestCase.assertEquals( 1, comp10.m_singleRefUnbind);

        // make srv2 to not match, expect binding to srv1
        srv2.setFilterProperty( "don't match" );
        TestCase.assertEquals( srv1, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );
        TestCase.assertEquals( 3, comp10.m_singleRefBind );
        TestCase.assertEquals( 2, comp10.m_singleRefUnbind);
    }


    @Test
    public void test_required_single_dynamic()
    {
        final Component component = findComponentByName( "test_required_single_dynamic_target" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );

        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertEquals( srv1, comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 0, comp10.m_singleRefUnbind);

        // update a service property
        srv1.update( "srv1-modified" );

        // no changes in bindings expected
        TestCase.assertEquals( srv1, comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 0, comp10.m_singleRefUnbind);

        // set target to not match any more -> deactivate this component
        srv1.setFilterProperty( "don't match" );
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 1, comp10.m_singleRefUnbind);

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertEquals( srv2, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp12.m_singleRefBind );
        TestCase.assertEquals( 0, comp12.m_singleRefUnbind);

        // make srv1 match again, expect not changes in bindings
        srv1.setFilterProperty( "match" );
        TestCase.assertEquals( srv2, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp12.m_singleRefBind );
        TestCase.assertEquals( 0, comp12.m_singleRefUnbind);

        // make srv2 to not match, expect binding to srv1
        srv2.setFilterProperty( "don't match" );
        TestCase.assertEquals( srv1, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );
        TestCase.assertEquals( 2, comp12.m_singleRefBind );
        TestCase.assertEquals( 1, comp12.m_singleRefUnbind);
    }


    @Test
    public void test_optional_multiple_dynamic()
    {
        final Component component = findComponentByName( "test_optional_multiple_dynamic_target" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );

        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 0, comp10.m_multiRefUnbind);

        // update a service property
        srv1.update( "srv1-modified" );

        // no changes in bindings expected
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 0, comp10.m_multiRefUnbind);

        // set target to not match any more
        srv1.setFilterProperty( "don't match" );

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp10, comp11 );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertFalse( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 1, comp10.m_multiRefUnbind);

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp10, comp12 );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertFalse( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 2, comp10.m_multiRefBind );
        TestCase.assertEquals( 1, comp10.m_multiRefUnbind);

        // make srv1 match again, expect not changes in bindings
        srv1.setFilterProperty( "match" );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 3, comp10.m_multiRefBind );
        TestCase.assertEquals( 1, comp10.m_multiRefUnbind);

        // make srv2 to not match, expect binding to srv1
        srv2.setFilterProperty( "don't match" );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertFalse( comp10.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 3, comp10.m_multiRefBind );
        TestCase.assertEquals( 2, comp10.m_multiRefUnbind);
    }


    @Test
    public void test_required_multiple_dynamic()
    {
        final Component component = findComponentByName( "test_required_multiple_dynamic_target" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );

        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 0, comp10.m_multiRefUnbind);

        // update a service property
        srv1.update( "srv1-modified" );

        // no changes in bindings expected
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 0, comp10.m_multiRefUnbind);

        // set target to not match any more
        srv1.setFilterProperty( "don't match" );

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertNull( comp11 );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertFalse( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 1, comp10.m_multiRefUnbind);

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertFalse( comp12.m_multiRef.contains( srv1 ) );
        TestCase.assertTrue( comp12.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 1, comp12.m_multiRefBind );
        TestCase.assertEquals( 0, comp12.m_multiRefUnbind);

        // make srv1 match again, expect not changes in bindings
        srv1.setFilterProperty( "match" );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.contains( srv1 ) );
        TestCase.assertTrue( comp12.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 2, comp12.m_multiRefBind );
        TestCase.assertEquals( 0, comp12.m_multiRefUnbind);

        // make srv2 to not match, expect binding to srv1
        srv2.setFilterProperty( "don't match" );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.contains( srv1 ) );
        TestCase.assertFalse( comp12.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 2, comp12.m_multiRefBind );
        TestCase.assertEquals( 1, comp12.m_multiRefUnbind);
    }


    @Test
    public void test_optional_single_static()
    {
        final Component component = findComponentByName( "test_optional_single_static_target" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );

        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertEquals( srv1, comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 0, comp10.m_singleRefUnbind);

        // update a service property
        srv1.update( "srv1-modified" );

        // no changes in bindings expected
        TestCase.assertEquals( srv1, comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 0, comp10.m_singleRefUnbind);

        // set target to not match any more -> recreate !
        srv1.setFilterProperty( "don't match" );
        delay(); // async reactivation

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp11 );
        TestCase.assertNull( comp11.m_singleRef );
        TestCase.assertTrue( comp11.m_multiRef.isEmpty() );
        TestCase.assertEquals( 0, comp11.m_singleRefBind );
        TestCase.assertEquals( 0, comp11.m_singleRefUnbind);

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertSame( comp11, comp12 );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );
        TestCase.assertEquals( 0, comp12.m_singleRefBind );
        TestCase.assertEquals( 0, comp12.m_singleRefUnbind);

        // make srv1 match again, expect not changes in bindings
        srv1.setFilterProperty( "match" );
        final SimpleComponent comp13 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp13 );
        TestCase.assertSame( comp11, comp13 );
        TestCase.assertSame( comp12, comp13 );
        TestCase.assertNull( comp13.m_singleRef );
        TestCase.assertTrue( comp13.m_multiRef.isEmpty() );
        TestCase.assertEquals( 0, comp13.m_singleRefBind );
        TestCase.assertEquals( 0, comp13.m_singleRefUnbind);

        // make srv2 to not match, expect binding to srv1
        srv2.setFilterProperty( "don't match" );
        final SimpleComponent comp14 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp14 );
        TestCase.assertSame( comp11, comp14 );
        TestCase.assertSame( comp12, comp14 );
        TestCase.assertSame( comp13, comp14 );
        TestCase.assertNull( comp14.m_singleRef );
        TestCase.assertTrue( comp14.m_multiRef.isEmpty() );
        TestCase.assertEquals( 0, comp14.m_singleRefBind );
        TestCase.assertEquals( 0, comp14.m_singleRefUnbind);
    }


    @Test
    public void test_required_single_static()
    {
        final Component component = findComponentByName( "test_required_single_static_target" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );

        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertEquals( srv1, comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 0, comp10.m_singleRefUnbind);

        // update a service property
        srv1.update( "srv1-modified" );

        // no changes in bindings expected
        TestCase.assertEquals( srv1, comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 0, comp10.m_singleRefUnbind);

        // set target to not match any more -> deactivate this component
        srv1.setFilterProperty( "don't match" );
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 1, comp10.m_singleRefUnbind);

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertEquals( srv2, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp12.m_singleRefBind );
        TestCase.assertEquals( 0, comp12.m_singleRefUnbind);

        // make srv1 match again, expect not changes in bindings
        srv1.setFilterProperty( "match" );
        final SimpleComponent comp13 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertSame( comp12, comp13 );
        TestCase.assertEquals( srv2, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp12.m_singleRefBind );
        TestCase.assertEquals( 0, comp12.m_singleRefUnbind);

        // make srv2 to not match, expect binding to srv1
        srv2.setFilterProperty( "don't match" );
        delay(); // reactivation required
        final SimpleComponent comp14 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp14 );
        TestCase.assertNotSame( comp12, comp14 );
        TestCase.assertNotSame( comp13, comp14 );
        TestCase.assertEquals( srv1, comp14.m_singleRef );
        TestCase.assertTrue( comp14.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp14.m_singleRefBind );
        TestCase.assertEquals( 0, comp14.m_singleRefUnbind);
    }


    @Test
    public void test_optional_multiple_static()
    {
        final Component component = findComponentByName( "test_optional_multiple_static_target" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );

        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 0, comp10.m_multiRefUnbind);

        // update a service property
        srv1.update( "srv1-modified" );

        // no changes in bindings expected
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 0, comp10.m_multiRefUnbind);

        // set target to not match any more
        srv1.setFilterProperty( "don't match" );
        delay(); // async reactivation (for unbind)

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp11 );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertFalse( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 1, comp10.m_multiRefUnbind);
        TestCase.assertNull( comp11.m_singleRef );
        TestCase.assertFalse( comp11.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 0, comp11.m_multiRefBind );
        TestCase.assertEquals( 0, comp11.m_multiRefUnbind);

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding (not expected for an optional static ref)

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertSame( comp11, comp12 );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertFalse( comp12.m_multiRef.contains( srv1 ) );
        TestCase.assertFalse( comp12.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 0, comp12.m_multiRefBind );
        TestCase.assertEquals( 0, comp12.m_multiRefUnbind);

        // make srv1 match again, expect not changes in bindings
        srv1.setFilterProperty( "match" );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertFalse( comp12.m_multiRef.contains( srv1 ) );
        TestCase.assertFalse( comp12.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 0, comp12.m_multiRefBind );
        TestCase.assertEquals( 0, comp12.m_multiRefUnbind);

        // make srv2 to not match, expect binding to srv1
        srv2.setFilterProperty( "don't match" );
        delay(); // allow reactivation delay (for unbind/bind)

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp13 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp13 );
        TestCase.assertSame( comp11, comp13 );
        TestCase.assertSame( comp12, comp13 );
        TestCase.assertNull( comp13.m_singleRef );
        TestCase.assertFalse( comp13.m_multiRef.contains( srv1 ) );
        TestCase.assertFalse( comp13.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 0, comp13.m_multiRefBind );
        TestCase.assertEquals( 0, comp13.m_multiRefUnbind);
    }


    @Test
    public void test_required_multiple_static()
    {
        final Component component = findComponentByName( "test_required_multiple_static_target" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );

        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 0, comp10.m_multiRefUnbind);

        // update a service property
        srv1.update( "srv1-modified" );

        // no changes in bindings expected
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 0, comp10.m_multiRefUnbind);

        // set target to not match any more
        srv1.setFilterProperty( "don't match" );

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertNull( comp11 );
        TestCase.assertNull( comp10.m_singleRef );
        TestCase.assertFalse( comp10.m_multiRef.contains( srv1 ) );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 1, comp10.m_multiRefUnbind);

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertFalse( comp12.m_multiRef.contains( srv1 ) );
        TestCase.assertTrue( comp12.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 1, comp12.m_multiRefBind );
        TestCase.assertEquals( 0, comp12.m_multiRefUnbind);

        // make srv1 match again, expect not changes in bindings
        srv1.setFilterProperty( "match" );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertFalse( comp12.m_multiRef.contains( srv1 ) );
        TestCase.assertTrue( comp12.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 1, comp12.m_multiRefBind );
        TestCase.assertEquals( 0, comp12.m_multiRefUnbind);

        // make srv2 to not match, expect binding to srv1
        srv2.setFilterProperty( "don't match" );
        delay(); // allow reactivation/rebinding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp13 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp13 );
        TestCase.assertNotSame( comp11, comp13 );
        TestCase.assertNotSame( comp12, comp13 );
        TestCase.assertNull( comp13.m_singleRef );
        TestCase.assertTrue( comp13.m_multiRef.contains( srv1 ) );
        TestCase.assertFalse( comp13.m_multiRef.contains( srv2 ) );
        TestCase.assertEquals( 1, comp13.m_multiRefBind );
        TestCase.assertEquals( 0, comp13.m_multiRefUnbind);
   }
}