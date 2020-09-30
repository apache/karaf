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
package org.apache.karaf.config.core.impl;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Constants;

import java.util.Dictionary;
import java.util.Hashtable;

public class KarafConfigurationPluginTest {

    @Test
    public void testSystemProperty() throws Exception {
        System.setProperty("org.apache.karaf.shell.sshPort", "8102");
        KarafConfigurationPlugin plugin = new KarafConfigurationPlugin();
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, "org.apache.karaf.shell");
        properties.put("foo", "bar");
        properties.put("sshPort", 8101);
        plugin.modifyConfiguration(null, properties);

        Assert.assertEquals(8102, properties.get("sshPort"));
        Assert.assertEquals("bar", properties.get("foo"));
    }

    @Test
    public void testAppending() throws Exception {
        System.setProperty("org.apache.karaf.features.repositories", "${repositories},third");
        KarafConfigurationPlugin plugin = new KarafConfigurationPlugin();
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, "org.apache.karaf.features");
        properties.put("repositories", "first,second");
        properties.put("foo", "bar");
        plugin.modifyConfiguration(null, properties);

        Assert.assertEquals("first,second,third", properties.get("repositories"));
        Assert.assertEquals("bar", properties.get("foo"));
    }

}
