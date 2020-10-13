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
import java.util.Dictionary;
import java.util.Hashtable;

public class JsonConfigInstaller implements ArtifactInstaller, ConfigurationListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(JsonConfigInstaller.class);

    private ConfigurationAdmin configurationAdmin;

    public JsonConfigInstaller(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
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
        String name = artifact.getName();
        String pid[] = parsePid(name);
        Configuration configuration = getConfiguration(toConfigKey(artifact), pid[0], pid[1]);
        Dictionary<String, Object> props = configuration.getProperties();
        Hashtable<String, Object> old = props != null ? new Hashtable<>(new DictionaryAsMap<>(props)) : null;
        Hashtable<String, Object> properties = Configurations.buildReader().build(new FileReader(artifact)).readConfiguration();
        if (old != null) {
            old.remove(DirectoryWatcher.FILENAME);
            old.remove(Constants.SERVICE_PID);
            old.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
        }
        if (!properties.equals(old)) {
            properties.put(DirectoryWatcher.FILENAME, toConfigKey(artifact));
            if (old == null) {
                LOGGER.info("Creating configuration from " + pid[0] + (pid[1] == null ? "" : "-" + pid[1]) + ".json");
            } else {
                LOGGER.info("Updating configuration from " + pid[0] + (pid[1] == null ? "" : "-" + pid[1]) + ".json");
            }
            configuration.update(properties);
        }
    }

    boolean deleteConfig(File f) throws Exception {
        String pid[] = parsePid(f.getName());
        LOGGER.info("Deleting configuration from " + pid[0] + (pid[1] == null ? "" : "-" + pid[1]) + ".json");
        Configuration config = getConfiguration(toConfigKey(f), pid[0], pid[1]);
        config.delete();
        return true;
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (event.getType() == ConfigurationEvent.CM_DELETED) {
            File file = new File(System.getProperty("karaf.etc"), event.getPid() + ".json");
            if (file.exists()) {
                file.delete();
            }
        } else if (event.getType() == ConfigurationEvent.CM_UPDATED) {
            try {
                Configuration configuration = configurationAdmin.getConfiguration(event.getPid(), null);
                Dictionary<String, Object> dictionary = configuration.getProcessedProperties(null);
                File file;
                if (dictionary.get(DirectoryWatcher.FILENAME) != null) {
                    file = getCfgFileFromProperty(configuration.getProperties().get(DirectoryWatcher.FILENAME));
                } else {
                    file = new File(System.getProperty("karaf.etc"), event.getPid() + ".json");
                }
                if (canHandle(file)) {
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

    String[] parsePid(String path) {
        String pid = path.substring(0, path.lastIndexOf('.'));
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]{pid, factoryPid};
        } else {
            return new String[]{pid, null};
        }
    }

    String toConfigKey(File f) {
        return f.getAbsoluteFile().toURI().toString();
    }

    Configuration getConfiguration(String fileName, String pid, String factoryPid) throws Exception {
        Configuration oldConfiguration = findExistingConfiguration(fileName);
        Configuration cachedConfiguration = oldConfiguration != null ?
                configurationAdmin.getConfiguration(oldConfiguration.getPid(), null) : null;
        if (cachedConfiguration != null) {
            return cachedConfiguration;
        } else {
            Configuration newConfiguration;
            if (factoryPid != null) {
                newConfiguration = configurationAdmin.createFactoryConfiguration(pid, "?");
            } else {
                newConfiguration = configurationAdmin.getConfiguration(pid, "?");
            }
            return newConfiguration;
        }
    }

    Configuration findExistingConfiguration(String fileName) throws Exception {
        String filter = "(" + DirectoryWatcher.FILENAME + "=" + escapeFilterValue(fileName) + ")";
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
