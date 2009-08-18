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

package org.apache.felix.sigil.config;


import java.io.IOException;
import java.util.Map;
import java.util.Properties;


public class BldProperties extends Properties
{
    private static final long serialVersionUID = 1L;
    private static final Map<String, String> env = System.getenv();
    private static final Properties sys = System.getProperties();

    private final BldProject project;
    private String dot;
    private String dotdot;

    private static final BldProperties global = new BldProperties();


    private BldProperties()
    {
        this.project = null;
    }


    BldProperties( BldProject project ) throws NullPointerException
    {
        if ( project == null )
        {
            throw new NullPointerException();
        }
        this.project = project;
    }


    public String getProperty( String key, String defaultValue )
    {
        if ( project != null )
        {
            try
            {
                if ( ".".equals( key ) )
                {
                    if ( dot == null )
                    {
                        dot = project.resolve( "." ).getCanonicalPath();
                    }
                    return dot;
                }
                else if ( "..".equals( key ) )
                {
                    if ( dotdot == null )
                    {
                        dotdot = project.resolve( ".." ).getCanonicalPath();
                    }
                    return dotdot;
                }
            }
            catch ( IOException e )
            {
                throw new IllegalStateException( e );
            }
        }

        String val = sys.getProperty( key, env.get( key ) );

        if ( val == null )
        {
            val = defaultValue;
        }

        return val;
    }


    public String getProperty( String key )
    {
        return getProperty( key, null );
    }


    public static Properties global()
    {
        return global;
    }
}
