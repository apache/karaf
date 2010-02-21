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
package org.apache.felix.webconsole.internal.misc;


import junit.framework.TestCase;


public class LicenseServletTest extends TestCase
{
    // required prefix of a valid path to parse with PathInfo.parse
    private static final String PREFIX = "/" + LicenseServlet.LABEL + "/";


    public void test_PathInfo_parse_null()
    {
        // if the path is empty, null or not starting with /LABEL/
        assertNull( LicenseServlet.PathInfo.parse( null ) );
        assertNull( LicenseServlet.PathInfo.parse( "" ) );
        assertNull( LicenseServlet.PathInfo.parse( LicenseServlet.LABEL ) );
        assertNull( LicenseServlet.PathInfo.parse( LicenseServlet.LABEL + "x" ) );
        assertNull( LicenseServlet.PathInfo.parse( LicenseServlet.LABEL + "/" ) );
        assertNull( LicenseServlet.PathInfo.parse( "/any_not_label/" ) );

        // if the path is only the label (with or with trailing slash)
        assertNull( LicenseServlet.PathInfo.parse( "/" + LicenseServlet.LABEL ) );
        assertNull( LicenseServlet.PathInfo.parse( PREFIX ) );

        // if path has second part not followed by a slash
        assertNull( LicenseServlet.PathInfo.parse( PREFIX + "xyz" ) );
        assertNull( LicenseServlet.PathInfo.parse( PREFIX + "-5" ) );
        assertNull( LicenseServlet.PathInfo.parse( PREFIX + "5.5" ) );

        // if path has second part not converting to a positive long
        assertNull( LicenseServlet.PathInfo.parse( PREFIX + "xyz/trailing" ) );
        assertNull( LicenseServlet.PathInfo.parse( PREFIX + "-5/trailing" ) );
        assertNull( LicenseServlet.PathInfo.parse( PREFIX + "5.5/trailing" ) );
    }


    public void test_PathInfo_parse_direct()
    {
        final long bundleId = 5;
        final String licenseFile = "/META-INF/LICENSE";
        LicenseServlet.PathInfo pi = LicenseServlet.PathInfo.parse( PREFIX + bundleId + licenseFile );
        assertNotNull( pi );
        assertEquals( bundleId, pi.bundleId );
        assertNull( pi.innerJar );
        assertEquals( licenseFile, pi.licenseFile );
    }


    public void test_PathInfo_parse_embedded()
    {
        final long bundleId = 5;
        final String innerJar = "/some.jar";
        final String licenseFile = "META-INF/LICENSE";
        LicenseServlet.PathInfo pi = LicenseServlet.PathInfo.parse( PREFIX + bundleId + innerJar + "!/" + licenseFile );
        assertNotNull( pi );
        assertEquals( bundleId, pi.bundleId );
        assertEquals( innerJar, pi.innerJar );
        assertEquals( licenseFile, pi.licenseFile );
    }
}
