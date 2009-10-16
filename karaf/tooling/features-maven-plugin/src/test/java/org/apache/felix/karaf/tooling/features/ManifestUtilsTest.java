/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.tooling.features;

import static org.apache.felix.karaf.tooling.features.ManifestUtils.matches;

import junit.framework.TestCase;
import org.osgi.impl.bundle.obr.resource.ManifestEntry;
import org.osgi.impl.bundle.obr.resource.VersionRange;

import java.util.HashMap;

/**
 * Test cased for {@link org.apache.felix.karaf.tooling.features.ManifestUtils} 
 */
public class ManifestUtilsTest extends TestCase {

    public void testIsOptional() {
        ManifestEntry entry = new ManifestEntry("org.apache.karaf.test");
        assertFalse(ManifestUtils.isOptional(entry));

        entry.directives = new HashMap();
        assertFalse(ManifestUtils.isOptional(entry));

        entry.directives.put("resolution", "mandatory");
        assertFalse(ManifestUtils.isOptional(entry));

        entry.directives.put("resolution", "optional");
        assertTrue(ManifestUtils.isOptional(entry));
    }

    public void testMatches() {
        assertFalse(matches(entry("org.apache.karaf.dev"), entry("org.apache.karaf.test")));
        assertTrue(matches(entry("org.apache.karaf.test"), entry("org.apache.karaf.test")));

        assertFalse(matches(entry("org.apache.karaf.test", "1.2.0"), entry("org.apache.karaf.test", "1.1.0")));
        assertTrue(matches(entry("org.apache.karaf.test", "1.1.0"), entry("org.apache.karaf.test", "1.1.0")));

        // a single version means >= 1.0.0, so 1.1.O should be a match
        assertTrue(matches(entry("org.apache.karaf.test", "1.0.0"), entry("org.apache.karaf.test", "1.1.0")));

        assertFalse(matches(entry("org.apache.karaf.test", "[1.1.0, 1.2.0)"), entry("org.apache.karaf.test", "1.0.0")));
        assertFalse(matches(entry("org.apache.karaf.test", "[1.1.0, 1.2.0)"), entry("org.apache.karaf.test", "1.2.0")));
        assertTrue(matches(entry("org.apache.karaf.test", "[1.1.0, 1.2.0)"), entry("org.apache.karaf.test", "1.1.0")));
        assertTrue(matches(entry("org.apache.karaf.test", "[1.1.0, 1.2.0)"), entry("org.apache.karaf.test", "1.1.1")));
    }

    private ManifestEntry entry(String name) {
        return new ManifestEntry(name);
    }

    private ManifestEntry entry(String name, String version) {
        return new ManifestEntry(name, new VersionRange(version));
    }
}
