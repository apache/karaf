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

package org.apache.felix.sigil.core;


import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.config.BldFactory;
import org.apache.felix.sigil.config.IBldProject;
import org.apache.felix.sigil.core.internal.model.osgi.PackageImport;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageImport;


public class ConfigTest extends TestCase
{

    static final URI base = URI.create( "test/ConfigTest/sigil.properties" );

    static {
        System.setProperty( "bar.version", "2.0.0" );        
    }
    
    public ConfigTest( String name )
    {
        super( name );
    }


    public void testSimple() throws IOException
    {
        IBldProject project = BldFactory.getProject( base.resolve( "test1.properties" ) );

        ISigilBundle bundle = project.getDefaultBundle();
        IBundleModelElement info = bundle.getBundleInfo();

        PackageImport foo = new PackageImport();
        foo.setPackageName( "foo" );
        foo.setVersions( VersionRange.parseVersionRange( "1.0.0" ) );
        PackageImport bar = new PackageImport();
        bar.setPackageName( "bar" );
        bar.setVersions( VersionRange.parseVersionRange( "[2.2.2, 3.3.3]" ) );
        PackageImport baz = new PackageImport();
        baz.setPackageName( "baz" );
        baz.setVersions( VersionRange.parseVersionRange( "[3.0.0, 4.0.0)" ) );

        Collection<IPackageImport> imports = info.getImports();
        
        assertTrue( foo.toString(), imports.contains( foo ) );
        assertTrue( bar.toString(), imports.contains( bar ) );
        assertTrue( baz.toString(), imports.contains( baz ) );
        //IBundleModelElement requirements = project.getRequirements();
    }
    
    public void testInherited() throws IOException {
        
        IBldProject project = BldFactory.getProject( base.resolve( "inheritance/foo/sigil.properties" ) );

        ISigilBundle bundle = project.getDefaultBundle();
        IBundleModelElement info = bundle.getBundleInfo();

        Collection<IPackageImport> imports = info.getImports();
        assertEquals( 1, imports.size() );
        IPackageImport i = imports.iterator().next();
        assertEquals( "org.bar", i.getPackageName() );
        assertEquals( VersionRange.parseVersionRange("2.0.0"), i.getVersions() );
        
//        project = BldFactory.getProject( base.resolve( "inheritance/foo/sigil.properties" ), true );
//
//        bundle = project.getDefaultBundle();
//        info = bundle.getBundleInfo();
//
//        imports = info.getImports();
//        assertEquals( 1, imports.size() );
//        i = imports.iterator().next();
//        assertEquals( "org.bar", i.getPackageName() );
//        assertEquals( VersionRange.parseVersionRange("2.0.0"), i.getVersions() );
    }

}
