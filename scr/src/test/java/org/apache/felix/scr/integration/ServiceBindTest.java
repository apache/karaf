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
public class ServiceBindTest extends ComponentTestBase
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
        final Component component = findComponentByName( "test_optional_single_dynamic" );
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

        srv1.drop();
        // no delay, should be immediate

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp10, comp11 );
        TestCase.assertNull( comp11.m_singleRef );
        TestCase.assertTrue( comp11.m_multiRef.isEmpty() );

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp10, comp12 );
        TestCase.assertEquals( srv2, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );

        component.disable();
        delay(); // async disabling

        final SimpleServiceImpl srv3 = SimpleServiceImpl.create( bundleContext, "srv3" );

        // enable component with two services available, expect srv2 bind
        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp20 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp20 );
        TestCase.assertNotSame( comp10, comp20 );
        TestCase.assertEquals( srv2, comp20.m_singleRef );
        TestCase.assertTrue( comp20.m_multiRef.isEmpty() );

        // drop srv2, expect rebind to srv3 (synchronously)
        srv2.drop();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp21 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp21 );
        TestCase.assertEquals( srv3, comp21.m_singleRef );
        TestCase.assertTrue( comp21.m_multiRef.isEmpty() );

        // create srv4, expect no rebind
        final SimpleServiceImpl srv4 = SimpleServiceImpl.create( bundleContext, "srv4" );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp22 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp22 );
        TestCase.assertEquals( srv3, comp22.m_singleRef );
        TestCase.assertTrue( comp22.m_multiRef.isEmpty() );

        // drop srv4 again, expect no rebind
        srv4.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp23 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp23 );
        TestCase.assertEquals( srv3, comp23.m_singleRef );
        TestCase.assertTrue( comp23.m_multiRef.isEmpty() );

        // "reset"
        component.disable();
        srv3.drop();
        delay();

        // two services with service ranking (srv6 > srv5)
        final SimpleServiceImpl srv5 = SimpleServiceImpl.create( bundleContext, "srv5", 10 );
        final SimpleServiceImpl srv6 = SimpleServiceImpl.create( bundleContext, "srv6", 20 );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp30 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp30 );
        TestCase.assertEquals( srv6, comp30.m_singleRef );
        TestCase.assertTrue( comp30.m_multiRef.isEmpty() );

        // another service with higher ranking -- no rebind !
        final SimpleServiceImpl srv7 = SimpleServiceImpl.create( bundleContext, "srv7", 30 );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp31 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp31 );
        TestCase.assertEquals( srv6, comp31.m_singleRef );
        TestCase.assertTrue( comp31.m_multiRef.isEmpty() );

        // srv6 goes, rebind to srv7
        srv6.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp32 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp32 );
        TestCase.assertEquals( srv7, comp32.m_singleRef );
        TestCase.assertTrue( comp32.m_multiRef.isEmpty() );
    }


    @Test
    public void test_required_single_dynamic()
    {
        final Component component = findComponentByName( "test_required_single_dynamic" );
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

        srv1.drop();
        // no delay, should be immediate

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertNull( comp11 );

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertEquals( srv2, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );

        component.disable();
        delay(); // async disabling

        final SimpleServiceImpl srv3 = SimpleServiceImpl.create( bundleContext, "srv3" );

        // enable component with two services available, expect srv2 bind
        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp20 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp20 );
        TestCase.assertNotSame( comp10, comp20 );
        TestCase.assertEquals( srv2, comp20.m_singleRef );
        TestCase.assertTrue( comp20.m_multiRef.isEmpty() );

        // drop srv2, expect rebind to srv3 (synchronously)
        srv2.drop();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp21 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp21 );
        TestCase.assertEquals( srv3, comp21.m_singleRef );
        TestCase.assertTrue( comp21.m_multiRef.isEmpty() );

        // create srv4, expect no rebind
        final SimpleServiceImpl srv4 = SimpleServiceImpl.create( bundleContext, "srv4" );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp22 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp22 );
        TestCase.assertEquals( srv3, comp22.m_singleRef );
        TestCase.assertTrue( comp22.m_multiRef.isEmpty() );

        // drop srv4 again, expect no rebind
        srv4.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp23 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp23 );
        TestCase.assertEquals( srv3, comp23.m_singleRef );
        TestCase.assertTrue( comp23.m_multiRef.isEmpty() );

        // "reset"
        component.disable();
        srv3.drop();
        delay();

        // two services with service ranking (srv6 > srv5)
        final SimpleServiceImpl srv5 = SimpleServiceImpl.create( bundleContext, "srv5", 10 );
        final SimpleServiceImpl srv6 = SimpleServiceImpl.create( bundleContext, "srv6", 20 );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp30 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp30 );
        TestCase.assertEquals( srv6, comp30.m_singleRef );
        TestCase.assertTrue( comp30.m_multiRef.isEmpty() );

        // another service with higher ranking -- no rebind !
        final SimpleServiceImpl srv7 = SimpleServiceImpl.create( bundleContext, "srv7", 30 );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp31 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp31 );
        TestCase.assertEquals( srv6, comp31.m_singleRef );
        TestCase.assertTrue( comp31.m_multiRef.isEmpty() );

        // srv6 goes, rebind to srv7
        srv6.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp32 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp32 );
        TestCase.assertEquals( srv7, comp32.m_singleRef );
        TestCase.assertTrue( comp32.m_multiRef.isEmpty() );
    }


    @Test
    public void test_optional_multiple_dynamic()
    {
        final Component component = findComponentByName( "test_optional_multiple_dynamic" );
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

        srv1.drop();
        // no delay, should be immediate

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp10, comp11 );
        TestCase.assertNull( comp11.m_singleRef );
        TestCase.assertTrue( comp11.m_multiRef.isEmpty() );

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp10, comp12 );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.contains( srv2 ) );

        component.disable();
        delay(); // async disabling

        final SimpleServiceImpl srv3 = SimpleServiceImpl.create( bundleContext, "srv3" );

        // enable component with two services available, expect both bind
        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp20 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp20 );
        TestCase.assertNotSame( comp10, comp20 );
        TestCase.assertNull( comp20.m_singleRef );
        TestCase.assertTrue( comp20.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp20.m_multiRef.contains( srv3 ) );

        srv2.drop();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp21 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp21 );
        TestCase.assertNull( comp21.m_singleRef );
        TestCase.assertFalse( comp21.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp21.m_multiRef.contains( srv3 ) );

        // create srv4, expect bind
        final SimpleServiceImpl srv4 = SimpleServiceImpl.create( bundleContext, "srv4" );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp22 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp22 );
        TestCase.assertNull( comp22.m_singleRef );
        TestCase.assertFalse( comp22.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp22.m_multiRef.contains( srv3 ) );
        TestCase.assertTrue( comp22.m_multiRef.contains( srv4 ) );

        // drop srv4 again, expect no rebind
        srv4.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp23 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp23 );
        TestCase.assertNull( comp23.m_singleRef );
        TestCase.assertFalse( comp23.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp23.m_multiRef.contains( srv3 ) );
        TestCase.assertFalse( comp23.m_multiRef.contains( srv4 ) );

        // "reset"
        component.disable();
        srv3.drop();
        delay();

        // two services with service ranking (srv6 > srv5)
        final SimpleServiceImpl srv5 = SimpleServiceImpl.create( bundleContext, "srv5", 10 );
        final SimpleServiceImpl srv6 = SimpleServiceImpl.create( bundleContext, "srv6", 20 );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp30 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp30 );
        TestCase.assertNull( comp30.m_singleRef );
        TestCase.assertTrue( comp30.m_multiRef.contains( srv5 ) );
        TestCase.assertTrue( comp30.m_multiRef.contains( srv6 ) );

        // another service with higher ranking -- no rebind !
        final SimpleServiceImpl srv7 = SimpleServiceImpl.create( bundleContext, "srv7", 30 );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp31 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp31 );
        TestCase.assertNull( comp31.m_singleRef );
        TestCase.assertTrue( comp31.m_multiRef.contains( srv5 ) );
        TestCase.assertTrue( comp31.m_multiRef.contains( srv6 ) );
        TestCase.assertTrue( comp31.m_multiRef.contains( srv7 ) );

        // srv6 goes, rebind to srv7
        srv6.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp32 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp32 );
        TestCase.assertNull( comp32.m_singleRef );
        TestCase.assertTrue( comp32.m_multiRef.contains( srv5 ) );
        TestCase.assertFalse( comp32.m_multiRef.contains( srv6 ) );
        TestCase.assertTrue( comp32.m_multiRef.contains( srv7 ) );
    }


    @Test
    public void test_required_multiple_dynamic()
    {
        final Component component = findComponentByName( "test_required_multiple_dynamic" );
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

        srv1.drop();
        // no delay, should be immediate

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertNull( comp11 );

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.contains( srv2 ) );

        component.disable();
        delay(); // async disabling

        final SimpleServiceImpl srv3 = SimpleServiceImpl.create( bundleContext, "srv3" );

        // enable component with two services available, expect both bind
        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp20 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp20 );
        TestCase.assertNotSame( comp10, comp20 );
        TestCase.assertNull( comp20.m_singleRef );
        TestCase.assertTrue( comp20.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp20.m_multiRef.contains( srv3 ) );

        srv2.drop();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp21 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp21 );
        TestCase.assertNull( comp21.m_singleRef );
        TestCase.assertFalse( comp21.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp21.m_multiRef.contains( srv3 ) );

        // create srv4, expect bind
        final SimpleServiceImpl srv4 = SimpleServiceImpl.create( bundleContext, "srv4" );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp22 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp22 );
        TestCase.assertNull( comp22.m_singleRef );
        TestCase.assertFalse( comp22.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp22.m_multiRef.contains( srv3 ) );
        TestCase.assertTrue( comp22.m_multiRef.contains( srv4 ) );

        // drop srv4 again, expect no rebind
        srv4.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp23 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp20, comp23 );
        TestCase.assertNull( comp23.m_singleRef );
        TestCase.assertFalse( comp23.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp23.m_multiRef.contains( srv3 ) );
        TestCase.assertFalse( comp23.m_multiRef.contains( srv4 ) );

        // "reset"
        component.disable();
        srv3.drop();
        delay();

        // two services with service ranking (srv6 > srv5)
        final SimpleServiceImpl srv5 = SimpleServiceImpl.create( bundleContext, "srv5", 10 );
        final SimpleServiceImpl srv6 = SimpleServiceImpl.create( bundleContext, "srv6", 20 );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp30 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp30 );
        TestCase.assertNull( comp30.m_singleRef );
        TestCase.assertTrue( comp30.m_multiRef.contains( srv5 ) );
        TestCase.assertTrue( comp30.m_multiRef.contains( srv6 ) );

        // another service with higher ranking -- no rebind !
        final SimpleServiceImpl srv7 = SimpleServiceImpl.create( bundleContext, "srv7", 30 );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp31 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp31 );
        TestCase.assertNull( comp31.m_singleRef );
        TestCase.assertTrue( comp31.m_multiRef.contains( srv5 ) );
        TestCase.assertTrue( comp31.m_multiRef.contains( srv6 ) );
        TestCase.assertTrue( comp31.m_multiRef.contains( srv7 ) );

        // srv6 goes, rebind to srv7
        srv6.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp32 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp32 );
        TestCase.assertNull( comp32.m_singleRef );
        TestCase.assertTrue( comp32.m_multiRef.contains( srv5 ) );
        TestCase.assertFalse( comp32.m_multiRef.contains( srv6 ) );
        TestCase.assertTrue( comp32.m_multiRef.contains( srv7 ) );
    }


    @Test
    public void test_optional_single_static()
    {
        final Component component = findComponentByName( "test_optional_single_static" );
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

        srv1.drop();
        delay(); // async reactivate

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp11 );
        TestCase.assertNull( comp11.m_singleRef );
        TestCase.assertTrue( comp11.m_multiRef.isEmpty() );

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        // static reference does not rebind unless component is cycled for other reasons !!
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertSame( comp11, comp12 );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );

        component.disable();
        delay(); // async disabling

        final SimpleServiceImpl srv3 = SimpleServiceImpl.create( bundleContext, "srv3" );

        // enable component with two services available, expect srv2 bind
        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp20 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp20 );
        TestCase.assertNotSame( comp10, comp20 );
        TestCase.assertEquals( srv2, comp20.m_singleRef );
        TestCase.assertTrue( comp20.m_multiRef.isEmpty() );

        // drop srv2, expect rebind to srv3 (synchronously)
        srv2.drop();
        delay(); // async reactivate

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp21 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp21 );
        TestCase.assertEquals( srv3, comp21.m_singleRef );
        TestCase.assertTrue( comp21.m_multiRef.isEmpty() );

        // create srv4, expect no rebind
        final SimpleServiceImpl srv4 = SimpleServiceImpl.create( bundleContext, "srv4" );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp22 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp22 );
        TestCase.assertSame( comp21, comp22 );
        TestCase.assertEquals( srv3, comp22.m_singleRef );
        TestCase.assertTrue( comp22.m_multiRef.isEmpty() );

        // drop srv4 again, expect no rebind
        srv4.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp23 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp23 );
        TestCase.assertSame( comp21, comp23 );
        TestCase.assertSame( comp22, comp23 );
        TestCase.assertEquals( srv3, comp23.m_singleRef );
        TestCase.assertTrue( comp23.m_multiRef.isEmpty() );

        // "reset"
        component.disable();
        srv3.drop();
        delay();

        // two services with service ranking (srv6 > srv5)
        final SimpleServiceImpl srv5 = SimpleServiceImpl.create( bundleContext, "srv5", 10 );
        final SimpleServiceImpl srv6 = SimpleServiceImpl.create( bundleContext, "srv6", 20 );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp30 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp23, comp30 );
        TestCase.assertEquals( srv6, comp30.m_singleRef );
        TestCase.assertTrue( comp30.m_multiRef.isEmpty() );

        // another service with higher ranking -- no rebind !
        final SimpleServiceImpl srv7 = SimpleServiceImpl.create( bundleContext, "srv7", 30 );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp31 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp31 );
        TestCase.assertEquals( srv6, comp31.m_singleRef );
        TestCase.assertTrue( comp31.m_multiRef.isEmpty() );

        // srv6 goes, rebind to srv7
        srv6.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp32 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp30, comp32 );
        TestCase.assertNotSame( comp31, comp32 );
        TestCase.assertEquals( srv7, comp32.m_singleRef );
        TestCase.assertTrue( comp32.m_multiRef.isEmpty() );
    }


    @Test
    public void test_required_single_static()
    {
        final Component component = findComponentByName( "test_required_single_static" );
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

        srv1.drop();
        delay(); // async reactivate

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertNull( comp11 );

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertEquals( srv2, comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );

        component.disable();
        delay(); // async disabling

        final SimpleServiceImpl srv3 = SimpleServiceImpl.create( bundleContext, "srv3" );

        // enable component with two services available, expect srv2 bind
        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp20 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp20 );
        TestCase.assertNotSame( comp10, comp20 );
        TestCase.assertEquals( srv2, comp20.m_singleRef );
        TestCase.assertTrue( comp20.m_multiRef.isEmpty() );

        // drop srv2, expect rebind to srv3
        srv2.drop();
        delay(); // async reactivate

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp21 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp21 );
        TestCase.assertEquals( srv3, comp21.m_singleRef );
        TestCase.assertTrue( comp21.m_multiRef.isEmpty() );

        // create srv4, expect no rebind
        final SimpleServiceImpl srv4 = SimpleServiceImpl.create( bundleContext, "srv4" );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp22 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp22 );
        TestCase.assertSame( comp21, comp22 );
        TestCase.assertEquals( srv3, comp22.m_singleRef );
        TestCase.assertTrue( comp22.m_multiRef.isEmpty() );

        // drop srv4 again, expect no rebind
        srv4.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp23 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp23 );
        TestCase.assertSame( comp21, comp23 );
        TestCase.assertSame( comp22, comp23 );
        TestCase.assertEquals( srv3, comp23.m_singleRef );
        TestCase.assertTrue( comp23.m_multiRef.isEmpty() );

        // "reset"
        component.disable();
        srv3.drop();
        delay();

        // two services with service ranking (srv6 > srv5)
        final SimpleServiceImpl srv5 = SimpleServiceImpl.create( bundleContext, "srv5", 10 );
        final SimpleServiceImpl srv6 = SimpleServiceImpl.create( bundleContext, "srv6", 20 );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp30 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp23, comp30 );
        TestCase.assertEquals( srv6, comp30.m_singleRef );
        TestCase.assertTrue( comp30.m_multiRef.isEmpty() );

        // another service with higher ranking -- no rebind !
        final SimpleServiceImpl srv7 = SimpleServiceImpl.create( bundleContext, "srv7", 30 );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp31 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp31 );
        TestCase.assertEquals( srv6, comp31.m_singleRef );
        TestCase.assertTrue( comp31.m_multiRef.isEmpty() );

        // srv6 goes, rebind to srv7
        srv6.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp32 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp30, comp32 );
        TestCase.assertNotSame( comp31, comp32 );
        TestCase.assertEquals( srv7, comp32.m_singleRef );
        TestCase.assertTrue( comp32.m_multiRef.isEmpty() );
    }


    @Test
    public void test_optional_multiple_static()
    {
        final Component component = findComponentByName( "test_optional_multiple_static" );
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

        srv1.drop();
        delay(); // async reactivate

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp11 );
        TestCase.assertNull( comp11.m_singleRef );
        TestCase.assertTrue( comp11.m_multiRef.isEmpty() );

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertSame( comp11, comp12 );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.isEmpty() );

        component.disable();
        delay(); // async disabling

        final SimpleServiceImpl srv3 = SimpleServiceImpl.create( bundleContext, "srv3" );

        // enable component with two services available, expect both bind
        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp20 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp20 );
        TestCase.assertNotSame( comp10, comp20 );
        TestCase.assertNotSame( comp11, comp20 );
        TestCase.assertNotSame( comp12, comp20 );
        TestCase.assertNull( comp20.m_singleRef );
        TestCase.assertTrue( comp20.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp20.m_multiRef.contains( srv3 ) );

        srv2.drop();
        delay(); // async reactivate

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp21 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp21 );
        TestCase.assertNull( comp21.m_singleRef );
        TestCase.assertFalse( comp21.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp21.m_multiRef.contains( srv3 ) );

        // create srv4, expect not bind (static case)
        final SimpleServiceImpl srv4 = SimpleServiceImpl.create( bundleContext, "srv4" );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp22 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp22 );
        TestCase.assertSame( comp21, comp22 );
        TestCase.assertNull( comp22.m_singleRef );
        TestCase.assertFalse( comp22.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp22.m_multiRef.contains( srv3 ) );
        TestCase.assertFalse( comp22.m_multiRef.contains( srv4 ) );

        // drop srv4 again, expect no rebind
        srv4.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp23 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp23 );
        TestCase.assertSame( comp21, comp23 );
        TestCase.assertSame( comp22, comp23 );
        TestCase.assertNull( comp23.m_singleRef );
        TestCase.assertFalse( comp23.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp23.m_multiRef.contains( srv3 ) );
        TestCase.assertFalse( comp23.m_multiRef.contains( srv4 ) );

        // "reset"
        component.disable();
        srv3.drop();
        delay();

        // two services with service ranking (srv6 > srv5)
        final SimpleServiceImpl srv5 = SimpleServiceImpl.create( bundleContext, "srv5", 10 );
        final SimpleServiceImpl srv6 = SimpleServiceImpl.create( bundleContext, "srv6", 20 );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp30 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp23, comp30 );
        TestCase.assertNull( comp30.m_singleRef );
        TestCase.assertTrue( comp30.m_multiRef.contains( srv5 ) );
        TestCase.assertTrue( comp30.m_multiRef.contains( srv6 ) );

        // another service with higher ranking -- no rebind !
        final SimpleServiceImpl srv7 = SimpleServiceImpl.create( bundleContext, "srv7", 30 );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp31 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp31 );
        TestCase.assertNull( comp31.m_singleRef );
        TestCase.assertTrue( comp31.m_multiRef.contains( srv5 ) );
        TestCase.assertTrue( comp31.m_multiRef.contains( srv6 ) );
        TestCase.assertFalse( comp31.m_multiRef.contains( srv7 ) );

        // srv6 goes, rebind to srv7
        srv6.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp32 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp30, comp32 );
        TestCase.assertNotSame( comp31, comp32 );
        TestCase.assertNull( comp32.m_singleRef );
        TestCase.assertTrue( comp32.m_multiRef.contains( srv5 ) );
        TestCase.assertFalse( comp32.m_multiRef.contains( srv6 ) );
        TestCase.assertTrue( comp32.m_multiRef.contains( srv7 ) );
    }


    @Test
    public void test_required_multiple_static()
    {
        final Component component = findComponentByName( "test_required_multiple_static" );
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

        srv1.drop();
        delay(); // async reactivate

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        final SimpleComponent comp11 = SimpleComponent.INSTANCE;
        TestCase.assertNull( comp11 );

        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );
        delay(); // async binding

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp12 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp10, comp12 );
        TestCase.assertNull( comp12.m_singleRef );
        TestCase.assertTrue( comp12.m_multiRef.contains( srv2 ) );

        component.disable();
        delay(); // async disabling

        final SimpleServiceImpl srv3 = SimpleServiceImpl.create( bundleContext, "srv3" );

        // enable component with two services available, expect both bind
        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp20 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp20 );
        TestCase.assertNotSame( comp10, comp20 );
        TestCase.assertNotSame( comp12, comp20 );
        TestCase.assertNull( comp20.m_singleRef );
        TestCase.assertTrue( comp20.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp20.m_multiRef.contains( srv3 ) );

        srv2.drop();
        delay(); // async reactivate

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp21 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp21 );
        TestCase.assertNull( comp21.m_singleRef );
        TestCase.assertFalse( comp21.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp21.m_multiRef.contains( srv3 ) );

        // create srv4, expect bind
        final SimpleServiceImpl srv4 = SimpleServiceImpl.create( bundleContext, "srv4" );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp22 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp22 );
        TestCase.assertSame( comp21, comp22 );
        TestCase.assertNull( comp22.m_singleRef );
        TestCase.assertFalse( comp22.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp22.m_multiRef.contains( srv3 ) );
        TestCase.assertFalse( comp22.m_multiRef.contains( srv4 ) );

        // drop srv4 again, expect no rebind
        srv4.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp23 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp20, comp23 );
        TestCase.assertSame( comp21, comp23 );
        TestCase.assertSame( comp22, comp23 );
        TestCase.assertNull( comp23.m_singleRef );
        TestCase.assertFalse( comp23.m_multiRef.contains( srv2 ) );
        TestCase.assertTrue( comp23.m_multiRef.contains( srv3 ) );
        TestCase.assertFalse( comp23.m_multiRef.contains( srv4 ) );

        // "reset"
        component.disable();
        srv3.drop();
        delay();

        // two services with service ranking (srv6 > srv5)
        final SimpleServiceImpl srv5 = SimpleServiceImpl.create( bundleContext, "srv5", 10 );
        final SimpleServiceImpl srv6 = SimpleServiceImpl.create( bundleContext, "srv6", 20 );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp30 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp23, comp30 );
        TestCase.assertNull( comp30.m_singleRef );
        TestCase.assertTrue( comp30.m_multiRef.contains( srv5 ) );
        TestCase.assertTrue( comp30.m_multiRef.contains( srv6 ) );

        // another service with higher ranking -- no rebind !
        final SimpleServiceImpl srv7 = SimpleServiceImpl.create( bundleContext, "srv7", 30 );
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp31 = SimpleComponent.INSTANCE;
        TestCase.assertSame( comp30, comp31 );
        TestCase.assertNull( comp31.m_singleRef );
        TestCase.assertTrue( comp31.m_multiRef.contains( srv5 ) );
        TestCase.assertTrue( comp31.m_multiRef.contains( srv6 ) );
        TestCase.assertFalse( comp31.m_multiRef.contains( srv7 ) );

        // srv6 goes, rebind to srv7
        srv6.drop();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp32 = SimpleComponent.INSTANCE;
        TestCase.assertNotSame( comp30, comp32 );
        TestCase.assertNotSame( comp31, comp32 );
        TestCase.assertNull( comp32.m_singleRef );
        TestCase.assertTrue( comp32.m_multiRef.contains( srv5 ) );
        TestCase.assertFalse( comp32.m_multiRef.contains( srv6 ) );
        TestCase.assertTrue( comp32.m_multiRef.contains( srv7 ) );
    }
}