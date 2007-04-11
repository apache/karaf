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
import java.util.BitSet;

import junit.framework.TestCase;


public class FileNameTest extends TestCase
{

    public void testPidPlain()
    {
        assertEquals( "plain", encodePath( "plain" ) );
        assertEquals( "plain" + File.separatorChar + "path", encodePath( "plain.path" ) );
        assertEquals( "encod%00e8", encodePath( "encod\u00E8" ) );
        assertEquals( "encod%00e8" + File.separatorChar + "path", encodePath( "encod\u00E8/path" ) );
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
