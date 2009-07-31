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
package org.apache.felix.scr.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;

public class BundleComponentActivatorTest extends TestCase
{

    /**
     * Test that an empty array is returned for a null bundle.
     */
    public void test_findDescriptors_withNullBundle()
    {
        final URL[] urls = BundleComponentActivator.findDescriptors( null, "foo.xml" );
        assertNotNull( "Descriptor array is not null", urls );
        assertEquals( "Descriptor array length", 0, urls.length );
    }

    /**
     * Test that an empty array is returned for a null location.
     */
    public void test_findDescriptors_withNullLocation()
    {
        final URL[] urls = BundleComponentActivator.findDescriptors( new MockBundle(), null );
        assertNotNull( "Descriptor array is not null", urls );
        assertEquals( "Descriptor array length", 0, urls.length );
    }

    /**
     * Test that an empty array is returned for an empty location.
     */
    public void test_findDescriptors_withEmptyLocation()
    {
        final URL[] urls = BundleComponentActivator.findDescriptors( new MockBundle(), "" );
        assertNotNull( "Descriptor array is not null", urls );
        assertEquals( "Descriptor array length", 0, urls.length );
    }

    /**
     * Test that an empty array is returned for a location containing only blanks.
     */
    public void test_findDescriptors_withBlankLocation()
    {
        final URL[] urls = BundleComponentActivator.findDescriptors( new MockBundle(), " " );
        assertNotNull( "Descriptor array is not null", urls );
        assertEquals( "Descriptor array length", 0, urls.length );
    }

    /**
     * Test that when using a non wilcarded location, getResource() will be used (for legacy reasons) and the returned
     * array is the one returned by bundle method call.
     *
     * @throws MalformedURLException unexpected
     */
    public void test_findDescriptors_withNonWildcardLocation()
        throws MalformedURLException
    {
        URL descriptor = new URL( "file:foo.xml" );
        final Bundle bundle = (Bundle) EasyMock.createNiceMock( Bundle.class );
        EasyMock.expect( bundle.getResource( "/some/location/foo.xml" ) ).andReturn( descriptor );

        EasyMock.replay( new Object[]{ bundle } );
        final URL[] urls = BundleComponentActivator.findDescriptors( bundle, "/some/location/foo.xml" );
        EasyMock.verify( new Object[]{ bundle } );

        assertNotNull( "Descriptor array is not null", urls );
        assertEquals( "Descriptor length", 1, urls.length );
        assertEquals( "Descriptor", descriptor, urls[ 0 ] );
    }

    public void findDescriptors_withWildcardLocation( final String location,
                                                      final String path,
                                                      final String filePattern )
        throws MalformedURLException
    {
        final URL[] urls = new URL[]
            {
                new URL( "file:foo1.xml" ),
                new URL( "file:foo2.xml" )
            };
        final Enumeration de = new Vector( Arrays.asList( urls ) ).elements();
        final Bundle bundle = (Bundle) EasyMock.createNiceMock( Bundle.class );
        EasyMock.expect( bundle.findEntries( path, filePattern, false ) ).andReturn( de );

        EasyMock.replay( new Object[]{ bundle } );
        final URL[] actualUrls = BundleComponentActivator.findDescriptors( bundle, location );
        EasyMock.verify( new Object[]{ bundle } );

        assertNotNull( "Descriptor array is not null", actualUrls );
        assertEquals( "Descriptor length", urls.length, actualUrls.length );
        for( int i = 0; i < actualUrls.length; i++ )
        {
            assertEquals( "Descriptor", urls[ i ], actualUrls[ i ] );
        }
    }

    /**
     * Test that when using "*.xml", path will be root of bundle "/" and file pattern will be "*.xml".
     *
     * @throws MalformedURLException unexpected
     */
    public void test_findDescriptors_withWildcardLocation01()
        throws MalformedURLException
    {
        findDescriptors_withWildcardLocation( "*.xml", "/", "*.xml" );
    }

    /**
     * Test that when using "/foo/*.xml", path will be "/foo/" and file pattern will be "*.xml".
     *
     * @throws MalformedURLException unexpected
     */
    public void test_findDescriptors_withWildcardLocation02()
        throws MalformedURLException
    {
        findDescriptors_withWildcardLocation( "/foo/*.xml", "/foo", "*.xml" );
    }

    /**
     * Test that in case that no resources are found (bundle return null) an empty array is returned.
     *
     * @throws MalformedURLException unexpected
     */
    public void test_findDescriptors_withWildcardLocation_nullEnum()
        throws MalformedURLException
    {
        final Bundle bundle = (Bundle) EasyMock.createNiceMock( Bundle.class );
        EasyMock.expect( bundle.findEntries( "/", "*.xml", false ) ).andReturn( null );

        EasyMock.replay( new Object[]{ bundle } );
        final URL[] actualUrls = BundleComponentActivator.findDescriptors( bundle, "*.xml" );
        EasyMock.verify( new Object[]{ bundle } );

        assertNotNull( "Descriptor array is not null", actualUrls );
        assertEquals( "Descriptor length", 0, actualUrls.length );
    }

    /**
     * Test that in case that no resources are found (bundle return empty enum) an empty array is returned.
     *
     * @throws MalformedURLException unexpected
     */
    public void test_findDescriptors_withWildcardLocation_emptyEnum()
        throws MalformedURLException
    {
        final Bundle bundle = (Bundle) EasyMock.createNiceMock( Bundle.class );
        EasyMock.expect( bundle.findEntries( "/", "*.xml", false ) ).andReturn( new Vector().elements() );

        EasyMock.replay( new Object[]{ bundle } );
        final URL[] actualUrls = BundleComponentActivator.findDescriptors( bundle, "*.xml" );
        EasyMock.verify( new Object[]{ bundle } );

        assertNotNull( "Descriptor array is not null", actualUrls );
        assertEquals( "Descriptor length", 0, actualUrls.length );
    }

}