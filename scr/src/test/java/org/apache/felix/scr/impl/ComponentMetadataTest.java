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
package org.apache.felix.scr.impl;


import junit.framework.TestCase;

import org.osgi.service.component.ComponentException;


public class ComponentMetadataTest extends TestCase
{

    private TestLogger logger = new TestLogger();


    // test various combinations of component metadata with respect to
    //  -- immediate: true, false, unset
    //  -- factory: set, unset
    //  -- service: set, unset
    //  -- servicefactory: true, false, unset

    public void testImmediate()
    {
        // immediate is default true if no service element is defined
        final ComponentMetadata cm0 = createComponentMetadata( null, null );
        cm0.validate( logger );
        assertTrue( "Component without service must be immediate", cm0.isImmediate() );

        // immediate is explicit true
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.validate( logger );
        assertTrue( "Component must be immediate", cm1.isImmediate() );

        // immediate is explicit true
        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.setService( createServiceMetadata( null ) );
        cm2.validate( logger );
        assertTrue( "Component must be immediate", cm2.isImmediate() );

        // immediate is explicit true
        final ComponentMetadata cm3 = createComponentMetadata( Boolean.TRUE, null );
        cm3.setService( createServiceMetadata( Boolean.FALSE ) );
        cm3.validate( logger );
        assertTrue( "Component must be immediate", cm3.isImmediate() );

        // validation failure of immediate with service factory
        final ComponentMetadata cm4 = createComponentMetadata( Boolean.TRUE, null );
        cm4.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm4.validate( logger );
            fail( "Expect validation failure for immediate service factory" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }
    }


    public void testDelayed()
    {
        // immediate is default false if service element is defined
        final ComponentMetadata cm0 = createComponentMetadata( null, null );
        cm0.setService( createServiceMetadata( null ) );
        cm0.validate( logger );
        assertFalse( "Component with service must be delayed", cm0.isImmediate() );

        // immediate is default false if service element is defined
        final ComponentMetadata cm1 = createComponentMetadata( null, null );
        cm1.setService( createServiceMetadata( Boolean.TRUE ) );
        cm1.validate( logger );
        assertFalse( "Component with service must be delayed", cm1.isImmediate() );

        // immediate is default false if service element is defined
        final ComponentMetadata cm2 = createComponentMetadata( null, null );
        cm2.setService( createServiceMetadata( Boolean.FALSE ) );
        cm2.validate( logger );
        assertFalse( "Component with service must be delayed", cm2.isImmediate() );

        // immediate is false if service element is defined
        final ComponentMetadata cm3 = createComponentMetadata( Boolean.FALSE, null );
        cm3.setService( createServiceMetadata( null ) );
        cm3.validate( logger );
        assertFalse( "Component with service must be delayed", cm3.isImmediate() );

        // immediate is false if service element is defined
        final ComponentMetadata cm4 = createComponentMetadata( Boolean.FALSE, null );
        cm4.setService( createServiceMetadata( Boolean.TRUE ) );
        cm4.validate( logger );
        assertFalse( "Component with service must be delayed", cm4.isImmediate() );

        // immediate is false if service element is defined
        final ComponentMetadata cm5 = createComponentMetadata( Boolean.FALSE, null );
        cm5.setService( createServiceMetadata( Boolean.FALSE ) );
        cm5.validate( logger );
        assertFalse( "Component with service must be delayed", cm5.isImmediate() );

        // explicit delayed fails when there is no service
        final ComponentMetadata cm6 = createComponentMetadata( Boolean.FALSE, null );
        try
        {
            cm6.validate( logger );
            fail( "Expect validation failure for delayed component without service" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }
    }


    public void testFactory()
    {
        // immediate is default false if factory is defined
        final ComponentMetadata cm0 = createComponentMetadata( null, "factory" );
        cm0.validate( logger );
        assertFalse( "Component with factory must be delayed", cm0.isImmediate() );

        // immediate is false if factory is defined
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.FALSE, "factory" );
        cm1.validate( logger );
        assertFalse( "Component with factory must be delayed", cm1.isImmediate() );

        // immediate is default false if factory is defined
        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, "factory" );
        try
        {
            cm2.validate( logger );
            fail( "Expect validation failure for immediate factory component" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

        // immediate is default false if factory is defined
        final ComponentMetadata cm10 = createComponentMetadata( null, "factory" );
        cm10.setService( createServiceMetadata( null ) );
        cm10.validate( logger );
        assertFalse( "Component with factory must be delayed", cm10.isImmediate() );

        // immediate is false if factory is defined
        final ComponentMetadata cm11 = createComponentMetadata( Boolean.FALSE, "factory" );
        cm11.setService( createServiceMetadata( null ) );
        cm11.validate( logger );
        assertFalse( "Component with factory must be delayed", cm11.isImmediate() );

        // immediate is default false if factory is defined
        final ComponentMetadata cm12 = createComponentMetadata( Boolean.TRUE, "factory" );
        cm12.setService( createServiceMetadata( null ) );
        try
        {
            cm12.validate( logger );
            fail( "Expect validation failure for immediate factory component" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

        // immediate is default false if factory is defined
        final ComponentMetadata cm20 = createComponentMetadata( null, "factory" );
        cm20.setService( createServiceMetadata( Boolean.FALSE ) );
        cm20.validate( logger );
        assertFalse( "Component with factory must be delayed", cm20.isImmediate() );

        // immediate is false if factory is defined
        final ComponentMetadata cm21 = createComponentMetadata( Boolean.FALSE, "factory" );
        cm21.setService( createServiceMetadata( Boolean.FALSE ) );
        cm21.validate( logger );
        assertFalse( "Component with factory must be delayed", cm21.isImmediate() );

        // immediate is default false if factory is defined
        final ComponentMetadata cm22 = createComponentMetadata( Boolean.TRUE, "factory" );
        cm22.setService( createServiceMetadata( Boolean.FALSE ) );
        try
        {
            cm22.validate( logger );
            fail( "Expect validation failure for immediate factory component" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

        // immediate is default false if factory is defined
        final ComponentMetadata cm30 = createComponentMetadata( null, "factory" );
        cm30.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm30.validate( logger );
            fail( "Expect validation failure for factory component with service factory" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

        // immediate is false if factory is defined
        final ComponentMetadata cm31 = createComponentMetadata( Boolean.FALSE, "factory" );
        cm31.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm31.validate( logger );
            fail( "Expect validation failure for factory component with service factory" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

        // immediate is default false if factory is defined
        final ComponentMetadata cm32 = createComponentMetadata( Boolean.TRUE, "factory" );
        cm32.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm32.validate( logger );
            fail( "Expect validation failure for immediate factory component with service factory" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

    }


    public void testReference()
    {
        // two references, should validate
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.addDependency( createReferenceMetadata( "name1" ) );
        cm1.addDependency( createReferenceMetadata( "name2" ) );
        cm1.validate( logger );

        // two references, must warn
        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.addDependency( createReferenceMetadata( "name1" ) );
        cm2.addDependency( createReferenceMetadata( "name1" ) );
        cm2.validate( logger );
        assertTrue( "Expected warning for duplicate reference name", logger.lastMessage != null
            && logger.lastMessage.indexOf( "Detected duplicate reference name" ) >= 0 );
    }


    private ComponentMetadata createComponentMetadata( Boolean immediate, String factory )
    {
        ComponentMetadata meta = new ComponentMetadata();
        meta.setName( "place.holder" );
        meta.setImplementationClassName( "place.holder.implementation" );
        if ( immediate != null )
        {
            meta.setImmediate( immediate.booleanValue() );
        }
        if ( factory != null )
        {
            meta.setFactoryIdentifier( factory );
        }
        return meta;
    }


    private ServiceMetadata createServiceMetadata( Boolean serviceFactory )
    {
        ServiceMetadata meta = new ServiceMetadata();
        meta.addProvide( "place.holder.service" );
        if ( serviceFactory != null )
        {
            meta.setServiceFactory( serviceFactory.booleanValue() );
        }
        return meta;
    }


    private ReferenceMetadata createReferenceMetadata( String name )
    {
        ReferenceMetadata meta = new ReferenceMetadata();
        meta.setName( name );
        meta.setInterface( "place.holder" );
        return meta;
    }

    private static class TestLogger implements Logger
    {
        String lastMessage;


        public void log( int level, String message, ComponentMetadata metadata, Throwable ex )
        {
            lastMessage = message;
        }
    }
}
