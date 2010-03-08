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

package org.apache.felix.sigil.eclipse.internal.repository.eclipse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.felix.sigil.repository.IBundleRepository;

public class RepositoryMap
{
    static class RepositoryCache
    {
        final Properties pref;
        final IBundleRepository repo;


        RepositoryCache( Properties pref, IBundleRepository repo )
        {
            this.pref = pref;
            this.repo = repo;
        }
    }
    
    private HashMap<String, RepositoryCache> cachedRepositories = new HashMap<String, RepositoryCache>();

    synchronized void retainAll(Collection<String> ids)
    {
        for ( Iterator<String> i = cachedRepositories.keySet().iterator(); i.hasNext(); )
        {
            if ( !ids.contains( i.next() ) )
            {
                i.remove();
            }
        }
    }

    synchronized RepositoryCache get(String id)
    {
        // TODO Auto-generated method stub
        return null;
    }

    synchronized void put(String id, RepositoryCache cache)
    {
        // TODO Auto-generated method stub
        
    }


}
