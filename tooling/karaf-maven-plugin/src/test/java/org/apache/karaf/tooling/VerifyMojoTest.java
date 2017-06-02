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
package org.apache.karaf.tooling;

import org.apache.karaf.tooling.VerifyMojo;
import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VerifyMojoTest {

    @Test
    public void testFeaturePattern() {
        Pattern pattern = VerifyMojo.getPattern(Arrays.asList("foobiz", "!foo*", "bar", "!ba*", "*"));
        assertTrue(pattern.matcher("foobiz/1.0").matches());
        assertFalse(pattern.matcher("foobaz/1.0").matches());
        assertTrue(pattern.matcher("bar/1.0").matches());
        assertFalse(pattern.matcher("baz/1.0").matches());
        assertTrue(pattern.matcher("biz/1.0").matches());

        pattern = VerifyMojo.getPattern(Arrays.asList("!hibernate", " *"));
        assertTrue(pattern.matcher("framework/4.2.0.SNAPSHOT").matches());
    }

}
