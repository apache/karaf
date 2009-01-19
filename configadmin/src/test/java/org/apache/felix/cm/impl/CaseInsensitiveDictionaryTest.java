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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;


public class CaseInsensitiveDictionaryTest extends TestCase
{

    public void testCheckValueNull()
    {
        // null which must throw IllegalArgumentException
        try
        {
            CaseInsensitiveDictionary.checkValue( null );
            fail( "Expected IllegalArgumentException for null value" );
        }
        catch ( IllegalArgumentException iae )
        {

        }

    }


    public void testCheckValueSimple()
    {
        internalTestCheckValue( "String" );
        internalTestCheckValue( new Integer( 1 ) );
        internalTestCheckValue( new Long( 1 ) );
        internalTestCheckValue( new Float( 1 ) );
        internalTestCheckValue( new Double( 1 ) );
        internalTestCheckValue( new Byte( ( byte ) 1 ) );
        internalTestCheckValue( new Short( ( short ) 1 ) );
        internalTestCheckValue( new Character( 'a' ) );
        internalTestCheckValue( Boolean.TRUE );
    }


    public void testCheckValueSimpleArray()
    {
        internalTestCheckValue( new String[]
            { "String" } );
        internalTestCheckValue( new Integer[]
            { new Integer( 1 ) } );
        internalTestCheckValue( new Long[]
            { new Long( 1 ) } );
        internalTestCheckValue( new Float[]
            { new Float( 1 ) } );
        internalTestCheckValue( new Double[]
            { new Double( 1 ) } );
        internalTestCheckValue( new Byte[]
            { new Byte( ( byte ) 1 ) } );
        internalTestCheckValue( new Short[]
            { new Short( ( short ) 1 ) } );
        internalTestCheckValue( new Character[]
            { new Character( 'a' ) } );
        internalTestCheckValue( new Boolean[]
            { Boolean.TRUE } );
    }


    public void testCheckValuePrimitiveArray()
    {
        internalTestCheckValue( new long[]
            { 1 } );
        internalTestCheckValue( new int[]
            { 1 } );
        internalTestCheckValue( new short[]
            { 1 } );
        internalTestCheckValue( new char[]
            { 1 } );
        internalTestCheckValue( new byte[]
            { 1 } );
        internalTestCheckValue( new double[]
            { 1 } );
        internalTestCheckValue( new float[]
            { 1 } );
        internalTestCheckValue( new boolean[]
            { true } );
    }


    public void testCheckValueSimpleVector()
    {
        internalTestCheckValue( "String", Vector.class );
        internalTestCheckValue( new Integer( 1 ), Vector.class );
        internalTestCheckValue( new Long( 1 ), Vector.class );
        internalTestCheckValue( new Float( 1 ), Vector.class );
        internalTestCheckValue( new Double( 1 ), Vector.class );
        internalTestCheckValue( new Byte( ( byte ) 1 ), Vector.class );
        internalTestCheckValue( new Short( ( short ) 1 ), Vector.class );
        internalTestCheckValue( new Character( 'a' ), Vector.class );
        internalTestCheckValue( Boolean.TRUE, Vector.class );
    }


    public void testCheckValueSimpleSet()
    {
        internalTestCheckValue( "String", HashSet.class );
        internalTestCheckValue( new Integer( 1 ), HashSet.class );
        internalTestCheckValue( new Long( 1 ), HashSet.class );
        internalTestCheckValue( new Float( 1 ), HashSet.class );
        internalTestCheckValue( new Double( 1 ), HashSet.class );
        internalTestCheckValue( new Byte( ( byte ) 1 ), HashSet.class );
        internalTestCheckValue( new Short( ( short ) 1 ), HashSet.class );
        internalTestCheckValue( new Character( 'a' ), HashSet.class );
        internalTestCheckValue( Boolean.TRUE, HashSet.class );
    }


    public void testCheckValueSimpleArrayList()
    {
        internalTestCheckValue( "String", ArrayList.class );
        internalTestCheckValue( new Integer( 1 ), ArrayList.class );
        internalTestCheckValue( new Long( 1 ), ArrayList.class );
        internalTestCheckValue( new Float( 1 ), ArrayList.class );
        internalTestCheckValue( new Double( 1 ), ArrayList.class );
        internalTestCheckValue( new Byte( ( byte ) 1 ), ArrayList.class );
        internalTestCheckValue( new Short( ( short ) 1 ), ArrayList.class );
        internalTestCheckValue( new Character( 'a' ), ArrayList.class );
        internalTestCheckValue( Boolean.TRUE, ArrayList.class );
    }


    private void internalTestCheckValue( Object value, Class collectionType )
    {
        Collection coll;
        try
        {
            coll = ( Collection ) collectionType.newInstance();
        }
        catch ( Throwable t )
        {
            throw new IllegalArgumentException( collectionType + " cannot be instantiated as a Collection" );
        }

        coll.add( value );
        internalTestCheckValue( coll );
    }


    private void internalTestCheckValue( Object value )
    {
        assertEqualValue( value, CaseInsensitiveDictionary.checkValue( value ) );
    }


    private void assertEqualValue( Object expected, Object actual )
    {
        if ( ( expected instanceof Collection ) && ( actual instanceof Collection ) )
        {
            Collection eColl = ( Collection ) expected;
            Collection aColl = ( Collection ) actual;
            if ( eColl.size() != aColl.size() )
            {
                fail( "Unexpected size. expected:" + eColl.size() + ", actual: " + aColl.size() );
            }

            // create a list from the expected collection and remove
            // all values from the actual collection, this should get
            // an empty collection
            List eList = new ArrayList( eColl );
            eList.removeAll( aColl );
            assertTrue( "Collections do not match. expected:" + eColl + ", actual: " + aColl, eList.isEmpty() );
        }
        else
        {
            assertEquals( expected, actual );
        }
    }


    public void testValidKeys()
    {
        CaseInsensitiveDictionary.checkKey( "a" );
        CaseInsensitiveDictionary.checkKey( "1" );
        CaseInsensitiveDictionary.checkKey( "-" );
        CaseInsensitiveDictionary.checkKey( "_" );
        CaseInsensitiveDictionary.checkKey( "A" );
        CaseInsensitiveDictionary.checkKey( "a.b.c" );
        CaseInsensitiveDictionary.checkKey( "a.1.c" );
        CaseInsensitiveDictionary.checkKey( "a-sample.dotted_key.end" );
    }


    public void testKeyDots()
    {
        testFailingKey( "." );
        testFailingKey( ".a.b.c" );
        testFailingKey( "a.b.c." );
        testFailingKey( ".a.b.c." );
        testFailingKey( "a..b" );
    }


    public void testKeyIllegalCharacters()
    {
        testFailingKey( " " );
        testFailingKey( "§" );
        testFailingKey( "${yikes}" );
        testFailingKey( "a key with spaces" );
        testFailingKey( "fail:key" );
    }


    private void testFailingKey( String key )
    {
        try
        {
            CaseInsensitiveDictionary.checkKey( key );
            fail( "Expected IllegalArgumentException for key [" + key + "]" );
        }
        catch ( IllegalArgumentException iae )
        {
            // expected
        }
    }
}
