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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.cm.json.io.Configurations;
import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.config.core.ConfigRepository;
import org.apache.karaf.util.PathUtils;
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

    @Override
    public void update(String pid, Map<String, Object> properties) throws IOException {
        update(pid, properties, "cfg");
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#update(java.lang.String, java.util.Dictionary, boolean)
     */
    @Override
    public void update(String pid, Map<String, Object> properties, String suffix) throws IOException {
        try {
            LOGGER.trace("Updating configuration {}", pid);
            Configuration cfg = configAdmin.getConfiguration(pid, "?");
            Dictionary<String, Object> dict = cfg.getProcessedProperties(null);
            TypedProperties props = new TypedProperties();
            File file = getCfgFileFromProperties(dict);
            if (file != null) {
                props = load(file);
                props.putAll(properties);
                props.keySet().retainAll(properties.keySet());
                store(props, file);
                props.clear();
                props = load(file);
                props.put(FILEINSTALL_FILE_NAME, file.toURI().toString());
            } else {
                if (properties.containsKey(FILEINSTALL_FILE_NAME)) {
                    file = getCfgFileFromProperty(properties.get(FILEINSTALL_FILE_NAME));
                }
                if (file == null) {
                    file = generateConfigFilename(cfg, suffix);
                }
                checkConfigFile(cfg.getPid(), cfg.getFactoryPid(), file);
                props.putAll(properties);
                props.keySet().retainAll(properties.keySet());
                store(props, file);
                props.put(FILEINSTALL_FILE_NAME, file.toURI().toString());
            }
            cfg.update(new Hashtable<>(props));
        } catch (URISyntaxException e) {
            throw new IOException("Error updating config", e);
        }
    }
    
    private static File generateConfigFilename(Configuration cfg, String suffix) {
        final String pid = cfg.getPid();
        final String factoryPid = cfg.getFactoryPid();
        String fName;
        if(factoryPid!=null) {
            //pid = <factoryPid>.<identifier>
            String identifier = pid.substring(factoryPid.length()+1);
            fName = cfg.getFactoryPid() + "-"+identifier+ "."  + suffix;
        }
        else {
            fName = pid + "."  + suffix;
        }
        return new File(System.getProperty("karaf.etc"), fName);
    }

    /**
     * Ensure that a configuration is only written to its own file.
     *
     * <p>The target file is derived from caller-controlled input: a {@code felix.fileinstall.filename}
     * entry in the property map, the storage type (suffix), the PID or the factory alias. The file has to
     * be located inside {@code ${karaf.etc}}, use an extension the configuration installers load
     * ({@code .cfg}, {@code .config} or the JSON one), and be named after the PID (or, for a factory
     * configuration, after the factory PID), so that it can only hold this configuration.</p>
     *
     * <p>Staying inside {@code ${karaf.etc}} is not enough: the files reserved to more privileged users
     * live there too. Without the name check, a user allowed to update an ordinary configuration could
     * overwrite {@code etc/users.properties}, or the file of a PID they are not allowed to edit, such as
     * the command and JMX ACLs.</p>
     *
     * @param pid the PID of the configuration, or {@code null} for a factory configuration not created yet.
     * @param factoryPid the factory PID, or {@code null} if this is not a factory configuration.
     * @param file the file the configuration is about to be written to.
     */
    static void checkConfigFile(String pid, String factoryPid, File file) throws IOException {
        PathUtils.checkWithin(new File(System.getProperty("karaf.etc")), file);
        // resolve symbolic links and ".." segments, and the actual case on case-insensitive file systems
        String name = configName(file.getCanonicalFile().getName());
        boolean ownFile = name != null
                && (name.equals(pid)
                    || (factoryPid != null && name.length() > factoryPid.length() + 1 && name.startsWith(factoryPid + "-")));
        if (!ownFile) {
            throw new IOException("For security reasons, configuration "
                    + (pid != null ? pid : factoryPid) + " can't be stored in " + file
                    + ": the file has to be named after the " + (pid != null ? "PID" : "factory PID")
                    + " and use one of the " + configExtensions() + " extensions");
        }
    }

    /**
     * Ensure that a storage type (suffix) is one of the configuration file extensions.
     */
    private static void checkSuffix(String suffix) throws IOException {
        if (!configExtensions().contains(suffix)) {
            throw new IOException("Unsupported configuration type " + suffix
                    + ", it has to be one of " + configExtensions());
        }
    }

    /**
     * The file extensions loaded as configurations: the JSON one first, as it can contain the others.
     */
    private static List<String> configExtensions() {
        return Arrays.asList(JsonConfigInstaller.getExtension(), "cfg", "config");
    }

    /**
     * Return the configuration name (PID, or factory PID and alias) of a file name, or {@code null}
     * if the file is not a configuration file.
     */
    private static String configName(String fileName) {
        for (String extension : configExtensions()) {
            String ending = "." + extension;
            if (fileName.endsWith(ending) && fileName.length() > ending.length()) {
                return fileName.substring(0, fileName.length() - ending.length());
            }
        }
        return null;
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

    @Override
    public boolean exists(String pid) throws Exception {
        Configuration[] configurations = configAdmin.listConfigurations("(service.pid=" + pid + ")");
        if (configurations == null || configurations.length == 0) {
            return false;
        }
        return true;
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
                Dictionary<String, Object> props = configuration.getProcessedProperties(null);
                if (props != null) {
                    File file;
                    try {
                        file = getCfgFileFromProperties(props);
                    } catch (URISyntaxException e) {
                        throw new IOException(e);
                    }
                    if (file != null && file.exists()) {
                        tp = load(file);

                        Object fileInstallFileName = props.get(FILEINSTALL_FILE_NAME);
                        if (fileInstallFileName != null && !"".equals(fileInstallFileName)) {
                            tp.put(FILEINSTALL_FILE_NAME, fileInstallFileName);
                        }

                    } else {
                        for (Enumeration<String> e = props.keys(); e.hasMoreElements();) {
                            String key = e.nextElement();
                            Object val = props.get(key);
                            tp.put(key, val);
                        }
                        tp.remove(Constants.SERVICE_PID);
                        tp.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
                    }
                }
                return tp;
            }
        }
        return null;
    }

    @Override
    public String createFactoryConfiguration(String factoryPid, Map<String, Object> properties) throws IOException {
        return createFactoryConfiguration(factoryPid, null, properties, "cfg");
    }

    @Override
    public String createFactoryConfiguration(String factoryPid, Map<String, Object> properties, String suffix) throws IOException {
        return createFactoryConfiguration(factoryPid, null, properties, suffix);
    }

    @Override
    public String createFactoryConfiguration(String factoryPid, String alias, Map<String, Object> properties) throws IOException {
        return createFactoryConfiguration(factoryPid, alias, properties, "cfg");
    }

    @Override
    public String createFactoryConfiguration(String factoryPid, String alias, Map<String, Object> properties, String suffix) throws IOException {
        checkSuffix(suffix);
        File file = null;
        if (alias != null && !"".equals(alias.trim())) {
            file = new File(new File(System.getProperty("karaf.etc")), factoryPid + "-" + alias + "." + suffix);
            checkConfigFile(null, factoryPid, file);
        }
        Configuration config = configAdmin.createFactoryConfiguration(factoryPid, "?");
        TypedProperties props = new TypedProperties();
        if (file == null) {
            file = Files.createTempFile(new File(System.getProperty("karaf.etc")).toPath(), factoryPid + "-", "." + suffix).toFile();
        }
        props.putAll(properties);
        store(props, file);
        props.put(FILEINSTALL_FILE_NAME, file.toURI().toString());
        config.update(new Hashtable<>(props));
        return config.getPid();
    }

    @Override
    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    static TypedProperties load(File file) throws IOException {
        TypedProperties props = new TypedProperties();
        if (file.toURI().toString().endsWith(".json")) {
            Hashtable<String, Object> configuration = Configurations.buildReader().build(new FileReader(file)).readConfiguration();
            for (String key : configuration.keySet()) {
                props.put(key, configuration.get(key));
            }
        } else {
            props.load(file);
        }
        return props;
    }

    void store(TypedProperties properties, File file) throws IOException {
        if (file.toURI().toString().endsWith(".json")) {
            Configurations.buildWriter().build(new FileWriter(file)).writeConfiguration(new Hashtable<>(properties));
        } else {
            properties.save(file);
        }
    }

}
