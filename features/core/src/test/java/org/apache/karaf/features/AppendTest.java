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

import java.util.Hashtable;
import java.util.Properties;

import junit.framework.TestCase;
import org.apache.karaf.features.internal.service.FeatureConfigInstaller;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.easymock.EasyMock;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class AppendTest extends TestCase {

	public void testLoad() throws Exception {

		System.setProperty("karaf.data", "data");
		System.setProperty("karaf.etc", "etc");

		RepositoryImpl r = new RepositoryImpl(getClass().getResource("internal/service/f08.xml").toURI());
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
        assertEquals(property, "data/pax-web-jsp");

		ConfigurationAdmin admin = EasyMock.createMock(ConfigurationAdmin.class);
		Configuration config = EasyMock.createMock(Configuration.class);
		EasyMock.expect(admin.listConfigurations(EasyMock.eq("(service.pid=org.ops4j.pax.web)")))
				.andReturn(new Configuration[] { config });
		Hashtable<String, Object> original = new Hashtable<>();
        original.put("javax.servlet.context.tempdir", "data/pax-web-jsp");
		EasyMock.expect(config.getProperties()).andReturn(original);

		Hashtable<String, Object> expected = new Hashtable<>();
        expected.put("org.ops4j.pax.web", "data/pax-web-jsp");
		expected.put("org.apache.karaf.features.configKey", "org.ops4j.pax.web");
		expected.put("foo", "bar");
		EasyMock.expectLastCall();
		EasyMock.replay(admin, config);

		FeatureConfigInstaller installer = new FeatureConfigInstaller(admin);
		installer.installFeatureConfigs(feature);
		EasyMock.verify(admin, config);

		EasyMock.reset(admin, config);
		EasyMock.expect(admin.listConfigurations(EasyMock.eq("(service.pid=org.ops4j.pax.web)")))
				.andReturn(new Configuration[]{config});
		original = new Hashtable<>();
		original.put("org.apache.karaf.features.configKey", "org.ops4j.pax.web");
		original.put("javax.servlet.context.tempdir", "value");
		original.put("foo", "bar");
		EasyMock.expect(config.getProperties()).andReturn(original);
		EasyMock.replay(admin, config);
		installer.installFeatureConfigs(feature);
		EasyMock.verify(admin, config);
	}
}
