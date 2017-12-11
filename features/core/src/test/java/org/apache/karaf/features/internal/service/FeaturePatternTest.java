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

import org.apache.karaf.features.FeaturePattern;
import org.junit.Test;
import org.osgi.framework.Version;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FeaturePatternTest {

    @Test
    public void matchingFeatureIds() {
        assertTrue(new FeaturePattern("spring").matches("spring", null));
        assertTrue(new FeaturePattern("spring").matches("spring", "0.0.0"));
        assertTrue(new FeaturePattern("spring").matches("spring", "1.0.0"));
        assertFalse(new FeaturePattern("spring").matches("springish", "1.0.0"));
        assertFalse(new FeaturePattern("commons/1").matches("commons", "0.0.0"));
        assertFalse(new FeaturePattern("commons/1").matches("commons", null));
        assertTrue(new FeaturePattern("commons/1").matches("commons", "1"));
        assertFalse(new FeaturePattern("commons/1").matches("commons", "1.0.0.1"));
        assertFalse(new FeaturePattern("space/[3,4]").matches("space", "1"));
        assertTrue(new FeaturePattern("space/[3,4]").matches("space", "3"));
        assertTrue(new FeaturePattern("space/[3,4]").matches("space", "3.1"));
        assertFalse(new FeaturePattern("space/[3,4]").matches("x-space", "3.1"));
        assertTrue(new FeaturePattern("space/[3,4]").matches("space", "4.0.0"));
        assertFalse(new FeaturePattern("space/[3,4]").matches("space", "4.0.0.0")); // last ".0" is qualifier
        assertFalse(new FeaturePattern("space/[3,4]").matches("space", "4.0.1"));
        assertTrue(new FeaturePattern("special;range=1").matches("special", "1"));
        assertTrue(new FeaturePattern("special;range=1").matches("special", "1.0"));
        assertTrue(new FeaturePattern("special;range=1").matches("special", "1.0.0"));
        assertFalse(new FeaturePattern("special;range=1").matches("special2", "1.0.0"));
        assertFalse(new FeaturePattern("special;range=1").matches("special", "1.0.1"));
        assertTrue(new FeaturePattern("universal;range=[3,4)").matches("universal", "3"));
        assertTrue(new FeaturePattern("universal;range=[3,4)").matches("universal", "3.0"));
        assertTrue(new FeaturePattern("universal;range=[3,4)").matches("universal", "3.0.0"));
        assertTrue(new FeaturePattern("universal;range=[3,4)").matches("universal", "3.4.2"));
        assertTrue(new FeaturePattern("universal;range=[3,4)").matches("universal", "3.9.9.GA"));
        assertFalse(new FeaturePattern("universal;range=[3,4)").matches("universalis", "3.9.9.GA"));
        assertFalse(new FeaturePattern("universal;range=[3,4)").matches("universal", "4.0.0"));
        assertTrue(new FeaturePattern("a*").matches("alphabet", null));
        assertTrue(new FeaturePattern("a*").matches("alphabet", "0"));
        assertTrue(new FeaturePattern("a*").matches("alphabet", "999"));
        assertFalse(new FeaturePattern("a*").matches("_alphabet", "999"));
        assertTrue(new FeaturePattern("*b/[3,4)").matches("b", "3.5"));
        assertTrue(new FeaturePattern("*b/[3,4)").matches("cb", "3.5"));
        assertFalse(new FeaturePattern("*b/[3,4)").matches("bc", "3.5"));
        assertFalse(new FeaturePattern("*b/[3,4)").matches("cb", "4.0.0"));
        assertFalse(new FeaturePattern("*b/[3,4)").matches("cb", null));
        assertFalse(new FeaturePattern("*b/[3,4)").matches("cb", "0"));
        assertFalse(new FeaturePattern("*b/[3,4)").matches("cb", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
        assertFalse(new FeaturePattern("*b/[3,4)").matches("cb", Version.emptyVersion.toString()));
    }

}
