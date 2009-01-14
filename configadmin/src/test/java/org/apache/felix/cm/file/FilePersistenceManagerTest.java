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
package org.apache.felix.cm.file;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import junit.framework.TestCase;


/**
 * The <code>FilePersistenceManagerTest</code> TODO
 *
 * @author fmeschbe
 */
public class FilePersistenceManagerTest extends TestCase
{
    private File file = new File( System.getProperty( "java.io.tmpdir" ), "config" );

    private FilePersistenceManager fpm;


    protected void setUp() throws Exception
    {
        super.setUp();

        fpm = new FilePersistenceManager( file.getAbsolutePath() );
    }


    protected void tearDown() throws Exception
    {
        File[] children = file.listFiles();
        for ( int i = 0; children != null && i < children.length; i++ )
        {
            children[i].delete();
        }
        file.delete();

        super.tearDown();
    }


    public void testPidPlain()
    {
        assertEquals( "plain", FilePersistenceManager.encodePid( "plain" ) );
        assertEquals( "plain" + File.separatorChar + "path", FilePersistenceManager.encodePid( "plain.path" ) );
        assertEquals( "encod%00e8", FilePersistenceManager.encodePid( "encod\u00E8" ) );
        assertEquals( "encod%00e8" + File.separatorChar + "path", FilePersistenceManager.encodePid( "encod\u00E8/path" ) );
        assertEquals( "encode" + File.separatorChar + "%1234" + File.separatorChar + "path", FilePersistenceManager
            .encodePid( "encode/\u1234/path" ) );
        assertEquals( "encode" + File.separatorChar + " %0025 " + File.separatorChar + "path", FilePersistenceManager
            .encodePid( "encode/ % /path" ) );
    }


    public void testCreateDir()
    {
        assertTrue( file.isDirectory() );
    }


    public void testSimple() throws IOException
    {
        check( "String", "String Value" );
        check( "Integer", new Integer( 2 ) );
        check( "Long", new Long( 2 ) );
        check( "Float", new Float( 2 ) );
        check( "Double", new Double( 2 ) );
        check( "Byte", new Byte( ( byte ) 2 ) );
        check( "Short", new Short( ( short ) 2 ) );
        check( "Character", new Character( 'a' ) );
        check( "Boolean", Boolean.TRUE );
    }


    public void testQuoting() throws IOException
    {
        check( "QuotingSeparators", "\\()[]{}.,=" );
        check( "QuotingWellKnown", "BSP:\b, TAB:\t, LF:\n, FF:\f, CR:\r" );
        check( "QuotingControl", new String( new char[]
            { 5, 10, 32, 64 } ) );
    }


    public void testArray() throws IOException
    {
        check( "StringArray", new String[]
            { "one", "two", "three" } );
        check( "IntArray", new int[]
            { 0, 1, 2 } );
        check( "IntegerArray", new Integer[]
            { new Integer( 0 ), new Integer( 1 ), new Integer( 2 ) } );
    }


    public void testVector() throws IOException
    {
        check( "StringVector", new Vector( Arrays.asList( new String[]
                                                                     { "one", "two", "three" } ) ) );
        check( "IntegerVector", new Vector( Arrays.asList( new Integer[]
                                                                       { new Integer( 0 ), new Integer( 1 ), new Integer( 2 ) } ) ) );
    }
    
    
    public void testList() throws IOException
    {
        check( "StringList", Arrays.asList( new String[]
            { "one", "two", "three" } ) );
        check( "IntegerList", Arrays.asList( new Integer[]
            { new Integer( 0 ), new Integer( 1 ), new Integer( 2 ) } ) );
    }


    public void testMultiValue() throws IOException
    {
        Dictionary props = new Hashtable();
        props.put( "String", "String Value" );
        props.put( "Integer", new Integer( 2 ) );
        props.put( "Long", new Long( 2 ) );
        props.put( "Float", new Float( 2 ) );
        props.put( "Double", new Double( 2 ) );
        props.put( "Byte", new Byte( ( byte ) 2 ) );
        props.put( "Short", new Short( ( short ) 2 ) );
        props.put( "Character", new Character( 'a' ) );
        props.put( "Boolean", Boolean.TRUE );
        props.put( "Array", new boolean[]
            { true, false } );

        check( "MultiValue", props );
    }


    private void check( String name, Object value ) throws IOException
    {
        Dictionary props = new Hashtable();
        props.put( name, value );

        check( name, props );
    }


    private void check( String pid, Dictionary props ) throws IOException
    {
        fpm.store( pid, props );

        assertTrue( new File( file, pid + ".config" ).exists() );

        Dictionary loaded = fpm.load( pid );
        assertNotNull( loaded );
        assertEquals( props.size(), loaded.size() );

        for ( Enumeration pe = props.keys(); pe.hasMoreElements(); )
        {
            String key = ( String ) pe.nextElement();
            checkValues( props.get( key ), loaded.get( key ) );
        }
    }


    private void checkValues( Object value1, Object value2 )
    {
        assertNotNull( value2 );
        if ( value1.getClass().isArray() )
        {
            assertTrue( value2.getClass().isArray() );
            assertEquals( value1.getClass().getComponentType(), value2.getClass().getComponentType() );
            assertEquals( Array.getLength( value1 ), Array.getLength( value2 ) );
            for ( int i = 0; i < Array.getLength( value1 ); i++ )
            {
                assertEquals( Array.get( value1, i ), Array.get( value2, i ) );
            }
        }
        else
        {
            assertEquals( value1, value2 );
        }
    }
}
