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

import junit.framework.TestCase;

public class VersionCleanerTest extends TestCase {

    public void testConvertVersionToOsgi()
    {
        String osgiVersion;

        osgiVersion = VersionCleaner.clean(null);
        assertEquals("0.0.0", osgiVersion);

        osgiVersion = VersionCleaner.clean("");
        assertEquals("0.0.0", osgiVersion);

        osgiVersion = VersionCleaner.clean("2.1.0-SNAPSHOT");
        assertEquals("2.1.0.SNAPSHOT", osgiVersion);

        osgiVersion = VersionCleaner.clean("2.1-SNAPSHOT");
        assertEquals("2.1.0.SNAPSHOT", osgiVersion);

        osgiVersion = VersionCleaner.clean("2-SNAPSHOT");
        assertEquals("2.0.0.SNAPSHOT", osgiVersion);

        osgiVersion = VersionCleaner.clean("2");
        assertEquals("2.0.0", osgiVersion);

        osgiVersion = VersionCleaner.clean("2.1");
        assertEquals("2.1.0", osgiVersion);

        osgiVersion = VersionCleaner.clean("2.1.3");
        assertEquals("2.1.3", osgiVersion);

        osgiVersion = VersionCleaner.clean("2.1.3.4");
        assertEquals("2.1.3.4", osgiVersion);

        osgiVersion = VersionCleaner.clean("4aug2000r7-dev");
        assertEquals("0.0.0.4aug2000r7-dev", osgiVersion);

        osgiVersion = VersionCleaner.clean("1.1-alpha-2");
        assertEquals("1.1.0.alpha-2", osgiVersion);

        osgiVersion = VersionCleaner.clean("1.0-alpha-16-20070122.203121-13");
        assertEquals("1.0.0.alpha-16-20070122_203121-13", osgiVersion);

        osgiVersion = VersionCleaner.clean("1.0-20070119.021432-1");
        assertEquals("1.0.0.20070119_021432-1", osgiVersion);

        osgiVersion = VersionCleaner.clean("1-20070119.021432-1");
        assertEquals("1.0.0.20070119_021432-1", osgiVersion);

        osgiVersion = VersionCleaner.clean("1.4.1-20070217.082013-7");
        assertEquals("1.4.1.20070217_082013-7", osgiVersion);
    }

}
