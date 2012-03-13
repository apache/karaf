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
package org.apache.karaf.config.management.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.karaf.config.core.ConfigRepository;
import org.apache.karaf.config.management.ConfigMBean;
import org.osgi.service.cm.Configuration;

/**
 * Implementation of the ConfigMBean.
 */
public class ConfigMBeanImpl extends StandardMBean implements ConfigMBean {

    private ConfigRepository configRepo;

    public ConfigMBeanImpl() throws NotCompliantMBeanException {
        super(ConfigMBean.class);
    }

    private Configuration getConfiguration(String pid) throws IOException {
        Configuration configuration = configRepo.getConfigAdmin().getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        return configuration;
    }
    
    @SuppressWarnings("rawtypes")
    private Dictionary getConfigProperties(String pid) throws IOException {
        Configuration configuration = getConfiguration(pid);

        Dictionary dictionary = configuration.getProperties();
        if (dictionary == null) {
            dictionary = new java.util.Properties();
        }
        return dictionary;
    }

    /**
     * Get all config pids
     */
    public List<String> getConfigs() throws Exception {
        Configuration[] configurations = this.configRepo.getConfigAdmin().listConfigurations(null);
        List<String> pids = new ArrayList<String>();
        for (int i = 0; i < configurations.length; i++) {
            pids.add(configurations[i].getPid());
        }
        return pids;
    }

    @SuppressWarnings("rawtypes")
    public void create(String pid) throws Exception {
        configRepo.update(pid, new Hashtable());
    }
    

    public void update(String pid, Map<String, String> properties) throws IOException {
        if (properties == null) {
            properties = new HashMap<String, String>();
        }
        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        for (String key : properties.keySet()) {
            dictionary.put(key, properties.get(key));
        }
        configRepo.update(pid, dictionary);
    }

    public void delete(String pid) throws Exception {
        this.configRepo.delete(pid);
    }

    @SuppressWarnings("rawtypes")
    public Map<String, String> listProperties(String pid) throws Exception {
        Dictionary dictionary = getConfigProperties(pid);

        Map<String, String> propertiesMap = new HashMap<String, String>();
        for (Enumeration e = dictionary.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            Object value = dictionary.get(key);
            propertiesMap.put(key.toString(), value.toString());
        }
        return propertiesMap;
    }

    @SuppressWarnings("rawtypes")
    public void deleteProperty(String pid, String key) throws Exception {
        Dictionary dictionary = getConfigProperties(pid);
        dictionary.remove(key);
        configRepo.update(pid, dictionary, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void appendProperty(String pid, String key, String value) throws Exception {
        Dictionary dictionary = getConfigProperties(pid);
        Object currentValue = dictionary.get(key);
        if (currentValue == null) {
            dictionary.put(key, value);
        } else if (currentValue instanceof String) {
            dictionary.put(key, currentValue + value);
        } else {
            throw new IllegalStateException("Current value is not a String");
        }
        configRepo.update(pid, dictionary, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setProperty(String pid, String key, String value) throws Exception {
        Dictionary dictionary = getConfigProperties(pid);
        dictionary.put(key, value);
        configRepo.update(pid, dictionary, false);
    }

    public void setConfigRepo(ConfigRepository configRepo) {
        this.configRepo = configRepo;
    }

}
