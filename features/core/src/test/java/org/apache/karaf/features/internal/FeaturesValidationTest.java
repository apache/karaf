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
package org.apache.karaf.features.internal;

import org.junit.Test;

import static org.junit.Assert.fail;

public class FeaturesValidationTest {

    @Test
    public void testNoNs() throws Exception {
        FeatureValidationUtil.validate(getClass().getResource("f01.xml").toURI());
    }

    @Test
    public void testNs10() throws Exception {
        FeatureValidationUtil.validate(getClass().getResource("f02.xml").toURI());
    }

    @Test
    public void testNs10NoName() throws Exception {
        FeatureValidationUtil.validate(getClass().getResource("f03.xml").toURI());
    }

    @Test
    public void testNs11() throws Exception {
        FeatureValidationUtil.validate(getClass().getResource("f04.xml").toURI());
    }

    @Test
    public void testNs12() throws Exception {
        FeatureValidationUtil.validate(getClass().getResource("f06.xml").toURI());
    }

    @Test
    public void testNs13() throws Exception {
        try {
            FeatureValidationUtil.validate(getClass().getResource("f05.xml").toURI());
            fail("Validation should have failed");
        } catch (Exception e) {
            // ok
        }
    }

}
