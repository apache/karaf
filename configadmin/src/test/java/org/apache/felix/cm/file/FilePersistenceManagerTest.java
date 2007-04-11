/*
 * $Url: $
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
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
 * @version $Rev:$, $Date:$
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
