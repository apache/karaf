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
package org.apache.felix.scrplugin.helper;


public class StringUtils
{

    public static String[] split( String value, String sep )
    {
        return value.split( sep );
    }


    public static boolean isEmpty( final String string )
    {
        return string == null || string.length() == 0;
    }


    public static String leftPad( final String base, final int width, final String pad )
    {
        StringBuilder builder = new StringBuilder( width );

        int missing = width - base.length();
        while ( missing > 0 )
        {
            builder.append( pad );
            missing -= pad.length();
        }

        builder.append( base );

        return builder.toString();
    }
}
