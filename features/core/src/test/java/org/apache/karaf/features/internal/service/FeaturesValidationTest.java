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

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Library;
import org.apache.karaf.features.Repository;
import org.junit.Test;

public class FeaturesValidationTest {

    @Test
    public void testNs10() throws Exception {
        Repository features = unmarshalAndValidate("f02.xml");
        assertNotNull(features);
    }

    @Test
    public void testNs10NoName() throws Exception {
        Repository features = unmarshalAndValidate("f03.xml");
        assertNotNull(features);
    }

    @Test
    public void testNs11() throws Exception {
        Repository features = unmarshalAndValidate("f04.xml");;
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
        Repository features = unmarshalAndValidate("f06.xml");
        assertNotNull(features);
    }

    @Test
    public void testNs13() throws Exception {
        Repository features = unmarshalAndValidate("f07.xml");
        assertNotNull(features);
        Feature f0 = features.getFeatures()[0];
        Feature f1 = features.getFeatures()[1];
        assertEquals("2.5.6.SEC02", f0.getVersion());
        assertTrue(f1.isHidden());
        assertNotNull(f1.getLibraries());
        assertEquals(1, f0.getLibraries().size());
        Library lib = f0.getLibraries().get(0);
        assertEquals("my-library", lib.getLocation());
        assertEquals(Library.TYPE_ENDORSED, lib.getType());
        assertFalse(lib.isExport());
        assertTrue(lib.isDelegate());
    }

    private Repository unmarshalAndValidate(String path) throws Exception {
        URI uri = getClass().getResource(path).toURI();
        return new RepositoryImpl(uri, true);
    }

}
