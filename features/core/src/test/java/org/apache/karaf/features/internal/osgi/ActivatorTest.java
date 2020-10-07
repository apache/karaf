package org.apache.karaf.features.internal.osgi;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.partialMockBuilder;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import org.easymock.EasyMock;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
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
