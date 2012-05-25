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
package org.apache.karaf.main;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class FeatureReaderTest {
    
    @Test
    public void testReadBundlesFromFeature() throws URISyntaxException, Exception {
        URL res = this.getClass().getClassLoader().getResource("test-karaf-home/system/org/apache/karaf/features/framework/1.0.0/framework-1.0.0-features.xml");
        List<BundleInfo> bundles = new FeatureReader().readBundles(res.toURI(), "framework");
        Assert.assertEquals(1, bundles.size());
        Assert.assertEquals(10, bundles.get(0).startLevel.intValue());
        Assert.assertEquals(new URI("mvn:org.apache.aries.blueprint/org.apache.aries.blueprint.api/0.3.1"), bundles.get(0).uri);
    }
}
