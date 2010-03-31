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
package org.apache.felix.framework.capabilityset;

import junit.framework.TestCase;
import java.util.List;

public class SimpleFilterTest extends TestCase
{
    public void testSubstringMatching()
    {
        List<String> pieces;

        pieces = SimpleFilter.parseSubstring("foo");
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "foo"));
        assertFalse("Should not match!", SimpleFilter.compareSubstring(pieces, "barfoo"));
        assertFalse("Should not match!", SimpleFilter.compareSubstring(pieces, "foobar"));

        pieces = SimpleFilter.parseSubstring("foo*");
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "foo"));
        assertFalse("Should not match!", SimpleFilter.compareSubstring(pieces, "barfoo"));
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "foobar"));

        pieces = SimpleFilter.parseSubstring("*foo");
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "foo"));
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "barfoo"));
        assertFalse("Should match!", SimpleFilter.compareSubstring(pieces, "foobar"));

        pieces = SimpleFilter.parseSubstring("foo*bar");
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "foobar"));
        assertFalse("Should not match!", SimpleFilter.compareSubstring(pieces, "barfoo"));
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "foosldfjbar"));

        pieces = SimpleFilter.parseSubstring("*foo*bar");
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "foobar"));
        assertFalse("Should not match!", SimpleFilter.compareSubstring(pieces, "foobarfoo"));
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "barfoobar"));
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "sdffoobsdfbar"));

        pieces = SimpleFilter.parseSubstring("*foo*bar*");
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "foobar"));
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "foobarfoo"));
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "barfoobar"));
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "sdffoobsdfbar"));
        assertTrue("Should match!", SimpleFilter.compareSubstring(pieces, "sdffoobsdfbarlj"));
        assertFalse("Should not match!", SimpleFilter.compareSubstring(pieces, "sdffobsdfbarlj"));
    }
}
