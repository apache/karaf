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
package org.apache.felix.framework.util;

import junit.framework.TestCase;
import java.util.List;

public class UtilTest extends TestCase
{
    public void testSubstringMatching()
    {
        List<String> pieces;

        pieces = Util.parseSubstring("foo");
        assertTrue("Should match!", Util.checkSubstring(pieces, "foo"));
        assertFalse("Should not match!", Util.checkSubstring(pieces, "barfoo"));
        assertFalse("Should not match!", Util.checkSubstring(pieces, "foobar"));

        pieces = Util.parseSubstring("foo*");
        assertTrue("Should match!", Util.checkSubstring(pieces, "foo"));
        assertFalse("Should not match!", Util.checkSubstring(pieces, "barfoo"));
        assertTrue("Should match!", Util.checkSubstring(pieces, "foobar"));

        pieces = Util.parseSubstring("*foo");
        assertTrue("Should match!", Util.checkSubstring(pieces, "foo"));
        assertTrue("Should match!", Util.checkSubstring(pieces, "barfoo"));
        assertFalse("Should match!", Util.checkSubstring(pieces, "foobar"));

        pieces = Util.parseSubstring("foo*bar");
        assertTrue("Should match!", Util.checkSubstring(pieces, "foobar"));
        assertFalse("Should not match!", Util.checkSubstring(pieces, "barfoo"));
        assertTrue("Should match!", Util.checkSubstring(pieces, "foosldfjbar"));

        pieces = Util.parseSubstring("*foo*bar");
        assertTrue("Should match!", Util.checkSubstring(pieces, "foobar"));
        assertFalse("Should not match!", Util.checkSubstring(pieces, "foobarfoo"));
        assertTrue("Should match!", Util.checkSubstring(pieces, "barfoobar"));
        assertTrue("Should match!", Util.checkSubstring(pieces, "sdffoobsdfbar"));

        pieces = Util.parseSubstring("*foo*bar*");
        assertTrue("Should match!", Util.checkSubstring(pieces, "foobar"));
        assertTrue("Should match!", Util.checkSubstring(pieces, "foobarfoo"));
        assertTrue("Should match!", Util.checkSubstring(pieces, "barfoobar"));
        assertTrue("Should match!", Util.checkSubstring(pieces, "sdffoobsdfbar"));
        assertTrue("Should match!", Util.checkSubstring(pieces, "sdffoobsdfbarlj"));
        assertFalse("Should not match!", Util.checkSubstring(pieces, "sdffobsdfbarlj"));
    }
}
