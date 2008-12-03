package org.apache.servicemix.kernel.jaas.modules.osgi;

import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigAdminHolder {

    private static ConfigurationAdmin configAdmin;

    public static ConfigurationAdmin getService() {
        return configAdmin;
    }

    public void setService(ConfigurationAdmin configAdmin) {
        ConfigAdminHolder.configAdmin = configAdmin;
    }

}
