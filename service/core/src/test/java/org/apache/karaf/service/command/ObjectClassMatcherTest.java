/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.service.command;

import org.junit.Assert;
import org.junit.Test;

public class ObjectClassMatcherTest {

    @Test
    public void testGetShortName() {
        Assert.assertEquals("TestClass", ObjectClassMatcher.getShortName("org.apache.TestClass"));
        Assert.assertEquals("", ObjectClassMatcher.getShortName("test."));
        Assert.assertEquals("TestClass", ObjectClassMatcher.getShortName("TestClass"));
    }

    @Test
    public void testMatchesName() {
        Assert.assertTrue(ObjectClassMatcher.matchesName("org.apache.TestClass", "TestClass"));
        Assert.assertTrue(ObjectClassMatcher.matchesName("TestClass", "TestClass"));
    }

    @Test
    public void testMatchesAtLeastOneName() {
        Assert.assertTrue(ObjectClassMatcher.matchesAtLeastOneName(new String[]{"other", "org.apache.TestClass"}, "TestClass"));
        Assert.assertFalse(ObjectClassMatcher.matchesAtLeastOneName(new String[]{"TestClass2"}, "TestClass"));
    }

    
}
