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
package org.apache.felix.scr.impl.metadata;


import junit.framework.TestCase;

import org.apache.felix.scr.impl.MockLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.apache.felix.scr.impl.metadata.XmlHandler;
import org.osgi.service.component.ComponentException;


public class ComponentMetadataTest extends TestCase
{

    private MockLogger logger = new MockLogger();


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


    public void test_component_no_name_ds10()
    {
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.setName( null );
        try
        {
            cm1.validate( logger );
            fail( "Expected validation failure for DS 1.0 component without name" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_component_no_name_ds11()
    {
        final ComponentMetadata cm1 = createComponentMetadata11( Boolean.TRUE, null );
        cm1.setName( null );
        cm1.validate( logger );
        assertEquals( "Expected name to equal implementation class name", cm1.getImplementationClassName(), cm1
            .getName() );
    }


    public void test_component_activate_ds10()
    {
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Activate method name", "activate", cm1.getActivate() );
        assertFalse( "Activate method expected to not be declared", cm1.isActivateDeclared() );

        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.setActivate( "someMethod" );
        cm2.validate( logger );
        assertEquals( "Activate method name", "activate", cm2.getActivate() );
        assertFalse( "Activate method expected to not be declared", cm2.isActivateDeclared() );
    }


    public void test_component_activate_ds11()
    {
        final ComponentMetadata cm1 = createComponentMetadata11( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Activate method name", "activate", cm1.getActivate() );
        assertFalse( "Activate method expected to not be declared", cm1.isActivateDeclared() );

        final ComponentMetadata cm2 = createComponentMetadata11( Boolean.TRUE, null );
        cm2.setActivate( "someMethod" );
        cm2.validate( logger );
        assertEquals( "Activate method name", "someMethod", cm2.getActivate() );
        assertTrue( "Activate method expected to be declared", cm2.isActivateDeclared() );
    }


    public void test_component_deactivate_ds10()
    {
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Deactivate method name", "deactivate", cm1.getDeactivate() );
        assertFalse( "Deactivate method expected to not be declared", cm1.isDeactivateDeclared() );

        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.setDeactivate( "someMethod" );
        cm2.validate( logger );
        assertEquals( "Deactivate method name", "deactivate", cm2.getDeactivate() );
        assertFalse( "Deactivate method expected to not be declared", cm2.isDeactivateDeclared() );
    }


    public void test_component_deactivate_ds11()
    {
        final ComponentMetadata cm1 = createComponentMetadata11( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Deactivate method name", "deactivate", cm1.getDeactivate() );
        assertFalse( "Deactivate method expected to not be declared", cm1.isDeactivateDeclared() );

        final ComponentMetadata cm2 = createComponentMetadata11( Boolean.TRUE, null );
        cm2.setDeactivate( "someMethod" );
        cm2.validate( logger );
        assertEquals( "Deactivate method name", "someMethod", cm2.getDeactivate() );
        assertTrue( "Deactivate method expected to be declared", cm2.isDeactivateDeclared() );
    }


    public void test_component_modified_ds10()
    {
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.validate( logger );
        assertNull( "Modified method name", cm1.getModified() );

        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.setModified( "someName" );
        cm2.validate( logger );
        assertNull( "Modified method name", cm2.getModified() );
    }


    public void test_component_modified_ds11()
    {
        final ComponentMetadata cm1 = createComponentMetadata11( Boolean.TRUE, null );
        cm1.validate( logger );
        assertNull( "Modified method name", cm1.getModified() );

        final ComponentMetadata cm2 = createComponentMetadata11( Boolean.TRUE, null );
        cm2.setModified( "someMethod" );
        cm2.validate( logger );
        assertEquals( "Modified method name", "someMethod", cm2.getModified() );
    }


    public void test_component_configuration_policy_ds10()
    {
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL, cm1
            .getConfigurationPolicy() );

        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_IGNORE );
        cm2.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL, cm2
            .getConfigurationPolicy() );

        final ComponentMetadata cm3 = createComponentMetadata( Boolean.TRUE, null );
        cm3.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL );
        cm3.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL, cm3
            .getConfigurationPolicy() );

        final ComponentMetadata cm4 = createComponentMetadata( Boolean.TRUE, null );
        cm4.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_REQUIRE );
        cm4.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL, cm4
            .getConfigurationPolicy() );

        final ComponentMetadata cm5 = createComponentMetadata( Boolean.TRUE, null );
        cm5.setConfigurationPolicy( "undefined" );
        cm5.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL, cm5
            .getConfigurationPolicy() );
    }


    public void test_component_configuration_policy_ds11()
    {
        final ComponentMetadata cm1 = createComponentMetadata11( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL, cm1
            .getConfigurationPolicy() );

        final ComponentMetadata cm2 = createComponentMetadata11( Boolean.TRUE, null );
        cm2.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_IGNORE );
        cm2.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_IGNORE, cm2
            .getConfigurationPolicy() );

        final ComponentMetadata cm3 = createComponentMetadata11( Boolean.TRUE, null );
        cm3.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL );
        cm3.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL, cm3
            .getConfigurationPolicy() );

        final ComponentMetadata cm4 = createComponentMetadata11( Boolean.TRUE, null );
        cm4.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_REQUIRE );
        cm4.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_REQUIRE, cm4
            .getConfigurationPolicy() );

        final ComponentMetadata cm5 = createComponentMetadata11( Boolean.TRUE, null );
        cm5.setConfigurationPolicy( "undefined" );
        try
        {
            cm5.validate( logger );
            fail( "Expected validation failure due to undefined configuration policy" );
        }
        catch ( ComponentException ce )
        {
            // expected due to undefned configuration policy
        }
    }


    public void test_reference_valid()
    {
        // two references, should validate
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.addDependency( createReferenceMetadata( "name1" ) );
        cm1.addDependency( createReferenceMetadata( "name2" ) );
        cm1.validate( logger );
    }


    public void test_reference_duplicate_name()
    {
        // two references with same name, must warn
        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.addDependency( createReferenceMetadata( "name1" ) );
        cm2.addDependency( createReferenceMetadata( "name1" ) );
        cm2.validate( logger );
        assertTrue( "Expected warning for duplicate reference name", logger
            .messageContains( "Detected duplicate reference name" ) );
    }


    public void test_reference_no_name_ds10()
    {
        // un-named reference, illegal for pre DS 1.1
        final ComponentMetadata cm3 = createComponentMetadata( Boolean.TRUE, null );
        cm3.addDependency( createReferenceMetadata( null ) );
        try
        {
            cm3.validate( logger );
            fail( "Expect validation failure for DS 1.0 reference without name" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_reference_no_name_ds11()
    {
        // un-named reference, illegal for DS 1.1
        final ComponentMetadata cm4 = createComponentMetadata11( Boolean.TRUE, null );
        final ReferenceMetadata rm4 = createReferenceMetadata( null );
        cm4.addDependency( rm4 );
        cm4.validate( logger );
        assertEquals( "Reference name defaults to interface", rm4.getInterface(), rm4.getName() );
    }


    public void test_reference_updated_ds10()
    {
        // updated method ignored for DS 1.0
        final ReferenceMetadata rm3 = createReferenceMetadata( "test" );
        rm3.setUpdated( "my_updated_method" );
        final ComponentMetadata cm3 = createComponentMetadata( Boolean.TRUE, null );
        cm3.addDependency( rm3 );

        // validates fine (though logging a warning) and sets field to null
        cm3.validate( logger );

        assertTrue( "Expected warning for unsupported updated method name", logger
            .messageContains( "Ignoring updated method definition" ) );
        assertNull( rm3.getUpdated() );
    }


    public void test_reference_updated_ds11()
    {
        // updated method ignored for DS 1.1
        final ReferenceMetadata rm3 = createReferenceMetadata( "test" );
        rm3.setUpdated( "my_updated_method" );
        final ComponentMetadata cm3 = createComponentMetadata11( Boolean.TRUE, null );
        cm3.addDependency( rm3 );

        // validates fine (though logging a warning) and sets field to null
        cm3.validate( logger );

        assertTrue( "Expected warning for unsupported updated method name", logger
            .messageContains( "Ignoring updated method definition" ) );
        assertNull( rm3.getUpdated() );
    }


    public void test_reference_updated_ds11_felix()
    {
        // updated method accepted for DS 1.1-felix
        final ReferenceMetadata rm3 = createReferenceMetadata( "test" );
        rm3.setUpdated( "my_updated_method" );
        final ComponentMetadata cm3 = createComponentMetadata( XmlHandler.DS_VERSION_1_1_FELIX, Boolean.TRUE, null );
        cm3.addDependency( rm3 );

        // validates fine and logs no message
        cm3.validate( logger );

        assertEquals( "my_updated_method", rm3.getUpdated() );
    }


    public void test_duplicate_implementation_ds10()
    {
        final ComponentMetadata cm = createComponentMetadata( Boolean.TRUE, null );
        cm.setImplementationClassName( "second.implementation.class" );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for duplicate implementation element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_implementation_ds11()
    {
        final ComponentMetadata cm = createComponentMetadata11( Boolean.TRUE, null );
        cm.setImplementationClassName( "second.implementation.class" );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for duplicate implementation element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_service_ds10()
    {
        final ComponentMetadata cm = createComponentMetadata( Boolean.TRUE, null );
        cm.setService( createServiceMetadata( Boolean.TRUE ) );
        cm.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for duplicate service element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_service_ds11()
    {
        final ComponentMetadata cm = createComponentMetadata11( Boolean.TRUE, null );
        cm.setService( createServiceMetadata( Boolean.TRUE ) );
        cm.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for duplicate service element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_property_no_name_ds10()
    {
        final ComponentMetadata cm = createComponentMetadata( null, null );
        cm.addProperty( createPropertyMetadata( null, null, "" ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for missing property name" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_property_no_name_ds11()
    {
        final ComponentMetadata cm = createComponentMetadata11( null, null );
        cm.addProperty( createPropertyMetadata( null, null, "" ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for missing property name" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_property_char_ds10() throws ComponentException
    {
        final ComponentMetadata cm = createComponentMetadata( null, null );
        PropertyMetadata prop = createPropertyMetadata( "x", "Char", "x" );
        cm.addProperty( prop );
        cm.validate( logger );
        assertTrue( prop.getValue() instanceof Character );
        assertEquals( new Character( 'x' ), prop.getValue() );
    }


    public void test_property_char_ds11()
    {
        final ComponentMetadata cm = createComponentMetadata11( null, null );
        cm.addProperty( createPropertyMetadata( "x", "Char", "x" ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for illegal property type Char" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_property_character_ds10()
    {
        final ComponentMetadata cm = createComponentMetadata( null, null );
        cm.addProperty( createPropertyMetadata( "x", "Character", "x" ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for illegal property type Character" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_property_character_ds11() throws ComponentException
    {
        final ComponentMetadata cm = createComponentMetadata11( null, null );
        PropertyMetadata prop = createPropertyMetadata( "x", "Character", "x" );
        cm.addProperty( prop );
        cm.validate( logger );
        assertTrue( prop.getValue() instanceof Character );
        assertEquals( new Character( 'x' ), prop.getValue() );
    }


    //---------- Helper methods

    // Creates Component Metadata for the given namespace
    private ComponentMetadata createComponentMetadata( int nameSpaceCode, Boolean immediate, String factory )
    {
        ComponentMetadata meta = new ComponentMetadata( nameSpaceCode );
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

    // Creates DS 1.0 Component Metadata
    private ComponentMetadata createComponentMetadata( Boolean immediate, String factory )
    {
        return createComponentMetadata( XmlHandler.DS_VERSION_1_0, immediate, factory );
    }


    // Creates DS 1.1 Component Metadata
    private ComponentMetadata createComponentMetadata11( Boolean immediate, String factory )
    {
        return createComponentMetadata( XmlHandler.DS_VERSION_1_1, immediate, factory );
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


    private PropertyMetadata createPropertyMetadata( String propertyName, String type, String value )
    {
        PropertyMetadata meta = new PropertyMetadata();
        if ( propertyName != null )
        {
            meta.setName( propertyName );
        }
        if ( type != null )
        {
            meta.setType( type );
        }
        if ( value != null )
        {
            meta.setValue( value );
        }
        return meta;
    }
}
