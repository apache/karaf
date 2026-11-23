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
package org.apache.karaf.services.url.internal;

import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

import org.junit.Test;

import static org.junit.Assert.*;

public class SnapshotMetadataTest {

    private static final String MAVEN3 = "<metadata><versioning>"
            + "<snapshot><timestamp>20240101.120000</timestamp><buildNumber>3</buildNumber></snapshot>"
            + "<lastUpdated>20240101120500</lastUpdated><snapshotVersions>"
            + "<snapshotVersion><classifier>sources</classifier><extension>jar</extension>"
            + "<value>1.0-20240101.110000-2</value><updated>20240101110000</updated></snapshotVersion>"
            + "<snapshotVersion><extension>jar</extension>"
            + "<value>1.0-20240101.120000-3</value><updated>20240101120000</updated></snapshotVersion>"
            + "<snapshotVersion><classifier>features</classifier><extension>xml</extension>"
            + "<value>1.0-20240101.120000-3</value><updated>20240101120000</updated></snapshotVersion>"
            + "</snapshotVersions></versioning></metadata>";

    @Test
    public void testMaven3Metadata() throws Exception {
        SnapshotMetadata jar = parse(MAVEN3, "jar", null);
        assertEquals("1.0-20240101.120000-3", jar.version);
        assertEquals(utc(2024, 0, 1, 12, 0, 0), jar.updated);

        assertEquals("1.0-20240101.110000-2", parse(MAVEN3, "jar", "sources").version);
        assertEquals("1.0-20240101.120000-3", parse(MAVEN3, "xml", "features").version);
    }

    @Test
    public void testMaven2Metadata() throws Exception {
        SnapshotMetadata snapshot = parse("<metadata><versioning><snapshot><timestamp>20240101.120000</timestamp>"
                + "<buildNumber>3</buildNumber></snapshot><lastUpdated>20240101120500</lastUpdated></versioning>"
                + "</metadata>", "jar", "javadoc");
        assertEquals("1.0-20240101.120000-3", snapshot.version);
        assertEquals(utc(2024, 0, 1, 12, 5, 0), snapshot.updated);
    }

    @Test
    public void testNonUniqueSnapshot() throws Exception {
        SnapshotMetadata snapshot = parse("<metadata><versioning><snapshot><localCopy>true</localCopy></snapshot>"
                + "</versioning></metadata>", "jar", null);
        assertEquals("1.0-SNAPSHOT", snapshot.version);
        assertEquals(0, snapshot.updated);
    }

    @Test
    public void testNoSnapshot() throws Exception {
        assertNull(parse("<metadata><versioning><lastUpdated>20240101120500</lastUpdated></versioning></metadata>",
                "jar", null));
        assertNull(parse("<metadata/>", "jar", null));
    }

    @Test(expected = java.io.IOException.class)
    public void testDoctypeIsRejected() throws Exception {
        parse("<!DOCTYPE metadata [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><metadata>&x;</metadata>", "jar", null);
    }

    private static SnapshotMetadata parse(String metadata, String extension, String classifier) throws Exception {
        return SnapshotMetadata.parse(metadata.getBytes(StandardCharsets.UTF_8), "1.0-SNAPSHOT", extension, classifier);
    }

    private static long utc(int year, int month, int day, int hour, int minute, int second) {
        java.util.Calendar calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

}
