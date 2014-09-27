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
package org.apache.karaf.config.core.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.config.core.ConfigRepository;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigRepositoryImpl implements ConfigRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRepositoryImpl.class);

    private static final String FILEINSTALL_FILE_NAME = "felix.fileinstall.filename";

    private ConfigurationAdmin configAdmin;
    private File storage;

    public ConfigRepositoryImpl(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public ConfigRepositoryImpl(ConfigurationAdmin configAdmin, File storage) {
        this.configAdmin = configAdmin;
        this.storage = storage;
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#update(java.lang.String, java.util.Dictionary, boolean)
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void update(String pid, Dictionary props) throws IOException {
        LOGGER.trace("Update configuration {}", pid);
        Configuration cfg = this.configAdmin.getConfiguration(pid, null);
        cfg.update(props);
        try {
            updateStorage(pid, props);
        } catch (Exception e) {
            LOGGER.warn("Can't update cfg file", e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#delete(java.lang.String)
     */
    @Override
    public void delete(String pid) throws Exception {
        LOGGER.trace("Delete configuration {}", pid);
        Configuration configuration = this.configAdmin.getConfiguration(pid, null);
        configuration.delete();
        try {
            deleteStorage(pid);
        } catch (Exception e) {
            LOGGER.warn("Can't delete cfg file", e);
        }
    }
    
    protected void deleteStorage(String pid) throws Exception {
        if (storage != null) {
            File cfgFile = new File(storage, pid + ".cfg");
            LOGGER.trace("Delete {}", cfgFile.getName());
            cfgFile.delete();
        }
    }

    protected void updateStorage(String pid, Dictionary props) throws IOException {
        if (storage != null) {
            // get the cfg file
            File cfgFile = new File(storage, pid + ".cfg");
            Configuration cfg = configAdmin.getConfiguration(pid, null);
            // update the cfg file depending of the configuration
            if (cfg != null && cfg.getProperties() != null) {
                Object val = cfg.getProperties().get(FILEINSTALL_FILE_NAME);
                try {
                    if (val instanceof URL) {
                        cfgFile = new File(((URL) val).toURI());
                    }
                    if (val instanceof URI) {
                        cfgFile = new File((URI) val);
                    }
                    if (val instanceof String) {
                        cfgFile = new File(new URL((String) val).toURI());
                    }
                } catch (Exception e) {
                    throw (IOException) new IOException(e.getMessage()).initCause(e);
                }
            }
            LOGGER.trace("Update {}", cfgFile.getName());
            // update the cfg file
            Properties properties = new Properties(cfgFile);
            for (Enumeration<String> keys = props.keys(); keys.hasMoreElements(); ) {
                String key = keys.nextElement();
                if (!Constants.SERVICE_PID.equals(key)
                        && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                        && !FILEINSTALL_FILE_NAME.equals(key)) {
                    if (props.get(key) != null) {
                        properties.put(key, props.get(key).toString());
                    }
                }
            }
            // remove "removed" properties from the cfg file
            ArrayList<String> propertiesToRemove = new ArrayList<>();
            for (String key : properties.keySet()) {
                if (props.get(key) == null
                        && !Constants.SERVICE_PID.equals(key)
                        && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                        && !FILEINSTALL_FILE_NAME.equals(key)) {
                    propertiesToRemove.add(key);
                }
            }
            for (String key : propertiesToRemove) {
                properties.remove(key);
            }
            // save the cfg file
            storage.mkdirs();
            properties.save();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#getConfigProperties(java.lang.String)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Dictionary getConfigProperties(String pid) throws IOException, InvalidSyntaxException {
        if (pid != null && configAdmin != null) {
            Configuration configuration = this.configAdmin.getConfiguration(pid, null);
            if(configuration != null) {
                Dictionary props = configuration.getProperties();
                return (props != null) ? props : new Hashtable<String, String>();
            }
        }
        return null;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return this.configAdmin;
    }

	@Override
	public String createFactoryConfiguration(String factoryPid, Dictionary<String, ?> properties) {
		try {
			Configuration config = configAdmin.createFactoryConfiguration(factoryPid, null);
			config.update(properties);
			return config.getPid();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
