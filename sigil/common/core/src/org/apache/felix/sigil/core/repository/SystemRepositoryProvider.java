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

package org.apache.felix.sigil.core.repository;


import java.io.IOException;

import java.io.InputStream;
import java.util.Properties;

import org.apache.felix.sigil.repository.IBundleRepository;
import org.apache.felix.sigil.repository.IRepositoryProvider;
import org.apache.felix.sigil.repository.RepositoryException;
import org.eclipse.core.runtime.Path;

public class SystemRepositoryProvider implements IRepositoryProvider
{

    public IBundleRepository createRepository( String id, Properties properties ) throws RepositoryException
    {
        String fw = properties.getProperty( "framework" );
        Path frameworkPath = fw == null ? null : new Path( fw );
        String extraPkgs = properties.getProperty( "packages" );
        String profile = properties.getProperty( "profile" );
        
        try
        {
            Properties p = readProfile( profile );
            String pkgs = p.getProperty( "org.osgi.framework.system.packages" ) + "," + extraPkgs;
            return new SystemRepository( id, frameworkPath, pkgs );
        }
        catch ( IOException e )
        {
            throw new RepositoryException( "Failed to load profile", e );
        }
    }


    public static Properties readProfile( String name ) throws IOException
    {
        if ( name == null )
        {
            String version = System.getProperty( "java.specification.version" );
            String[] split = version.split( "\\." );
            String prefix = ( "6".compareTo( split[1] ) <= 0 ) ? "JavaSE-" : "J2SE-";
            name = prefix + version;
        }

        String profilePath = "profiles/" + name + ".profile";
        InputStream in = SystemRepositoryProvider.class.getClassLoader().getResourceAsStream( profilePath );

        if ( in == null )
            throw new IOException( "No such profile: " + profilePath );

        Properties p = new Properties();
        p.load( in );

        return p;
    }

}
