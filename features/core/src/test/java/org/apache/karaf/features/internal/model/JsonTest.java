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
package org.apache.karaf.features.internal.model;

import org.junit.Assert;
import org.junit.Test;

public class JsonTest {

    @Test
    public void testUnmarshall() throws Exception {
        Assert.assertTrue(JacksonUtil.isJson(this.getClass().getResourceAsStream("/org/apache/karaf/features/repo.json")));

        Features features = JacksonUtil.unmarshal(this.getClass().getResourceAsStream("/org/apache/karaf/features/repo.json"));

        Assert.assertEquals("test", features.getName());

        Assert.assertEquals("mvn:org.apache.karaf/test/1.0-SNAPSHOT", features.getRepository().get(0));

        Assert.assertEquals("test-feature", features.getFeature().get(0).getName());
        Assert.assertEquals("1.0.0.SNAPSHOT", features.getFeature().get(0).getVersion());
        Assert.assertEquals("This is test feature", features.getFeature().get(0).getDescription());
        Assert.assertEquals("obr", features.getFeature().get(0).getResolver());
        Assert.assertEquals("auto", features.getFeature().get(0).getInstall());
        Assert.assertEquals(30, features.getFeature().get(0).getStartLevel());
        Assert.assertEquals(false, features.getFeature().get(0).isHidden());

        Assert.assertEquals("inner-feature", features.getFeature().get(0).getFeature().get(0).getName());
        Assert.assertEquals("[1,2)", features.getFeature().get(0).getFeature().get(0).getVersion());

        Assert.assertEquals("mvn:org.apache.karaf/test-bundle/1.0-SNAPSHOT", features.getFeature().get(0).getBundle().get(0).getLocation());
        Assert.assertEquals(40, features.getFeature().get(0).getBundle().get(0).getStartLevel());
        Assert.assertEquals(true, features.getFeature().get(0).getBundle().get(0).isDependency());
        Assert.assertEquals(true, features.getFeature().get(0).getBundle().get(0).isStart());

        Assert.assertEquals("test-config", features.getFeature().get(0).getConfig().get(0).getName());
        Assert.assertEquals(false, features.getFeature().get(0).getConfig().get(0).isAppend());
        Assert.assertEquals(false, features.getFeature().get(0).getConfig().get(0).isExternal());
        Assert.assertEquals(false, features.getFeature().get(0).getConfig().get(0).isOverride());
        Assert.assertEquals("foo=bar", features.getFeature().get(0).getConfig().get(0).getValue());

        Assert.assertEquals("test-configfile", features.getFeature().get(0).getConfigfile().get(0).getFinalname());
        Assert.assertEquals(false, features.getFeature().get(0).getConfigfile().get(0).isOverride());
        Assert.assertEquals("mvn:org.apache.karaf/test-configfile/1.0-SNAPSHOT", features.getFeature().get(0).getConfigfile().get(0).getLocation());
    }

}
