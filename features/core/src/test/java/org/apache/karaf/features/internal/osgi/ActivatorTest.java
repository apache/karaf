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
package org.apache.karaf.features.internal.osgi;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ActivatorTest {

  private static final String[] SYSTEM_PROPERTIES = {
      "karaf.etc",
      "org.apache.karaf.features.updateSnapshots",
      "org.apache.karaf.features.featuresRepositories",
      "org.apache.karaf.features.key1"
  };

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Activator activator;
  private ConfigurationAdmin configurationAdmin;
  private BundleContext bundleContext;

  @Before
  public void setup() throws Exception {
    activator = new TestActivator();

    configurationAdmin = niceMock(ConfigurationAdmin.class);
    Configuration featuresConfig = niceMock(Configuration.class);
    Dictionary<String, Object> properties = buildTestProperties();
    expect(featuresConfig.getProperties()).andReturn(properties);
    expect(configurationAdmin.getConfiguration(Activator.FEATURES_SERVICE_CONFIG)).andReturn(featuresConfig);

    bundleContext = niceMock(BundleContext.class);
    Bundle bundle = niceMock(Bundle.class);
    expect(bundle.getResource(anyString())).andReturn(null);
    expect(bundleContext.getBundle()).andReturn(bundle);

    replay(bundleContext, bundle, configurationAdmin, featuresConfig);
  }

  @After
  public void clearSystemProperties() {
    for (String property : SYSTEM_PROPERTIES) {
      System.clearProperty(property);
    }
  }

  private Dictionary<String, Object> buildTestProperties() {
    Dictionary<String, Object> properties = new Hashtable<>();
    properties.put("key1", "value1");
    properties.put("key2", "value2");

    return properties;
  }

  @Test
  public void testStart() throws Exception {
    activator.start(bundleContext);

    Dictionary<String, ?> configuration = ((TestActivator) activator).getConfiguration();
    assertThat(configuration.get("key1"), is("value1"));
    assertThat(configuration.get("key2"), is("value2"));
  }

  @Test
  public void testSystemPropertyOverridesConfigurationAdminValue() throws Exception {
    System.setProperty("org.apache.karaf.features.key1", "overridden");

    activator.start(bundleContext);

    TestActivator testActivator = (TestActivator) activator;
    assertThat(testActivator.getString("key1", null), is("overridden"));
    assertThat(testActivator.getString("key2", null), is("value2"));
    assertThat(testActivator.getConfiguration().get("key1"), is("value1"));
  }

  @Test
  public void testSystemPropertySuppliesKeyTheConfigurationLacks() throws Exception {
    System.setProperty("org.apache.karaf.features.updateSnapshots", "none");

    activator.start(bundleContext);

    TestActivator testActivator = (TestActivator) activator;
    assertThat(testActivator.getString("updateSnapshots", "crc"), is("none"));
    assertThat(testActivator.getString("featuresBoot", "standard"), is("standard"));
  }

  @Test
  public void testAppendIsAppliedOnce() throws Exception {
    System.setProperty("org.apache.karaf.features.key1", "${key1},more");

    activator.start(bundleContext);

    TestActivator testActivator = (TestActivator) activator;
    assertThat(testActivator.getString("key1", null), is("value1,more"));
    assertThat(testActivator.getString("key1", null), is("value1,more"));
  }

  @Test
  public void testArray() throws Exception {
    System.setProperty("org.apache.karaf.features.featuresRepositories", "[first,second]");

    activator.start(bundleContext);

    TestActivator testActivator = (TestActivator) activator;
    assertThat(testActivator.getStringArray("featuresRepositories", ""), is(new String[] { "first", "second" }));
  }

  @Test
  public void testSystemPropertyOverridesConfigurationFile() throws Exception {
    File etc = temporaryFolder.newFolder("etc");
    Files.write(new File(etc, Activator.FEATURES_SERVICE_CONFIG_FILE).toPath(),
        "updateSnapshots = crc\nfeaturesBoot = standard\n".getBytes(StandardCharsets.ISO_8859_1));
    System.setProperty("karaf.etc", etc.getAbsolutePath());
    System.setProperty("org.apache.karaf.features.updateSnapshots", "none");

    activator.start(bundleContext);

    TestActivator testActivator = (TestActivator) activator;
    assertThat(testActivator.getString("updateSnapshots", null), is("none"));
    assertThat(testActivator.getString("featuresBoot", null), is("standard"));
    assertThat(testActivator.getString("key1", null), nullValue());
  }

  @Test
  public void testSystemPropertyWithNoConfiguration() throws Exception {
    configurationAdmin = niceMock(ConfigurationAdmin.class);
    Configuration featuresConfig = niceMock(Configuration.class);
    expect(featuresConfig.getProperties()).andReturn(null);
    expect(configurationAdmin.getConfiguration(Activator.FEATURES_SERVICE_CONFIG)).andReturn(featuresConfig);
    replay(configurationAdmin, featuresConfig);
    System.setProperty("org.apache.karaf.features.updateSnapshots", "none");

    activator.start(bundleContext);

    TestActivator testActivator = (TestActivator) activator;
    assertThat(testActivator.getConfiguration(), nullValue());
    assertThat(testActivator.getString("updateSnapshots", "crc"), is("none"));
  }

  /**
   * Overwrite #getTrackedService which could otherwise not be mocked.
   * Overwrite #doStart to avoid full start up of the Activator.
   * Overwrite #getConfiguration to allow test validation.
   */
  public class TestActivator extends Activator {
    @Override
    protected <T> T getTrackedService(Class<T> clazz) {
      if (clazz.isAssignableFrom(ConfigurationAdmin.class)) {
        return (T) configurationAdmin;
      }
      return null;
    }

    @Override
    protected void doStart() throws Exception {
      // nop
    }

    @Override
    public Dictionary<String, ?> getConfiguration() {
      return super.getConfiguration();
    }

    @Override
    public String getString(String key, String def) {
      return super.getString(key, def);
    }

    @Override
    public String[] getStringArray(String key, String def) {
      return super.getStringArray(key, def);
    }
  }
}
