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
import org.osgi.framework.Version;


public class VersionRangeTest extends TestCase {

    public void testVersionRange() throws Exception {
        String version1 = "[1.2.3, 4.5.6]";
        String version2 = "(1, 2]";
        String version3 = "[2,4)";
        String version4 = "(1,2)";
        String version5 = "2";
        String version6 = "2.3";
        String version7 = "[1.2.3.q, 2.3.4.p)";
        String version8 = "1.2.2.5";
        String version9 = "a.b.c";
        String version10 = null;
        String version11 = "";
        String version12 = "\"[1.2.3, 4.5.6]\"";

        VersionRange vr = new VersionRange(version1);
        assertEquals("The value is wrong", "1.2.3", vr.getFloor().toString());
        assertFalse("The value is wrong", vr.isOpenFloor());
        assertEquals("The value is wrong", "4.5.6", vr.getCeiling().toString());
        assertFalse("The value is wrong", vr.isOpenCeiling());

        vr = new VersionRange(version2);
        assertEquals("The value is wrong", "1.0.0", vr.getFloor().toString());
        assertTrue("The value is wrong", vr.isOpenFloor());
        assertEquals("The value is wrong", "2.0.0", vr.getCeiling().toString());
        assertFalse("The value is wrong", vr.isOpenCeiling());

        vr = new VersionRange(version3);

        assertEquals("The value is wrong", "2.0.0", vr.getFloor().toString());
        assertFalse("The value is wrong", vr.isOpenFloor());
        assertEquals("The value is wrong", "4.0.0", vr.getCeiling().toString());
        assertTrue("The value is wrong", vr.isOpenCeiling());

        vr = new VersionRange(version4);

        assertEquals("The value is wrong", "1.0.0", vr.getFloor().toString());
        assertTrue("The value is wrong", vr.isOpenFloor());
        assertEquals("The value is wrong", "2.0.0", vr.getCeiling().toString());
        assertTrue("The value is wrong", vr.isOpenCeiling());

        vr = new VersionRange(version5);
        assertEquals("The value is wrong", "2.0.0", vr.getFloor().toString());
        assertFalse("The value is wrong", vr.isOpenFloor());
        assertEquals("The value is wrong", VersionRange.INFINITE_VERSION, vr.getCeiling());
        assertTrue("The value is wrong", vr.isOpenCeiling());

        vr = new VersionRange(version6, true);
        assertEquals("The value is wrong", "2.3.0", vr.getFloor().toString());
        assertFalse("The value is wrong", vr.isOpenFloor());
        assertEquals("The value is wrong", "2.3.0", vr.getCeiling().toString());
        assertFalse("The value is wrong", vr.isOpenCeiling());

        vr = new VersionRange(version7);
        assertEquals("The value is wrong", "1.2.3.q", vr.getFloor().toString());
        assertFalse("The value is wrong", vr.isOpenFloor());
        assertEquals("The value is wrong", "2.3.4.p", vr.getCeiling().toString());
        assertTrue("The value is wrong", vr.isOpenCeiling());

        vr = new VersionRange(version8);
        assertEquals("The value is wrong", "1.2.2.5", vr.getFloor().toString());
        assertFalse("The value is wrong", vr.isOpenFloor());
        assertEquals("The value is wrong", VersionRange.INFINITE_VERSION, vr.getCeiling());
        assertTrue("The value is wrong", vr.isOpenCeiling());
        boolean exception = false;
        try {
            vr = new VersionRange(version9, false, false);
        } catch (Exception e) {
            exception = true;
        }
        assertTrue("The value is wrong", exception);
        boolean exceptionNull = false;
        try {
            vr = new VersionRange(version10, false, false);
        } catch (Exception e) {
            exceptionNull = true;
        }
        assertTrue("The value is wrong", exceptionNull);
        // empty version should be defaulted to >=0.0.0
        vr = VersionRange.parseVersionRange(version11);
        assertEquals("The value is wrong", "0.0.0", vr.getFloor().toString());
        assertFalse("The value is wrong", vr.isOpenFloor());
        assertEquals("The value is wrong", VersionRange.INFINITE_VERSION, vr.getCeiling());
        assertTrue("The value is wrong", vr.isOpenCeiling());

        vr = new VersionRange(version12);
        assertEquals("The value is wrong", "1.2.3", vr.getFloor().toString());
        assertFalse("The value is wrong", vr.isOpenFloor());
        assertEquals("The value is wrong", "4.5.6", vr.getCeiling().toString());
        assertFalse("The value is wrong", vr.isOpenCeiling());
    }

    public void testInvalidVersions() throws Exception {
        try {
            new VersionRange("a", false, false);
            assertTrue("Should have thrown an exception", false);
        } catch (IllegalArgumentException e) {
        }

    }

    public void testMatches() {
        VersionRange vr = new VersionRange("[1.0.0, 2.0.0]");

        assertFalse(vr.contains(new Version(0, 9, 0)));
        assertFalse(vr.contains(new Version(2, 1, 0)));
        assertTrue(vr.contains(new Version(2, 0, 0)));
        assertTrue(vr.contains(new Version(1, 0, 0)));
        assertTrue(vr.contains(new Version(1, 5, 0)));

        vr = new VersionRange("[1.0.0, 2.0.0)");

        assertFalse(vr.contains(new Version(0, 9, 0)));
        assertFalse(vr.contains(new Version(2, 1, 0)));
        assertFalse(vr.contains(new Version(2, 0, 0)));
        assertTrue(vr.contains(new Version(1, 0, 0)));
        assertTrue(vr.contains(new Version(1, 5, 0)));

        vr = new VersionRange("(1.0.0, 2.0.0)");

        assertFalse(vr.contains(new Version(0, 9, 0)));
        assertFalse(vr.contains(new Version(2, 1, 0)));
        assertFalse(vr.contains(new Version(2, 0, 0)));
        assertFalse(vr.contains(new Version(1, 0, 0)));
        assertTrue(vr.contains(new Version(1, 5, 0)));

        vr = new VersionRange("[1.0.0, 1.0.0]");
        assertFalse(vr.contains(new Version(0, 9, 0)));
        assertFalse(vr.contains(new Version(2, 0, 0)));
        assertTrue(vr.contains(new Version(1, 0, 0)));
        assertFalse(vr.contains(new Version(1, 5, 0)));
        assertFalse(vr.contains(new Version(1, 9, 9)));
    }

    public void testIntersectVersionRange_Valid1() {
        VersionRange v1 = new VersionRange("[1.0.0,3.0.0]");
        VersionRange v2 = new VersionRange("[2.0.0,3.0.0)");
        VersionRange result = v1.intersect(v2);
        assertNotNull(result);
        assertEquals("[2.0.0,3.0.0)", result.toString());
    }

    public void testIntersectVersionRange_Valid2() {
        VersionRange v1 = new VersionRange("[1.0.0,3.0.0)");
        VersionRange v2 = new VersionRange("(2.0.0,3.0.0]");
        VersionRange result = v1.intersect(v2);
        assertNotNull(result);
        assertEquals("(2.0.0,3.0.0)", result.toString());
    }

    public void testIntersectVersionRange_Valid3() {
        VersionRange v1 = new VersionRange("[2.0.0,2.0.0]");
        VersionRange v2 = new VersionRange("[1.0.0,3.0.0]");
        VersionRange result = v1.intersect(v2);
        assertNotNull(result);
        assertEquals("[2.0.0,2.0.0]", result.toString());
    }

    public void testIntersectVersionRange_Invalid1() {
        VersionRange v1 = new VersionRange("[1.0.0,2.0.0]");
        VersionRange v2 = new VersionRange("(2.0.0,3.0.0]");
        VersionRange result = v1.intersect(v2);
        assertNull(result);
    }

    public void testIntersectVersionRange_Invalid2() {
        VersionRange v1 = new VersionRange("[1.0.0,2.0.0)");
        VersionRange v2 = new VersionRange("[2.0.0,3.0.0]");
        VersionRange result = v1.intersect(v2);
        assertNull(result);
    }

    public void testIntersectVersionRange_Invalid3() {
        VersionRange v1 = new VersionRange("[1.0.0,1.0.0]");
        VersionRange v2 = new VersionRange("[2.0.0,2.0.0]");
        VersionRange result = v1.intersect(v2);
        assertNull(result);
    }

}
