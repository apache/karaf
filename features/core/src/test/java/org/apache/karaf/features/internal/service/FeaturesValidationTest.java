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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;

import org.apache.karaf.features.Library;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.junit.Test;

public class FeaturesValidationTest {

    @Test
    public void testNs10() throws Exception {
        Features features = unmarshalAndValidate("f02.xml");
        assertNotNull(features);
    }

    @Test
    public void testNs10NoName() throws Exception {
        Features features = unmarshalAndValidate("f03.xml");
        assertNotNull(features);
    }

    @Test
    public void testNs11() throws Exception {
        Features features = unmarshalAndValidate("f04.xml");;
        assertNotNull(features);
    }

    @Test
    public void testNs11NoName() throws Exception {
        try {
            unmarshalAndValidate("f05.xml");
            fail("Validation should have failed");
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testNs12Unmarshall() throws Exception {
        Features features = unmarshalAndValidate("f06.xml");
        assertNotNull(features);
    }

    @Test
    public void testNs13() throws Exception {
        Features features = unmarshalAndValidate("f07.xml");
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

    private Features unmarshalAndValidate(String path) throws Exception {
        URI uri = getClass().getResource(path).toURI();
        return JaxbUtil.unmarshal(uri.toASCIIString(), true);
    }

}
