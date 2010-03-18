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
package org.apache.felix.utils.version;

import java.util.WeakHashMap;

import org.osgi.framework.Version;

/**
 * Cache of Versions backed by a WeakHashMap to conserve memory.
 * 
 * VersionTable.getVersion should be used in preference to new Version() or Version.parseVersion.
 * 
 * @author dave
 *
 */
public final class VersionTable
{
    private static final WeakHashMap versions = new WeakHashMap();

    private VersionTable() { }
    
    public static Version getVersion(String version)
    {
        return getVersion( version, true );
    }

    public static Version getVersion(String version, boolean clean)
    {
        if (clean)
        {
            version = VersionCleaner.clean(version);
        }
        synchronized( versions )
        {
            Version v = (Version) versions.get(version);
            if ( v == null )
            {
                v = Version.parseVersion(version);
                versions.put(version, v);
            }
            return v;
        }
    }

    public static Version getVersion(int major, int minor, int micro)
    {
        return getVersion(major, minor, micro, null);
    }

    public static Version getVersion(int major, int minor, int micro, String qualifier)
    {
        String key;
        
        if ( qualifier == null || qualifier.length() == 0 )
        {
            key = major + "." + minor + "." + micro;
        }
        else
        {
            key = major + "." + minor + "." + micro + "." + qualifier;            
        }
        
        synchronized( versions )
        {
            Version v = (Version) versions.get(key);
            if ( v == null )
            {
                v = new Version(major, minor, micro, qualifier);
                versions.put(key, v);
            }
            return v;
        }
    }
}
