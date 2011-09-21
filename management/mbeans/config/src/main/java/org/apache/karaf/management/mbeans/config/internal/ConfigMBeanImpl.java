/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.management.mbeans.config.internal;

import org.apache.karaf.management.mbeans.config.ConfigMBean;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.util.*;

/**
 * Implementation of the ConfigMBean.
 */
public class ConfigMBeanImpl extends StandardMBean implements ConfigMBean {

    private ConfigurationAdmin configurationAdmin;

    public ConfigurationAdmin getConfigurationAdmin() {
        return this.configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public ConfigMBeanImpl() throws NotCompliantMBeanException {
        super(ConfigMBean.class);
    }

    public List<String> list() throws Exception {
        Configuration[] configurations = configurationAdmin.listConfigurations(null);
        List<String> pids = new ArrayList<String>();
        for (int i = 0; i < configurations.length; i++) {
            pids.add(configurations[i].getPid());
        }
        return pids;
    }

    public void delete(String pid) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        configuration.delete();
    }

    public Map<String, String> proplist(String pid) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        Dictionary dictionary = configuration.getProperties();
        Map<String, String> propertiesMap = new HashMap<String, String>();
        for (Enumeration e = dictionary.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            Object value = dictionary.get(key);
            propertiesMap.put(key.toString(), value.toString());
        }
        return propertiesMap;
    }

    public void propdel(String pid, String key) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        Dictionary dictionary = configuration.getProperties();
        dictionary.remove(key);
    }

    public void propappend(String pid, String key, String value) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        Dictionary dictionary = configuration.getProperties();
        Object currentValue = dictionary.get(key);
        if (currentValue == null) {
            dictionary.put(key, value);
        } else if (currentValue instanceof String) {
            dictionary.put(key, currentValue + value);
        } else {
            throw new IllegalStateException("Current value is not a String");
        }
    }

    public void propset(String pid, String key, String value) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        Dictionary dictionary = configuration.getProperties();
        dictionary.put(key, value);
    }

}
