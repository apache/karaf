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
package org.apache.karaf.features;

import junit.framework.TestCase;

public class FeatureTest extends TestCase {

    public void testValueOf() {
        Feature feature = org.apache.karaf.features.internal.model.Feature.valueOf("name/1.0.0");
        assertEquals(feature.getName(), "name");
        assertEquals(feature.getVersion(), "1.0.0");
        feature = org.apache.karaf.features.internal.model.Feature.valueOf("name");
        assertEquals(feature.getName(), "name");
        assertEquals(feature.getVersion(), org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION);
    }

}
