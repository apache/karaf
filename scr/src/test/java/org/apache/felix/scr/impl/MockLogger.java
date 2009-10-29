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
package org.apache.felix.scr.impl;


import java.text.MessageFormat;

import org.apache.felix.scr.impl.helper.Logger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;


public class MockLogger implements Logger
{
    String lastMessage;


    public boolean isLogEnabled( int level )
    {
        return true;
    }


    public void log( int level, String pattern, Object[] arguments, ComponentMetadata metadata, Throwable ex )
    {
        if ( isLogEnabled( level ) )
        {
            log( level, MessageFormat.format( pattern, arguments ), metadata, ex );
        }
    }


    public void log( int level, String message, ComponentMetadata metadata, Throwable ex )
    {
        lastMessage = message;
    }


    public boolean messageContains( String value )
    {
        return lastMessage != null && lastMessage.indexOf( value ) >= 0;
    }
}