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
package org.apache.felix.karaf.admin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import org.apache.felix.karaf.admin.InstanceSettings;
import org.junit.Assert;

public class InstanceSettingsTest extends TestCase {
    public void testInstanceSettings() {
        InstanceSettings is =
            new InstanceSettings(1, null, Collections.<String>emptyList(), Arrays.asList("hi"));
        assertEquals(1, is.getPort());
        Assert.assertNull(is.getLocation());
        assertEquals(Arrays.asList("hi"), is.getFeatures());
        assertEquals(0, is.getFeatureURLs().size());
    }
    
    public void testEqualsHashCode() {
        testEqualsHashCode(1, "top", Collections.<String>emptyList(), Arrays.asList("hi"));
        testEqualsHashCode(0, null, null, null);
    }

    private void testEqualsHashCode(int port, String location, List<String> featureURLs, List<String> features) {
        InstanceSettings is = new InstanceSettings(port, location, featureURLs, features);
        InstanceSettings is2 = new InstanceSettings(port, location, featureURLs, features);
        assertEquals(is, is2);
        assertEquals(is.hashCode(), is2.hashCode());
    }
    
    public void testEqualsHashCode2() {
        InstanceSettings is = new InstanceSettings(1, "top", Collections.<String>emptyList(), Arrays.asList("hi"));
        Assert.assertFalse(is.equals(null));
        Assert.assertFalse(is.equals(new Object()));
        assertEquals(is, is);
    }
}
