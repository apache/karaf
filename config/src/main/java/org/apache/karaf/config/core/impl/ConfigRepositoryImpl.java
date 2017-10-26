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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.utils.properties.TypedProperties;
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

    public ConfigRepositoryImpl(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#update(java.lang.String, java.util.Dictionary, boolean)
     */
    @Override
    public void update(String pid, Map<String, Object> properties) throws IOException {
        try {
            LOGGER.trace("Updating configuration {}", pid);
            Configuration cfg = configAdmin.getConfiguration(pid, "?");
            Dictionary<String, Object> dict = cfg.getProperties();
            TypedProperties props = new TypedProperties();
            File file = getCfgFileFromProperties(dict);
            if (file != null) {
                props.load(file);
                props.putAll(properties);
                props.keySet().retainAll(properties.keySet());
                props.save(file);
                props.clear();
                props.load(file);
                props.put(FILEINSTALL_FILE_NAME, file.toURI().toString());
            } else {
                file = new File(System.getProperty("karaf.etc"), pid + ".cfg");
                props.putAll(properties);
                props.keySet().retainAll(properties.keySet());
                props.save(file);
                props.put(FILEINSTALL_FILE_NAME, file.toURI().toString());
            }
            cfg.update(new Hashtable<>(props));
        } catch (URISyntaxException e) {
            throw new IOException("Error updating config", e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#delete(java.lang.String)
     */
    @Override
    public void delete(String pid) throws Exception {
        LOGGER.trace("Deleting configuration {}", pid);
        Configuration configuration = configAdmin.getConfiguration(pid, null);
        configuration.delete();
    }

    private File getCfgFileFromProperties(Dictionary<String, Object> properties) throws URISyntaxException, MalformedURLException {
        if (properties != null) {
            Object val = properties.get(FILEINSTALL_FILE_NAME);
            return getCfgFileFromProperty(val);
        }
        return null;
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

    @Override
    public TypedProperties getConfig(String pid) throws IOException, InvalidSyntaxException {
        if (pid != null && configAdmin != null) {
            Configuration configuration = configAdmin.getConfiguration(pid, null);
            if (configuration != null) {
                TypedProperties tp = new TypedProperties();
                Dictionary<String, Object> props = configuration.getProperties();
                if (props != null) {
                    File file;
                    try {
                        file = getCfgFileFromProperties(props);
                    } catch (URISyntaxException e) {
                        throw new IOException(e);
                    }
                    if (file != null) {
                        tp.load(file);
                    } else {
                        for (Enumeration<String> e = props.keys(); e.hasMoreElements();) {
                            String key = e.nextElement();
                            Object val = props.get(key);
                            tp.put(key, val);
                        }
                        tp.remove( Constants.SERVICE_PID );
                        tp.remove( ConfigurationAdmin.SERVICE_FACTORYPID );
                    }
                }
                return tp;
            }
        }
        return null;
    }

    @Override
    public String createFactoryConfiguration(String factoryPid, Map<String, Object> properties) throws IOException {
        return createFactoryConfiguration(factoryPid, null, properties);
    }

    @Override
    public String createFactoryConfiguration(String factoryPid, String alias, Map<String, Object> properties) throws IOException {
        Configuration config = configAdmin.createFactoryConfiguration(factoryPid, "?");
        TypedProperties props = new TypedProperties();
        File file = File.createTempFile(factoryPid + "-", ".cfg", new File(System.getProperty("karaf.etc")));
        props.putAll(properties);
        props.save(file);
        props.put(FILEINSTALL_FILE_NAME, file.toURI().toString());
        config.update(new Hashtable<>(props));
        return config.getPid();
    }

    @Override
    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

}
