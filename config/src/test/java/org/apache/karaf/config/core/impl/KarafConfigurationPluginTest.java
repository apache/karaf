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

}
