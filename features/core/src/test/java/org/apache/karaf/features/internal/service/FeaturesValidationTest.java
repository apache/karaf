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
package org.apache.karaf.features.internal.service;

import java.net.URL;

import org.apache.karaf.features.Library;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FeaturesValidationTest {

    @Test
    public void testNs10() throws Exception {
        FeatureValidationUtil.validate(getClass().getResource("f02.xml").toURI());
    }

    @Test
    public void testNs10Unmarshall() throws Exception {
        URL url = getClass().getResource("f02.xml");
        Features features = JaxbUtil.unmarshal(url.toExternalForm(), true);
        assertNotNull(features);
    }

    @Test
    public void testNs10NoName() throws Exception {
        FeatureValidationUtil.validate(getClass().getResource("f03.xml").toURI());
    }

    @Test
    public void testNs10NoNameUnmarshall() throws Exception {
        URL url = getClass().getResource("f03.xml");
        Features features = JaxbUtil.unmarshal(url.toExternalForm(), true);
        assertNotNull(features);
    }

    @Test
    public void testNs11() throws Exception {
        FeatureValidationUtil.validate(getClass().getResource("f04.xml").toURI());
    }

    @Test
    public void testNs11Unmarshall() throws Exception {
        URL url = getClass().getResource("f04.xml");
        Features features = JaxbUtil.unmarshal(url.toExternalForm(), true);
        assertNotNull(features);
    }

    @Test
    public void testNs11NoName() throws Exception {
        try {
            FeatureValidationUtil.validate(getClass().getResource("f05.xml").toURI());
            fail("Validation should have failed");
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testNs12() throws Exception {
        FeatureValidationUtil.validate(getClass().getResource("f06.xml").toURI());
    }

    @Test
    public void testNs12Unmarshall() throws Exception {
        URL url = getClass().getResource("f06.xml");
        Features features = JaxbUtil.unmarshal(url.toExternalForm(), true);
        assertNotNull(features);
    }

    @Test
    public void testNs13() throws Exception {
        FeatureValidationUtil.validate(getClass().getResource("f07.xml").toURI());
    }

    @Test
    public void testNs13Unmarshall() throws Exception {
        URL url = getClass().getResource("f07.xml");
        Features features = JaxbUtil.unmarshal(url.toExternalForm(), true);
        assertNotNull(features);
        assertEquals("2.5.6.SEC02", features.getFeature().get(0).getVersion());
        assertTrue(features.getFeature().get(1).isHidden());
        assertNotNull(features.getFeature().get(1).getLibraries());
        assertEquals(1, features.getFeature().get(0).getLibraries().size());
        assertEquals("my-library", features.getFeature().get(0).getLibraries().get(0).getLocation());
        assertEquals(Library.TYPE_ENDORSED, features.getFeature().get(0).getLibraries().get(0).getType());
        assertFalse(features.getFeature().get(0).getLibraries().get(0).isExport());
        assertTrue(features.getFeature().get(0).getLibraries().get(0).isDelegate());
    }

}
