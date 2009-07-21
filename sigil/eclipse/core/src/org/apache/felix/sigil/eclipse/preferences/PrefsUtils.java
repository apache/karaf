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

package org.apache.felix.sigil.eclipse.preferences;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.text.StrTokenizer;


public class PrefsUtils
{

    private PrefsUtils()
    {
    }


    public static final String arrayToString( String[] array )
    {
        StringBuilder builder = new StringBuilder();

        for ( int i = 0; i < array.length; i++ )
        {
            if ( i > 0 )
                builder.append( ',' );
            builder.append( StringEscapeUtils.escapeCsv( array[i] ) );
        }

        return builder.toString();
    }


    public static final String[] stringToArray( String string )
    {
        StrTokenizer tokenizer = new StrTokenizer( string, ',', '"' );
        String[] array = new String[tokenizer.size()];

        for ( int i = 0; i < array.length; i++ )
        {
            array[i] = tokenizer.nextToken();
        }

        return array;
    }


    public static String listToString( List<String> names )
    {
        return arrayToString( names.toArray( new String[names.size()] ) );
    }


    public static List<String> stringToList( String string )
    {
        return new ArrayList<String>( Arrays.asList( stringToArray( string ) ) );
    }
}
