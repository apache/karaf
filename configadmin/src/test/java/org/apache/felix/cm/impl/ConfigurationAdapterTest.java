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
package org.apache.felix.cm.impl;


import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.felix.cm.MockPersistenceManager;
import org.apache.felix.cm.PersistenceManager;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;


public class ConfigurationAdapterTest extends TestCase
{

    private static final String SCALAR = "scalar";
    private static final String STRING_VALUE = "String Value";
    private static final String STRING_VALUE2 = "Another String Value";

    private static final String ARRAY = "array";
    private final String[] ARRAY_VALUE;

    private static final String COLLECTION = "collection";
    private final Collection COLLECTION_VALUE;

    private static final String TEST_PID = "test.pid";
    private static final String TEST_LOCATION = "test:location";

    private final PersistenceManager pm = new MockPersistenceManager();
    private final MockConfigurationManager configMgr = new MockConfigurationManager();

    {
        ARRAY_VALUE = new String[]
            { STRING_VALUE };
        COLLECTION_VALUE = new ArrayList();
        COLLECTION_VALUE.add( STRING_VALUE );
    }


    private Configuration getConfiguration() throws IOException
    {
        ConfigurationImpl cimpl = new ConfigurationImpl( configMgr, pm, TEST_PID, null, TEST_LOCATION );
        return new ConfigurationAdapter( null, cimpl );
    }


    public void testScalar() throws IOException
    {
        Configuration cimpl = getConfiguration();
        Dictionary props = cimpl.getProperties();
        assertNull( "Configuration is fresh", props );

        props = new Hashtable();
        props.put( SCALAR, STRING_VALUE );
        cimpl.update( props );

        Dictionary newProps = cimpl.getProperties();
        assertNotNull( "Configuration is not fresh", newProps );
        assertEquals( "Expect 2 elements", 2, newProps.size() );
        assertEquals( "Service.pid must match", TEST_PID, newProps.get( Constants.SERVICE_PID ) );
        assertEquals( "Scalar value must match", STRING_VALUE, newProps.get( SCALAR ) );
    }


    public void testArray() throws IOException
    {
        Configuration cimpl = getConfiguration();

        Dictionary props = cimpl.getProperties();
        assertNull( "Configuration is fresh", props );

        props = new Hashtable();
        props.put( ARRAY, ARRAY_VALUE );
        cimpl.update( props );

        Dictionary newProps = cimpl.getProperties();
        assertNotNull( "Configuration is not fresh", newProps );
        assertEquals( "Expect 2 elements", 2, newProps.size() );
        assertEquals( "Service.pid must match", TEST_PID, newProps.get( Constants.SERVICE_PID ) );

        Object testProp = newProps.get( ARRAY );
        assertNotNull( testProp );
        assertTrue( testProp.getClass().isArray() );
        assertEquals( 1, Array.getLength( testProp ) );
        assertEquals( STRING_VALUE, Array.get( testProp, 0 ) );

        // modify the array property
        Array.set( testProp, 0, STRING_VALUE2 );

        // the array element change must not be reflected in the configuration
        Dictionary newProps2 = cimpl.getProperties();
        Object testProp2 = newProps2.get( ARRAY );
        assertNotNull( testProp2 );
        assertTrue( testProp2.getClass().isArray() );
        assertEquals( 1, Array.getLength( testProp2 ) );
        assertEquals( STRING_VALUE, Array.get( testProp2, 0 ) );
    }


    public void testCollection() throws IOException
    {
        Configuration cimpl = getConfiguration();

        Dictionary props = cimpl.getProperties();
        assertNull( "Configuration is fresh", props );

        props = new Hashtable();
        props.put( COLLECTION, COLLECTION_VALUE );
        cimpl.update( props );

        Dictionary newProps = cimpl.getProperties();
        assertNotNull( "Configuration is not fresh", newProps );
        assertEquals( "Expect 2 elements", 2, newProps.size() );
        assertEquals( "Service.pid must match", TEST_PID, newProps.get( Constants.SERVICE_PID ) );

        Object testProp = newProps.get( COLLECTION );
        assertNotNull( testProp );
        assertTrue( testProp instanceof Collection );
        Collection coll = ( Collection ) testProp;
        assertEquals( 1, coll.size() );
        assertEquals( STRING_VALUE, coll.iterator().next() );

        // modify the array property
        coll.clear();
        coll.add( STRING_VALUE2 );

        // the array element change must not be reflected in the configuration
        Dictionary newProps2 = cimpl.getProperties();
        Object testProp2 = newProps2.get( COLLECTION );
        assertNotNull( testProp2 );
        assertTrue( testProp2 instanceof Collection );
        Collection coll2 = ( Collection ) testProp2;
        assertEquals( 1, coll2.size() );
        assertEquals( STRING_VALUE, coll2.iterator().next() );
    }
}
