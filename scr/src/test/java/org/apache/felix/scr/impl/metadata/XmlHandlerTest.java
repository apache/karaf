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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.felix.scr.impl.MockBundle;
import org.apache.felix.scr.impl.MockLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.PropertyMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.apache.felix.scr.impl.metadata.XmlHandler;
import org.apache.felix.scr.impl.parser.KXml2SAXParser;
import org.osgi.service.component.ComponentException;
import org.xmlpull.v1.XmlPullParserException;


public class XmlHandlerTest extends TestCase
{
    private MockLogger logger;


    protected void setUp() throws Exception
    {
        super.setUp();

        logger = new MockLogger();
    }


    public void test_no_namespace() throws Exception
    {
        final List metadataList = readMetadata( "/components_no_namespace.xml" );
        assertEquals( "1 Descriptor expected", 1, metadataList.size() );

        final ComponentMetadata metadata = ( ComponentMetadata ) metadataList.get( 0 );
        assertEquals( "Expect NS 1.0.0", XmlHandler.DS_VERSION_1_0, metadata.getNamespaceCode() );
    }


    public void test_component_attributes_11() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_activate_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );

        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );
        cm10.validate( logger );
        assertEquals( "DS Version 1.0", XmlHandler.DS_VERSION_1_0, cm10.getNamespaceCode() );
        assertFalse( "DS Version 1.0", cm10.isDS11() );
        assertEquals( "Expected Activate Method not set", "activate", cm10.getActivate() );
        assertFalse( "Activate method expected to not be declared", cm10.isActivateDeclared() );
        assertEquals( "Expected Deactivate Method not set", "deactivate", cm10.getDeactivate() );
        assertFalse( "Deactivate method expected to not be declared", cm10.isDeactivateDeclared() );
        assertNull( "Expected Modified Method not set", cm10.getModified() );
        assertEquals( "Expected Configuration Policy not set", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL, cm10
            .getConfigurationPolicy() );

        final List metadataList11 = readMetadata( "/components_activate_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );
        cm11.validate( logger );
        assertEquals( "DS Version 1.1", XmlHandler.DS_VERSION_1_1, cm11.getNamespaceCode() );
        assertEquals( "Expected Activate Method set", "myactivate", cm11.getActivate() );
        assertTrue( "Activate method expected to be declared", cm11.isActivateDeclared() );
        assertEquals( "Expected Deactivate Method set", "mydeactivate", cm11.getDeactivate() );
        assertTrue( "Activate method expected to be declared", cm11.isDeactivateDeclared() );
        assertEquals( "Expected Modified Method set", "mymodified", cm11.getModified() );
        assertEquals( "Expected Configuration Policy set", ComponentMetadata.CONFIGURATION_POLICY_IGNORE, cm11
            .getConfigurationPolicy() );
    }


    public void test_component_no_name() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_anonymous_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );
        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );
        try
        {
            cm10.validate( logger );
            fail( "Expected validation failure for component without name" );
        }
        catch ( ComponentException ce )
        {
            // expected !!
        }

        final List metadataList11 = readMetadata( "/components_anonymous_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );
        cm11.validate( logger );
        assertEquals( "Expected name equals class", cm11.getImplementationClassName(), cm11.getName() );
    }


    public void test_reference_no_name() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_anonymous_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );
        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );
        try
        {
            cm10.validate( logger );
            fail( "Expected validation failure for component without name" );
        }
        catch ( ComponentException ce )
        {
            // expected !!
        }

        final List metadataList11 = readMetadata( "/components_anonymous_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );
        cm11.validate( logger );
        assertEquals( "Expected name equals class", cm11.getImplementationClassName(), cm11.getName() );
    }


    public void test_all_elements_10() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_all_elements_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );
        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );

        // dont validate this, we test the raw reading

        // ds namespace
        assertEquals( "DS Version 1.0", XmlHandler.DS_VERSION_1_0, cm10.getNamespaceCode() );
        assertFalse( "DS Version 1.0", cm10.isDS11() );

        // base component attributes
        assertEquals( "component name", true, cm10.isEnabled() );
        assertEquals( "component name", "components.all.name", cm10.getName() );
        assertEquals( "component name", "components.all.factory", cm10.getFactoryIdentifier() );
        assertEquals( "component name", true, cm10.isFactory() );
        assertEquals( "component name", true, cm10.isImmediate() );

        // ds 1.1 elements
        assertEquals( "activate method", "myactivate", cm10.getActivate() );
        assertEquals( "deactivate method", "mydeactivate", cm10.getDeactivate() );
        assertTrue( "Activate method expected to be declared", cm10.isActivateDeclared() );
        assertEquals( "modified method", "mymodified", cm10.getModified() );
        assertTrue( "Deactivate method expected to be declared", cm10.isDeactivateDeclared() );
        assertEquals( "configuration policy", "ignore", cm10.getConfigurationPolicy() );

        // from the implementation element
        assertEquals( "component name", "components.all.impl", cm10.getImplementationClassName() );

        // property setting
        final PropertyMetadata prop = getPropertyMetadata( cm10, "prop" );
        assertNotNull( "prop exists", prop );
        assertEquals( "prop type", "Integer", prop.getType() );
        assertEquals( "prop value", 1234, ( ( Integer ) prop.getValue() ).intValue() );

        final PropertyMetadata file_property = getPropertyMetadata( cm10, "file.property" );
        assertNotNull( "file.property exists", file_property );
        assertEquals( "file.property type", "String", file_property.getType() );
        assertEquals( "file.property value", "Property from File", file_property.getValue() );

        // service setup
        final ServiceMetadata sm = cm10.getServiceMetadata();
        assertNotNull( "service", sm );
        assertEquals( "servicefactory", true, sm.isServiceFactory() );
        assertEquals( "1 interface", 1, sm.getProvides().length );
        assertEquals( "service interface", "components.all.service", sm.getProvides()[0] );

        // references - basic
        final ReferenceMetadata rm = getReference( cm10, "ref.name" );
        assertNotNull( "refeference ref.name", rm );
        assertEquals( "ref.name name", "ref.name", rm.getName() );
        assertEquals( "ref.name interface", "ref.service", rm.getInterface() );
        assertEquals( "ref.name cardinality", "0..n", rm.getCardinality() );
        assertEquals( "ref.name policy", "dynamic", rm.getPolicy() );
        assertEquals( "ref.name target", "ref.target", rm.getTarget() );
        assertEquals( "ref.name target prop name", "ref.name.target", rm.getTargetPropertyName() );
        assertEquals( "ref.name bind method", "ref_bind", rm.getBind() );
        assertEquals( "ref.name undbind method", "ref_unbind", rm.getUnbind() );

        // references - cardinality side properties (isOptional, isMultiple)
        final ReferenceMetadata rm01 = getReference( cm10, "ref.01" );
        assertNotNull( "refeference ref.01", rm01 );
        assertEquals( "ref.01 cardinality", "0..1", rm01.getCardinality() );
        final ReferenceMetadata rm11 = getReference( cm10, "ref.11" );
        assertNotNull( "refeference ref.11", rm11 );
        assertEquals( "ref.11 cardinality", "1..1", rm11.getCardinality() );
        final ReferenceMetadata rm0n = getReference( cm10, "ref.0n" );
        assertNotNull( "refeference ref.0n", rm0n );
        assertEquals( "ref.0n cardinality", "0..n", rm0n.getCardinality() );
        final ReferenceMetadata rm1n = getReference( cm10, "ref.1n" );
        assertNotNull( "refeference ref.1n", rm1n );
        assertEquals( "ref.1n cardinality", "1..n", rm1n.getCardinality() );
    }


    public void test_duplicate_implementation_class_10() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_duplicate_implementation_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );
        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );
        try
        {
            cm10.validate( logger );
            fail( "Expect validation failure for duplicate implementation element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_implementation_class_11() throws Exception
    {
        final List metadataList11 = readMetadata( "/components_duplicate_implementation_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );
        try
        {
            cm11.validate( logger );
            fail( "Expect validation failure for duplicate implementation element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_service_10() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_duplicate_service_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );
        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );
        try
        {
            cm10.validate( logger );
            fail( "Expect validation failure for duplicate service element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_service_11() throws Exception
    {
        final List metadataList11 = readMetadata( "/components_duplicate_service_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );
        try
        {
            cm11.validate( logger );
            fail( "Expect validation failure for duplicate service element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    //---------- helper

    private List readMetadata( String filename ) throws IOException, ComponentException, XmlPullParserException,
        Exception
    {
        BufferedReader in = new BufferedReader( new InputStreamReader( getClass().getResourceAsStream( filename ),
            "UTF-8" ) );
        final KXml2SAXParser parser = new KXml2SAXParser( in );

        XmlHandler handler = new XmlHandler( new MockBundle(), logger );
        parser.parseXML( handler );

        return handler.getComponentMetadataList();
    }


    private ReferenceMetadata getReference( final ComponentMetadata cm, final String name )
    {
        List rmlist = cm.getDependencies();
        for ( Iterator rmi = rmlist.iterator(); rmi.hasNext(); )
        {
            ReferenceMetadata rm = ( ReferenceMetadata ) rmi.next();
            if ( name.equals( rm.getName() ) )
            {
                return rm;
            }
        }

        // none found
        return null;
    }


    private PropertyMetadata getPropertyMetadata( final ComponentMetadata cm, final String name )
    {
        List pmlist = cm.getPropertyMetaData();
        for ( Iterator pmi = pmlist.iterator(); pmi.hasNext(); )
        {
            PropertyMetadata pm = ( PropertyMetadata ) pmi.next();
            if ( name.equals( pm.getName() ) )
            {
                return pm;
            }
        }

        // none found
        return null;
    }
}
