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
package org.apache.felix.cm;


import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


public class MockPersistenceManager implements PersistenceManager
{

    private final Map configs = new HashMap();


    public void delete( String pid )
    {
        configs.remove( pid );
    }


    public boolean exists( String pid )
    {
        return configs.containsKey( pid );
    }


    public Enumeration getDictionaries()
    {
        return Collections.enumeration( configs.values() );
    }


    public Dictionary load( String pid ) throws IOException
    {
        Dictionary config = ( Dictionary ) configs.get( pid );
        if ( config != null )
        {
            return config;
        }

        throw new IOException( "No such configuration: " + pid );
    }


    public void store( String pid, Dictionary properties )
    {
        configs.put( pid, properties );
    }

}
