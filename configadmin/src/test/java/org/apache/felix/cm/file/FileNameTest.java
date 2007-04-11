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
import java.util.BitSet;

import junit.framework.TestCase;


public class FileNameTest extends TestCase
{

    public void testPidPlain()
    {
        assertEquals( "plain", encodePath( "plain" ) );
        assertEquals( "plain" + File.separatorChar + "path", encodePath( "plain.path" ) );
        assertEquals( "encod%00e8", encodePath( "encodè" ) );
        assertEquals( "encod%00e8" + File.separatorChar + "path", encodePath( "encodè/path" ) );
        assertEquals( "encode" + File.separatorChar + "%1234" + File.separatorChar + "path", encodePath( "encode/\u1234/path" ) );
        assertEquals( "encode" + File.separatorChar + " %0025 " + File.separatorChar + "path", encodePath( "encode/ % /path" ) );
    }

    private static final BitSet VALID_PATH_CHARS;

    static
    {
        VALID_PATH_CHARS = new BitSet();

        for ( int i = 'a'; i <= 'z'; i++ )
        {
            VALID_PATH_CHARS.set( i );
        }
        for ( int i = 'A'; i <= 'Z'; i++ )
        {
            VALID_PATH_CHARS.set( i );
        }
        for ( int i = '0'; i <= '9'; i++ )
        {
            VALID_PATH_CHARS.set( i );
        }
        VALID_PATH_CHARS.set( File.separatorChar );
        VALID_PATH_CHARS.set( ' ' );
        VALID_PATH_CHARS.set( '-' );
        VALID_PATH_CHARS.set( '_' );
    }


    private String encodePath( String pid )
    {
        // replace dots by File.separatorChar
        pid = pid.replace( '.', File.separatorChar );

        // replace slash by File.separatorChar if different
        if (File.separatorChar != '/') {
            pid = pid.replace( '/', File.separatorChar );
        }

        // scan for first non-valid character (if any)
        int first = 0;
        while ( first < pid.length() && VALID_PATH_CHARS.get( pid.charAt( first ) ) )
        {
            first++;
        }

        // check whether we exhausted
        if ( first < pid.length() )
        {
            StringBuffer buf = new StringBuffer( pid.substring( 0, first ) );

            for ( int i = first; i < pid.length(); i++ )
            {
                char c = pid.charAt( i );
                if ( VALID_PATH_CHARS.get( c ) )
                {
                    buf.append( c );
                }
                else
                {
                    String val = "000" + Integer.toHexString( c );
                    buf.append( '%' );
                    buf.append( val.substring( val.length() - 4 ) );
                }
            }
            
            return buf.toString();
        }
        
        return pid;
    }
}
