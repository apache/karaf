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
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;

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
        this(configAdmin, null);
    }

    public ConfigRepositoryImpl(ConfigurationAdmin configAdmin, File storage) {
        this.configAdmin = configAdmin;
        this.storage = storage;
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#update(java.lang.String, java.util.Dictionary, boolean)
     */
    @Override
    public void update(String pid, Dictionary<String, Object> properties) throws IOException {
        LOGGER.trace("Update configuration {}", pid);
        Configuration cfg = configAdmin.getConfiguration(pid, null);
        if (storage != null) {
            // Check, whether a file location is already provided.
            if (properties.get(FILEINSTALL_FILE_NAME) == null) {
                String cfgFileName = pid + ".cfg";
                File cfgFile = new File(storage, cfgFileName);
                properties.put(FILEINSTALL_FILE_NAME, cfgFile.getCanonicalFile().toURI().toString());
            }
        }
        cfg.update(properties);
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#delete(java.lang.String)
     */
    @Override
    public void delete(String pid) throws Exception {
        LOGGER.trace("Delete configuration {}", pid);
        Configuration configuration = configAdmin.getConfiguration(pid, null);
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

    private File getCfgFileFromProperties(Dictionary<String, Object> properties) throws URISyntaxException, MalformedURLException {
        File cfgFile = null;
        if (properties != null) {
            Object val = properties.get(FILEINSTALL_FILE_NAME);
            if (val instanceof URL) {
                cfgFile = new File(((URL) val).toURI());
            }
            if (val instanceof URI) {
                cfgFile = new File((URI) val);
            }
            if (val instanceof String) {
                cfgFile = new File(new URL((String) val).toURI());
            }
        }
        return cfgFile;
    }

    protected void updateStorage(String pid, Dictionary<String, Object> props) throws IOException {
        if (storage != null) {
            Configuration cfg = configAdmin.getConfiguration(pid, null);
            // Initialize cfgFile with default location. Value gets overwritten when the existing configuration references a correct location.
            File cfgFile = new File(storage, pid + ".cfg");
            if (cfg != null) {
                Dictionary<String, Object> oldProps = cfg.getProperties();
                if (oldProps != null && oldProps.get(FILEINSTALL_FILE_NAME) != null) {
                    try {
                        cfgFile = getCfgFileFromProperties(oldProps);
                        if (cfgFile == null) {
                            throw new IOException("The configuration value '" + oldProps.get(FILEINSTALL_FILE_NAME)
                                    + "' for '" + FILEINSTALL_FILE_NAME + "' does not represent a valid file location.");
                        }
                    } catch (URISyntaxException | MalformedURLException e) {
                        throw new IOException(e);
                    }
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
    public Dictionary<String, Object> getConfigProperties(String pid) throws IOException, InvalidSyntaxException {
        if (pid != null && configAdmin != null) {
            Configuration configuration = configAdmin.getConfiguration(pid, null);
            if (configuration != null) {
                Dictionary<String, Object> props = configuration.getProperties();
                return (props != null) ? props : new Hashtable<>();
            }
        }
        return null;
    }

    @Override
    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    @Override
    public String createFactoryConfiguration(String factoryPid, Dictionary<String, Object> properties) {
        return createFactoryConfiguration(factoryPid, null, properties);
    }

    @Override
    public String createFactoryConfiguration(String factoryPid, String alias, Dictionary<String, Object> properties) {
        try {
            Configuration config = configAdmin.createFactoryConfiguration(factoryPid, null);
            if (storage != null) {
                // Check, whether a file location is already provided.
                if (properties.get(FILEINSTALL_FILE_NAME) == null) {
                    // Create a synthetic unique alias for the factory
                    // configuration when it is unspecified.
                    if (alias == null) {
                        // Felix Fileinstall uses the hyphen as separator
                        // between factoryPid and alias. For safety reasons, all
                        // hyphens are removed from the generated UUID.
                        alias = UUID.randomUUID().toString().replaceAll("-", "");
                    }
                    String cfgFileName = factoryPid + "-" + alias + ".cfg";
                    File cfgFile = new File(storage, cfgFileName);
                    properties.put(FILEINSTALL_FILE_NAME, cfgFile.getCanonicalFile().toURI().toString());
                }
            }
            config.update(properties);
            String pid = config.getPid();
//            updateStorage(pid, properties);
            return pid;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
