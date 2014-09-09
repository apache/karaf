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

import java.util.Properties;

import junit.framework.TestCase;

import org.apache.karaf.features.internal.RepositoryImpl;


public class AppendTest extends TestCase {

    public void testLoad() throws Exception {
		System.setProperty("karaf.data", "data");

        RepositoryImpl r = new RepositoryImpl(getClass().getResource("internal/f07.xml").toURI());
        // Check repo
        Feature[] features = r.getFeatures();
        assertNotNull(features);
        assertEquals(1, features.length);
        Feature feature = features[0];

		ConfigInfo configInfo = feature.getConfigurations().get(0);
		assertNotNull(configInfo);
		assertTrue(configInfo.isAppend());
		
		Properties properties = configInfo.getProperties();
		assertNotNull(properties);
		String property = properties.getProperty("javax.servlet.context.tempdir");
		assertNotNull(property);
		assertFalse(property.contains("${"));

    }
}
