package org.apache.karaf.config.core.impl;

import org.apache.felix.utils.properties.TypedProperties;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Enumeration;

public class KarafConfigurationPlugin implements ConfigurationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(KarafConfigurationPlugin.class);

    public static final String PLUGIN_ID = "org.apache.karaf.config.plugin";
    public static final int PLUGIN_RANKING = 500;

    @Override
    public void modifyConfiguration(ServiceReference<?> reference, Dictionary<String, Object> properties) {
        final Object pid = properties.get(Constants.SERVICE_PID);
        for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements(); ) {
            String key = keys.nextElement();
            // looking for env variable and system property matching key (pid.key).toUpperCase().replace('.', '_')
            String env = (pid + "." + key).toUpperCase().replaceAll("\\.", "_");
            String sys = pid + "." + key;
            if (System.getenv(env) != null) {
                if (properties.get(key) != null && (properties.get(key) instanceof Number)) {
                    properties.put(key, Integer.parseInt(System.getenv(env)));
                } else {
                    properties.put(key, System.getenv(env));
                }
            } else if (System.getProperty(sys) != null) {
                if (properties.get(key) != null && (properties.get(key) instanceof Number)) {
                    properties.put(key, Integer.parseInt(System.getProperty(sys)));
                } else {
                    properties.put(key, System.getProperty(sys));
                }
            }
        }
    }

}
