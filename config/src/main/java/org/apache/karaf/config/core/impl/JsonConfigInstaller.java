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

import org.apache.felix.cm.json.Configurations;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.internal.DirectoryWatcher;
import org.apache.felix.utils.collections.DictionaryAsMap;
import org.apache.karaf.util.config.ConfigurationPID;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

public class JsonConfigInstaller implements ArtifactInstaller, ConfigurationListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(JsonConfigInstaller.class);

    private final ConfigurationAdmin configurationAdmin;
    private final Map<String, String> pidToFile = new HashMap<>();

    public JsonConfigInstaller(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
        this.init();
    }
    
    private void init() {
        try {
            Configuration[] configs = configurationAdmin.listConfigurations("("+DirectoryWatcher.FILENAME+"=*)");
            if (configs != null)
            {
                for (Configuration config : configs)
                {
                    @SuppressWarnings("rawtypes")
                    Dictionary dict = config.getProperties();
                    String fileName = dict != null ? (String) dict.get( DirectoryWatcher.FILENAME ) : null;
                    if (fileName != null && fileName.endsWith(".json"))
                    {   
                        LOGGER.debug("Monitoring json cfg {} (fileName={})", config.getPid(), fileName);
                        pidToFile.put(config.getPid(), fileName);
                    }
                }
            }
        }
        catch (Exception ex) {
            LOGGER.error("Unable to get configurations", ex);
        }                
    }

    @Override
    public boolean canHandle(File artifact) {
        return artifact.getName().endsWith(".json");
    }

    @Override
    public void install(File artifact) throws Exception {
        setConfig(artifact);
    }

    @Override
    public void update(File artifact) throws Exception {
        setConfig(artifact);
    }

    @Override
    public void uninstall(File artifact) throws Exception {
        deleteConfig(artifact);
    }

    private void setConfig(File artifact) throws Exception {
        final String filename = artifact.getName();
        final ConfigurationPID configurationPID = ConfigurationPID.parseFilename(filename);
        Configuration configuration = getConfiguration(toConfigKey(artifact), configurationPID);
        Dictionary<String, Object> props = configuration.getProperties();
        Hashtable<String, Object> old = props != null ? new Hashtable<>(new DictionaryAsMap<>(props)) : null;
        Hashtable<String, Object> properties = Configurations.buildReader().build(new FileReader(artifact)).readConfiguration();
        if (old != null) {
            old.remove(DirectoryWatcher.FILENAME);
            old.remove(Constants.SERVICE_PID);
            old.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
        }
        boolean updated = false;
        if (old == null || old.size() != properties.size()) {
            updated = true;
        } else {
            for (String key : old.keySet()) {
                Object oldValue = old.get(key);
                Object propertiesValue = properties.get(key);
                if (oldValue instanceof Object[] && propertiesValue instanceof Object[]) {
                    updated = !Arrays.deepEquals((Object[]) oldValue, (Object[]) propertiesValue);
                } else {
                    updated = !oldValue.equals(propertiesValue);
                }
                if (updated) {
                    break;
                }
            }
        }
        if (updated) {
            properties.put(DirectoryWatcher.FILENAME, toConfigKey(artifact));
            if (old == null) {
                LOGGER.info("Creating configuration from {}", artifact.getName());
            } else {
                LOGGER.info("Updating configuration from {}", artifact.getName());
            }
            configuration.update(properties);
        }
    }

    void deleteConfig(File artifact) throws Exception {
        Configuration config = findExistingConfiguration(toConfigKey(artifact));
        if (Objects.nonNull(config)) {
            config.delete();
            LOGGER.info("Configuration for {} found and deleted", artifact.getName());
        } else {
            LOGGER.info("Configuration for {} not found, unable to delete", artifact.getName());
        }
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (event.getType() == ConfigurationEvent.CM_DELETED) {
            String fileName = pidToFile.remove(event.getPid());            
            File file = fileName != null ? new File(URI.create(fileName)) : null;
            if (file != null && file.isFile()) {
                if (file.delete()) {
                    LOGGER.info("Deleted json cfg file {} (pid={})",file, event.getPid());
                }
                else {
                    LOGGER.error("Unable to delete file: {}",file);
                }                
            }
            
        } else if (event.getType() == ConfigurationEvent.CM_UPDATED) {
            try {
                Configuration configuration = configurationAdmin.getConfiguration(event.getPid(), null);
                Dictionary<String, Object> dictionary = configuration.getProcessedProperties(null);
                final Object fileInstallFname = dictionary.get(DirectoryWatcher.FILENAME);
                File file = null;
                if (dictionary.get(DirectoryWatcher.FILENAME) != null) {
                    file = getCfgFileFromProperty(fileInstallFname);
                }
                if (file != null && canHandle(file)) {
                    LOGGER.debug("Monitoring json cfg {} (file={})", configuration.getPid(), file.getAbsolutePath());
                    pidToFile.put(configuration.getPid(), toConfigKey(file));                   
                    dictionary.remove(DirectoryWatcher.FILENAME);
                    dictionary.remove(Constants.SERVICE_PID);
                    dictionary.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
                    Configurations.buildWriter().build(new FileWriter(file)).writeConfiguration(dictionary);
                }
            } catch (Exception e) {
                LOGGER.warn("Can't update json configuration file", e);
            }
        }
    }

    private File getCfgFileFromProperty(Object val) throws URISyntaxException, MalformedURLException {
        if (val instanceof URL) {
            return new File(((URL) val).toURI());
        }
        if (val instanceof URI) {
            return new File((URI) val);
        }
        if (val instanceof String) {
            return new File(new URL((String) val).toURI());
        }
        return null;
    }

    String toConfigKey(File f) {
        return f.getAbsoluteFile().toURI().toString();
    }

    Configuration getConfiguration(String configKey, ConfigurationPID configurationPID) throws Exception {
        Configuration oldConfiguration = findExistingConfiguration(configKey);
        Configuration cachedConfiguration = oldConfiguration != null ?
                configurationAdmin.getConfiguration(oldConfiguration.getPid(), null) : null;
        if (cachedConfiguration != null) {
            return cachedConfiguration;
        } else {
            final Configuration newConfiguration;
            if (configurationPID.isFactory()) {
                if (configurationPID.isR7()) {
                    newConfiguration = configurationAdmin.getFactoryConfiguration(configurationPID.getFactoryPid(), configurationPID.getName(), "?");
                } else {
                    newConfiguration = configurationAdmin.createFactoryConfiguration(configurationPID.getFactoryPid(), "?");
                }
            } else {
                newConfiguration = configurationAdmin.getConfiguration(configurationPID.getPid(), "?");
            }
            return newConfiguration;
        }
    }

    Configuration findExistingConfiguration(String configKey) throws Exception {
        String filter = "(" + DirectoryWatcher.FILENAME + "=" + escapeFilterValue(configKey) + ")";
        Configuration[] configurations = configurationAdmin.listConfigurations(filter);
        if (configurations != null && configurations.length > 0) {
            return configurations[0];
        } else {
            return null;
        }
    }

    private String escapeFilterValue(String s) {
        return s.replaceAll("[(]", "\\\\(").
                replaceAll("[)]", "\\\\)").
                replaceAll("[=]", "\\\\=").
                replaceAll("[\\*]", "\\\\*");
    }

}
