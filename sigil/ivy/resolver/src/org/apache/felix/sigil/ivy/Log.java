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

package org.apache.felix.sigil.ivy;


import org.apache.ivy.util.Message;


// ensure common prefix to all sigil messages
public class Log
{
    public static final String PREFIX = "Sigil: ";

    private static final boolean highlight = false;


    public static void error( String msg )
    {
        if ( highlight )
            Message.deprecated( PREFIX + "[error] " + msg );
        Message.error( PREFIX + msg );
    }


    public static void warn( String msg )
    {
        if ( highlight )
            Message.deprecated( PREFIX + "[warn] " + msg );
        else
            Message.warn( PREFIX + msg );
    }


    public static void info( String msg )
    {
        if ( highlight )
            Message.deprecated( PREFIX + "[info] " + msg );
        else
            Message.info( PREFIX + msg );
    }


    public static void verbose( String msg )
    {
        Message.verbose( PREFIX + "[verbose] " + msg );
    }


    public static void debug( String msg )
    {
        if ( highlight )
            Message.deprecated( PREFIX + "[debug] " + msg );
        else
            Message.debug( PREFIX + msg );
    }

}
