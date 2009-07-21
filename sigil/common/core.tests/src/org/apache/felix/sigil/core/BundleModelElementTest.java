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


import java.util.Arrays;

import org.apache.felix.sigil.core.internal.model.osgi.BundleModelElement;
import org.apache.felix.sigil.core.internal.model.osgi.PackageImport;
import org.apache.felix.sigil.core.internal.model.osgi.RequiredBundle;
import org.apache.felix.sigil.model.common.VersionRange;

import junit.framework.TestCase;


public class BundleModelElementTest extends TestCase
{

    public BundleModelElementTest( String name )
    {
        super( name );
    }


    public void testAddRequires()
    {
        BundleModelElement element = new BundleModelElement();
        checkRequires( element );
    }


    public void testAddImports()
    {
        BundleModelElement element = new BundleModelElement();
        checkImports( element );
    }


    public void testAddImportsAndRequires()
    {
        BundleModelElement element = new BundleModelElement();
        checkImports( element );
        checkRequires( element );

        element = new BundleModelElement();
        checkRequires( element );
        checkImports( element );
    }


    private void checkImports( BundleModelElement element )
    {
        PackageImport foo = new PackageImport();
        foo.setPackageName( "foo" );
        foo.setVersions( VersionRange.parseVersionRange( "1.0.0" ) );
        PackageImport bar = new PackageImport();
        bar.setPackageName( "bar" );
        bar.setVersions( VersionRange.parseVersionRange( "[2.2.2, 3.3.3]" ) );
        PackageImport baz = new PackageImport();
        baz.setPackageName( "baz" );
        baz.setVersions( VersionRange.parseVersionRange( "[3.0.0, 4.0.0)" ) );

        element.addChild( foo.clone() );
        element.addChild( bar.clone() );
        element.addChild( baz.clone() );

        assertTrue( Arrays.asList( element.children() ).contains( foo ) );
        assertTrue( Arrays.asList( element.children() ).contains( bar ) );
        assertTrue( Arrays.asList( element.children() ).contains( baz ) );
    }


    private void checkRequires( BundleModelElement element )
    {
        RequiredBundle foo = new RequiredBundle();
        foo.setSymbolicName( "foo" );
        foo.setVersions( VersionRange.parseVersionRange( "1.0.0" ) );
        RequiredBundle bar = new RequiredBundle();
        bar.setSymbolicName( "bar" );
        bar.setVersions( VersionRange.parseVersionRange( "[2.2.2, 3.3.3]" ) );
        RequiredBundle baz = new RequiredBundle();
        baz.setSymbolicName( "baz" );
        baz.setVersions( VersionRange.parseVersionRange( "[3.0.0, 4.0.0)" ) );

        element.addChild( foo.clone() );
        element.addChild( bar.clone() );
        element.addChild( baz.clone() );

        assertTrue( Arrays.asList( element.children() ).contains( foo ) );
        assertTrue( Arrays.asList( element.children() ).contains( bar ) );
        assertTrue( Arrays.asList( element.children() ).contains( baz ) );
    }
}
