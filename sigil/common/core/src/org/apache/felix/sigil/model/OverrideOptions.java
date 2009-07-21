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

package org.apache.felix.sigil.model;


import java.util.HashMap;
import java.util.Map;


/**
 * @author dave
 * 
 */
public enum OverrideOptions
{
    NO("no"), MAY("may"), MUST("must");

    private String str;

    private static Map<String, OverrideOptions> map = new HashMap<String, OverrideOptions>();

    static
    {
        for ( OverrideOptions option : OverrideOptions.values() )
        {
            map.put( option.str.toLowerCase(), option );
        }
    }


    private OverrideOptions( String str )
    {
        this.str = str;
    }


    public static OverrideOptions parse( String val )
    {
        OverrideOptions option = map.get( val.toLowerCase() );

        if ( option == null )
        {
            throw new IllegalArgumentException( "Invalid override value " + val );
        }

        return option;
    }


    @Override
    public String toString()
    {
        return str;
    }

}
