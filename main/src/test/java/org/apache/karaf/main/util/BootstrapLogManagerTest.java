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
package org.apache.karaf.main.util;

import java.io.File;
import java.util.logging.Handler;

import org.apache.felix.utils.properties.Properties;
import org.junit.Assert;
import org.junit.Test;

public class BootstrapLogManagerTest {
	
	private Properties getConfigProperties() {
		Properties configProps = new Properties();
		configProps.put("karaf.log", "target/log");
		return configProps;
	}
	
	@Test
	public void testGetLogManagerNoProperties() {
		BootstrapLogManager.setProperties(getConfigProperties());
		try {
			BootstrapLogManager.getDefaultHandler();
		} catch (IllegalStateException e) {
			Assert.assertEquals("Properties must be set before calling getDefaultHandler", e.getMessage());
		}
	}
	
	@Test
	public void testGetLogManager() {
		new File("target/log/karaf.log").delete();
		BootstrapLogManager.setProperties(getConfigProperties());
		Handler handler = BootstrapLogManager.getDefaultHandler();
		Assert.assertNotNull(handler);
		assertExists("target/log/karaf.log");
	}
	
	@Test
	public void testGetLogManagerFromPaxLoggingConfig() {
		new File("target/test.log").delete();
		Properties configProps = getConfigProperties();
		BootstrapLogManager.setProperties(configProps, "src/test/resources/org.ops4j.pax.logging.cfg");
		Handler handler = BootstrapLogManager.getDefaultHandler();
		Assert.assertNotNull(handler);
		assertExists("target/test.log");
	}
	
	private void assertExists(String path) {
		Assert.assertTrue("File should exist at " + path, new File(path).exists());
	}
}
