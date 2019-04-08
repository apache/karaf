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
package org.apache.karaf.config.core.impl;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.config.core.ConfigMBean;
import org.apache.karaf.config.core.ConfigRepository;
import org.apache.karaf.util.StreamUtils;
import org.osgi.framework.InvalidSyntaxException;
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
        Configuration configuration = configRepo.getConfigAdmin().getConfiguration(pid, null);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        return configuration;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private TypedProperties getConfigProperties(String pid) throws IOException, InvalidSyntaxException {
        return configRepo.getConfig(pid);
    }

    /**
     * Get all config pids
     */
    @Override
    public List<String> getConfigs() throws MBeanException {
        try {
            return Arrays.stream(configRepo.getConfigAdmin().listConfigurations(null))
                    .map(Configuration::getPid)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void create(String pid) throws MBeanException {
        try {
            configRepo.update(pid, new TypedProperties());
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void install(String url, String finalname, boolean override) throws MBeanException {
        if (finalname.contains("..")) {
            throw new IllegalArgumentException("For security reason, relative path is not allowed in config file final name");
        }
        try {
            File etcFolder = new File(System.getProperty("karaf.etc"));
            File file = new File(etcFolder, finalname);
            if (file.exists()) {
                if (!override) {
                    throw new IllegalArgumentException("Configuration file {} already exists " + finalname);
                }
            }

            try (InputStream is = new BufferedInputStream(new URL(url).openStream())) {
                if (!file.exists()) {
                    File parentFile = file.getParentFile();
                    if (parentFile != null) {
                        parentFile.mkdirs();
                    }
                    file.createNewFile();
                }
                try (FileOutputStream fop = new FileOutputStream(file)) {
                    StreamUtils.copy(is, fop);
                }
            } catch (RuntimeException | MalformedURLException e) {
                throw e;
            }
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void delete(String pid) throws MBeanException {
        try {
            this.configRepo.delete(pid);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, String> listProperties(String pid) throws MBeanException {
        try {
            TypedProperties dictionary = getConfigProperties(pid);
            Map<String, String> propertiesMap = new HashMap<>();
            for (Map.Entry<String, Object> e : dictionary.entrySet()) {
                propertiesMap.put(e.getKey(), displayValue(e.getValue().toString()));
            }
            return propertiesMap;
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    protected String displayValue(Object value) {
        if (value == null) {
            return "<null>";
        }
        if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        }
        return value.toString();
    }

    @Override
    public void deleteProperty(String pid, String key) throws MBeanException {
        try {
            TypedProperties dictionary = getConfigProperties(pid);
            dictionary.remove(key);
            configRepo.update(pid, dictionary);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void appendProperty(String pid, String key, String value) throws MBeanException {
        try {
            TypedProperties dictionary = getConfigProperties(pid);
            Object currentValue = dictionary.get(key);
            if (currentValue == null) {
                dictionary.put(key, value);
            } else if (currentValue instanceof String) {
                dictionary.put(key, currentValue + value);
            } else {
                throw new IllegalStateException("Current value is not a String");
            }
            configRepo.update(pid, dictionary);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void setProperty(String pid, String key, String value) throws MBeanException {
        try {
            TypedProperties dictionary = getConfigProperties(pid);
            dictionary.put(key, value);
            configRepo.update(pid, dictionary);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public String getProperty(String pid, String key) throws MBeanException {
        try {
            TypedProperties dictionary = getConfigProperties(pid);
            Object value = dictionary.get(key);
            if (value != null) {
                return value.toString();
            }
            return null;
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void update(String pid, Map<String, String> properties) throws MBeanException {
        try {
            TypedProperties props = configRepo.getConfig(pid);
            props.putAll(properties);
            configRepo.update(pid, props);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

	private Dictionary<String, Object> toDictionary(
			Map<String, String> properties) {
		Dictionary<String, Object> dictionary = new Hashtable<>();
		for (String key : properties.keySet()) {
		    dictionary.put(key, properties.get(key));
		}
		return dictionary;
	}


    public void setConfigRepo(ConfigRepository configRepo) {
        this.configRepo = configRepo;
    }

	@Override
	public String createFactoryConfiguration(String factoryPid, Map<String, String> properties) throws MBeanException {
        try {
            TypedProperties props = new TypedProperties();
            props.putAll(properties);
            return configRepo.createFactoryConfiguration(factoryPid, props);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
	}

}
