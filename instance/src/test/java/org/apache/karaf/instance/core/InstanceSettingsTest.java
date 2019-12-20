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
package org.apache.karaf.instance.core;

import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

public class InstanceSettingsTest extends TestCase {
    public void testInstanceSettings() {
        InstanceSettings is =
            new InstanceSettings(1, 1, 1, null, null, Collections.emptyList(), Collections.singletonList("hi"));
        assertEquals(1, is.getSshPort());
        assertEquals(1, is.getRmiRegistryPort());
        assertEquals(1, is.getRmiServerPort());
        Assert.assertNull(is.getLocation());
        assertEquals(Collections.singletonList("hi"), is.getFeatures());
        assertEquals(0, is.getFeatureURLs().size());
    }
    
    public void testEqualsHashCode() {
        testEqualsHashCode(1, 1, 1, "top", "foo", Collections.emptyList(), Collections.singletonList("hi"));
        testEqualsHashCode(0, 0, 0, null, null, null, null);
    }

    private void testEqualsHashCode(int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, List<String> featureURLs, List<String> features) {
        InstanceSettings is = new InstanceSettings(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, featureURLs, features);
        InstanceSettings is2 = new InstanceSettings(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, featureURLs, features);
        assertEquals(is, is2);
        assertEquals(is.hashCode(), is2.hashCode());
    }
    
    public void testEqualsHashCode2() {
        InstanceSettings is = new InstanceSettings(1, 1, 1, "top", "foo", Collections.emptyList(),
            Collections.singletonList("hi"));
        Assert.assertNotEquals(null, is);
        Assert.assertNotEquals(is, new Object());
        assertEquals(is, is);
    }
}
