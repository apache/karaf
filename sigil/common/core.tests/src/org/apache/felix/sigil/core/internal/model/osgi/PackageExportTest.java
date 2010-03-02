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
package org.apache.felix.sigil.core.internal.model.osgi;

import org.osgi.framework.Version;

import junit.framework.TestCase;

public class PackageExportTest extends TestCase
{
    public PackageExportTest( String name )
    {
        super( name );
    }

    public void testEquals() {
        PackageExport p1 = new PackageExport();
        p1.setPackageName("foo");
        p1.setVersion(Version.parseVersion("1.0.0"));
        
        PackageExport p2 = new PackageExport();
        p2.setPackageName("foo");
        p2.setVersion(Version.parseVersion("1.0.0"));
        
        assertTrue( p1.equals( p2 ) );
        assertTrue( p2.equals( p1 ) );
        
        PackageExport p3 = new PackageExport();
        p3.setPackageName("foo");

        assertFalse( p1.equals( p3 ) );
        assertFalse( p3.equals( p1 ) );
        
        PackageExport p4 = new PackageExport();
        p4.setVersion(Version.parseVersion("1.0.0"));

        assertFalse( p1.equals( p4 ) );
        assertFalse( p4.equals( p1 ) );
        
        PackageExport p5 = new PackageExport();
        assertFalse( p1.equals( p5 ) );
        assertFalse( p5.equals( p1 ) );
        
    }
}
