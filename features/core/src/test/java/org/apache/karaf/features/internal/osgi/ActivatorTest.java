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

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ActivatorTest {

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
  }
}
