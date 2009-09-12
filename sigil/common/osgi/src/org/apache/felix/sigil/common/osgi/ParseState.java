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

package org.apache.felix.sigil.common.osgi;


import java.io.Serializable;


/**
 * @author dave
 * 
 */
class ParseState implements Serializable
{
    private static final long serialVersionUID = 1L;

    int pos;

    String str;


    ParseState( String str )
    {
        this.str = str;
    }


    public boolean lookingAt( String start )
    {
        return str.substring( pos ).startsWith( start );
    }


    public CharSequence skip( int n )
    {
        int end = pos + n < str.length() ? pos + n : str.length();
        int start = pos;
        pos = end;
        return str.subSequence( start, end );
    }


    public char read()
    {
        char ch = str.charAt( pos );
        if ( pos < str.length() )
        {
            pos++;
        }
        return ch;
    }


    public char readAndSkipWhiteSpace()
    {
        char ch = read();
        skipWhitespace();
        return ch;
    }


    char peek()
    {
        if ( isEndOfString() )
        {
            return ( char ) -1;
        }
        return str.charAt( pos );
    }


    boolean isEndOfString()
    {
        return pos == str.length();
    }


    void skipWhitespace()
    {
        while ( pos < str.length() && Character.isWhitespace( str.charAt( pos ) ) )
        {
            pos++;
        }
    }
}
