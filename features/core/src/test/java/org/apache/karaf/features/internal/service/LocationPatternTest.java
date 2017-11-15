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
package org.apache.karaf.features.internal.service;

import java.net.MalformedURLException;

import org.apache.karaf.features.LocationPattern;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocationPatternTest {

    @Test
    public void matchingNonMavenUris() throws MalformedURLException {
        assertTrue(new LocationPattern("file:1").matches("file:1"));
        assertFalse(new LocationPattern("file:1").matches("file:2"));
        assertFalse(new LocationPattern("file:*").matches(null));
        assertTrue(new LocationPattern("http://*").matches("http://a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q.txt"));
        assertTrue(new LocationPattern("file:/tmp/x*.txt").matches("file:/tmp/x1.txt"));
        assertTrue(new LocationPattern("file:/tmp/x$2*.txt").matches("file:/tmp/x$24.txt"));
        assertTrue(new LocationPattern("file:/tmp/x^2*.txt").matches("file:/tmp/x^24.txt"));
    }

    @Test
    public void correctMvnLocationPatterns() {
        boolean exception = false;
        for (String p : new String[] {
                "mvn:groupId/artifactId",
                "mvn:groupId/artifactId/1",
                "mvn:groupId/artifactId/1/t",
                "mvn:groupId/artifactId/1/t/c",
                "mvn:groupId/*",
                "mvn:*/*",
                "mvn:*/*/[0,*)/*/*",
                "mvn:g/a/[1,2)",
                "mvn:g/a/[1,*)",
                "mvn:groupId/artifactId/[1.0.0,1.0.0.0)",
                "mvn:groupId/artifactId/[1.0,1.0.0.0)",
                "mvn:groupId/artifactId/[1,1.0.0.0)",
        }) {
            try {
                new LocationPattern(p);
            } catch (MalformedURLException ignored) {
                exception |= true;
            }
        }
        assertFalse("We should not fail for correct mvn: URIs", exception);
    }

    @Test
    public void incorrectMvnLocationPatterns() {
        boolean exception = true;
        for (String p : new String[] {
                "mvn:onlyGroupId",
//                "mvn:groupId/artifactId/wrongVersion",
                "mvn:groupId/artifactId/[1.2,2",
                "mvn:groupId/artifactId/[1.2,",
                "mvn:groupId/artifactId/[1.2",
                "mvn:groupId/artifactId/[",
//                "mvn:groupId/artifactId/*",
                "mvn:groupId/artifactId/[wrongRange,wrongRange]",
                "mvn:groupId/artifactId/(wrongRange,wrongRange]",
                "mvn:groupId/artifactId/(wrongRange,3]",
                "mvn:groupId/artifactId/[1,wrongRange)",
                "mvn:groupId/artifactId/[1,1.2.3.4.5)",
                "mvn:groupId/artifactId/[1,1.2.a)",
                "mvn:groupId/artifactId/[1,1.a)",
                "mvn:groupId/artifactId/[1,1)",
                "mvn:groupId/artifactId/[1.0,1)",
                "mvn:groupId/artifactId/[1.0.0,1)",
                "mvn:groupId/artifactId/[1.0.0.0,1)",
                "mvn:groupId/artifactId/[1.0.0.0,1.0)",
                "mvn:groupId/artifactId/[1.0.0.0,1.0.0)",
                "mvn:groupId/artifactId/[1.0.0.0,1.0.0.0)"
        }) {
            try {
                new LocationPattern(p);
                exception &= false;
            } catch (MalformedURLException ignored) {
            }
        }
        assertTrue("We should fail for all broken mvn: URIs", exception);
    }

    @Test
    public void matchingMavenUrisWithoutPatterns() throws MalformedURLException {
        assertTrue(new LocationPattern("mvn:g/a/1/t/c").matches("mvn:g/a/1/t/c"));
        assertTrue(new LocationPattern("mvn:g/a/1//c").matches("mvn:g/a/1/jar/c"));
        assertTrue(new LocationPattern("mvn:g/a/1").matches("mvn:g/a/1"));
        assertTrue(new LocationPattern("mvn:g/a").matches("mvn:g/a/1"));
        assertTrue(new LocationPattern("mvn:g/a").matches("mvn:g/a/1/jar"));
        assertTrue("Special case - when there's no version, we don't match to \"jar\" type, but to all types",
                new LocationPattern("mvn:g/a").matches("mvn:g/a/1/j"));
        assertTrue(new LocationPattern("mvn:g/a").matches("mvn:g/a/1/t/c"));
        assertTrue(new LocationPattern("mvn:g/a/1").matches("mvn:g/a/1/jar"));
        assertFalse(new LocationPattern("mvn:g/a/1").matches("mvn:g/a/1/war"));
        assertTrue(new LocationPattern("mvn:g/a/1").matches("mvn:g/a/1/jar/c"));
        assertTrue(new LocationPattern("mvn:g/a/1").matches("mvn:g/a/1//c"));
    }

    @Test
    public void matchingMavenUrisWithVersionRangesInPattern() throws MalformedURLException {
        assertTrue(new LocationPattern("mvn:g/a/[1,1]").matches("mvn:g/a"));
        assertTrue(new LocationPattern("mvn:g/a/[1,1]").matches("mvn:g/a/1"));
        assertFalse(new LocationPattern("mvn:g/a/[1,1.1)").matches("mvn:g/a/1.1"));
        assertTrue(new LocationPattern("mvn:g/a/[1,2)").matches("mvn:g/a/1"));
        assertTrue(new LocationPattern("mvn:g/a/[1,2)").matches("mvn:g/a/1.1"));
        assertTrue(new LocationPattern("mvn:g/a/[1,2)").matches("mvn:g/a/1.9"));
        assertTrue(new LocationPattern("mvn:g/a/[1,2)").matches("mvn:g/a/1.9.9.BUILD-SNAPSHOT"));
        assertFalse(new LocationPattern("mvn:g/a/[1,2)").matches("mvn:g/a/2.0"));
        assertTrue(new LocationPattern("mvn:g/a/[1,*)").matches("mvn:g/a/2.0"));
        assertTrue(new LocationPattern("mvn:g/a/[1,*)").matches("mvn:g/a/42.0"));
        assertTrue(new LocationPattern("mvn:g/a/[1,*)").matches("mvn:g/a/9999.9999.9999.9999"));
    }

    @Test
    public void matchingMavenUrisWithPatterns() throws MalformedURLException {
        assertTrue(new LocationPattern("mvn:*/a/1").matches("mvn:g/a/1"));
        assertTrue(new LocationPattern("mvn:g/*/1").matches("mvn:g/a/1"));
        assertTrue(new LocationPattern("mvn:*/*/1").matches("mvn:g/a/1"));
        assertFalse(new LocationPattern("mvn:*/*/[1,*)/jar").matches("mvn:g/a/1/war/c"));
        assertFalse(new LocationPattern("mvn:*/*/[1,*)/jar").matches("mvn:g/a/1/war"));
        assertTrue(new LocationPattern("mvn:*/*/[1,*)/jar").matches("mvn:g/a/1"));
        assertTrue(new LocationPattern("mvn:*/*/[1,*)/jar").matches("mvn:g/a/1//c"));
        assertFalse(new LocationPattern("mvn:*/*/[1,*)/jar/c*").matches("mvn:g/a/1//d"));
        assertTrue(new LocationPattern("mvn:*/*/[1,*)/jar/d*").matches("mvn:g/a/1//d"));
        assertTrue(new LocationPattern("mvn:*/*").matches("mvn:g/a/1//c"));
        assertTrue(new LocationPattern("mvn:*/*").matches("mvn:g/a/1/t/c"));
        assertTrue(new LocationPattern("mvn:*/*").matches("mvn:g/a/1/t"));
        assertTrue(new LocationPattern("mvn:*/*").matches("mvn:g/a/1"));
        assertTrue(new LocationPattern("mvn:*/*").matches("mvn:g/a"));
        assertFalse(new LocationPattern("mvn:*/*/2/*/*").matches("mvn:g/a/1"));
        assertTrue(new LocationPattern("mvn:*/*/[1,2)/*/*").matches("mvn:g/a/1"));
        assertTrue(new LocationPattern("mvn:*/*/1/*/*").matches("mvn:g/a/1"));
        assertTrue(new LocationPattern("mvn:*/*/1/*/*").matches("mvn:g/a/1//c"));
        assertTrue(new LocationPattern("mvn:*/*/1/*/*").matches("mvn:g/a/1/jar/c"));
        assertTrue(new LocationPattern("mvn:*/*/1/*/*").matches("mvn:g/a/1/t/c"));
    }

    @Test
    public void matchingMavenUrisWithVersionRangesInUri() throws MalformedURLException {
        assertFalse(new LocationPattern("mvn:g/a/[1,1]").matches("mvn:g/a/[1,1]"));
    }

}
