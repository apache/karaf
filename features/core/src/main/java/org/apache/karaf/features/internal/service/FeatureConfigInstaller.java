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
package org.apache.karaf.features.internal.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.ConfigInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.util.StreamUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureConfigInstaller {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesServiceImpl.class);
    private static final String CONFIG_KEY = "org.apache.karaf.features.configKey";
    private static final String FILEINSTALL_FILE_NAME = "felix.fileinstall.filename";

    private final ConfigurationAdmin configAdmin;
    private File storage;
    private boolean configCfgStore;

    public FeatureConfigInstaller(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
        this.storage = new File(System.getProperty("karaf.etc"));
        this.configCfgStore = FeaturesServiceImpl.DEFAULT_CONFIG_CFG_STORE;
    }

    public FeatureConfigInstaller(ConfigurationAdmin configAdmin, boolean configCfgStore) {
        this.configAdmin = configAdmin;
        this.storage = new File(System.getProperty("karaf.etc"));
        this.configCfgStore = configCfgStore;
    }

    private ConfigId parsePid(String pid) {
        int n = pid.indexOf('-');
        ConfigId cid = new ConfigId();
        cid.fullPid = pid;
        if (n > 0) {
            cid.factoryPid = pid.substring(n + 1);
            cid.pid = pid.substring(0, n);
        } else {
            cid.pid = pid;
        }
        return cid;
    }

    private Configuration createConfiguration(ConfigurationAdmin configurationAdmin, String pid,
                                              String factoryPid)
        throws IOException, InvalidSyntaxException {
        if (factoryPid != null) {
            return configurationAdmin.createFactoryConfiguration(pid, null);
        } else {
            return configurationAdmin.getConfiguration(pid, null);
        }
    }

    private Configuration findExistingConfiguration(ConfigurationAdmin configurationAdmin, ConfigId cid)
        throws IOException, InvalidSyntaxException {
        String filter;
        if (cid.factoryPid == null) {
            filter = "(" + Constants.SERVICE_PID + "=" + cid.pid + ")";
        } else {
            filter = "(" + CONFIG_KEY + "=" + cid.fullPid + ")";
        }
        Configuration[] configurations = configurationAdmin.listConfigurations(filter);
        return (configurations != null && configurations.length > 0) ? configurations[0] : null;
    }

    public void installFeatureConfigs(Feature feature) throws IOException, InvalidSyntaxException {
        for (ConfigInfo config : feature.getConfigurations()) {
            TypedProperties props = new TypedProperties();
            // trim lines
            String val = config.getValue();
            if (config.isExternal()) {
                try {
                    props.load(new URL(val));
                } catch (java.net.MalformedURLException e) {
                    throw new IOException("Failed to load config info from URL [" + val + "] for feature [" + feature.getName() + "/" + feature.getVersion() + "].", e);
                }
            } else {
                props.load(new StringReader(val));
            }
            ConfigId cid = parsePid(config.getName());
            Configuration cfg = findExistingConfiguration(configAdmin, cid);
            if (cfg == null || config.isOverride()) {

                File cfgFile = null;
                if (storage != null) {
                    cfgFile = new File(storage, cid.fullPid + ".cfg");
                }
                if (!cfgFile.exists() || config.isOverride()) {
                    Dictionary<String, Object> cfgProps = convertToDict(props);
                    cfg = createConfiguration(configAdmin, cid.pid, cid.factoryPid);
                    cfgProps.put(CONFIG_KEY, cid.fullPid);
                    props.put(CONFIG_KEY, cid.fullPid);
                    if (storage != null && configCfgStore) {
                        cfgProps.put(FILEINSTALL_FILE_NAME, cfgFile.getAbsoluteFile().toURI().toString());
                    }
                    cfg.update(cfgProps);
                    try {
                        updateStorage(cid, props, false);
                    } catch (Exception e) {
                        LOGGER.warn("Can't update cfg file", e);
                    }
                } else {
                    LOGGER.info("Skipping configuration {} - file already exists", cfgFile);
                }
            } else if (config.isAppend()) {
                boolean update = false;
                Dictionary<String, Object> properties = cfg.getProperties();
                for (String key : props.keySet()) {
                    if (properties.get(key) == null) {
                        properties.put(key, props.get(key));
                        update = true;
                    }
                }
                if (update) {
                    cfg.update(properties);
                    try {
                        updateStorage(cid, props, true);
                    } catch (Exception e) {
                        LOGGER.warn("Can't update cfg file", e);
                    }
                }
            }
        }
        for (ConfigFileInfo configFile : feature.getConfigurationFiles()) {
            installConfigurationFile(configFile.getLocation(), configFile.getFinalname(),
                                     configFile.isOverride());
        }
    }

    public void uninstallFeatureConfigs(Feature feature) throws IOException, InvalidSyntaxException {
        if (feature != null) {
            if (feature.getConfigurations() != null) {
                for (ConfigInfo configInfo : feature.getConfigurations()) {
                    ConfigId configId = parsePid(configInfo.getName());
                    Configuration configuration = findExistingConfiguration(configAdmin, configId);
                    if (configuration != null) {
                        configuration.delete();
                    }
                    File cfgFile = null;
                    if (storage != null) {
                        cfgFile = new File(storage, configId.fullPid + ".cfg");
                    }
                    if (cfgFile.exists()) {
                        cfgFile.delete();
                    }
                }
            }
            if (feature.getConfigurationFiles() != null) {
                for (ConfigFileInfo configFileInfo : feature.getConfigurationFiles()) {
                    String finalname = substFinalName(configFileInfo.getFinalname());
                    File cfgFile = new File(finalname);
                    if (cfgFile.exists()) {
                        cfgFile.delete();
                    }
                }
            }
        }
    }

    private Dictionary<String, Object> convertToDict(Map<String, Object> props) {
        Dictionary<String, Object> cfgProps = new Hashtable<>();
        for (Map.Entry<String, Object> e : props.entrySet()) {
            cfgProps.put(e.getKey(), e.getValue());
        }
        return cfgProps;
    }

    /**
     * Substitute variables in the final name and append prefix if necessary.
     *
     * <ol>
     * <li>If the final name does not start with '${' it is prefixed with
     * karaf.base (+ file separator).</li>
     * <li>It substitute also all variables (scheme ${...}) with the respective
     * configuration values and system properties.</li>
     * <li>All unknown variables kept unchanged.</li>
     * <li>If the substituted string starts with an variable that could not be
     * substituted, it will be prefixed with karaf.base (+ file separator), too.
     * </li>
     * </ol>
     * 
     * @param finalname
     *            The final name that should be processed.
     * @return the location in the file system that should be accesses.
     */
    protected static String substFinalName(String finalname) {
        final String markerVarBeg = "${";
        final String markerVarEnd = "}";

        boolean startsWithVariable = finalname.startsWith(markerVarBeg) && finalname.contains(markerVarEnd);

        // Substitute all variables, but keep unknown ones.
        final String dummyKey = "";
        try {
            finalname = InterpolationHelper.substVars(finalname, dummyKey, null, null, null, true, true,
                                                      false);
        } catch (final IllegalArgumentException ex) {
            LOGGER.info("Substitution failed. Skip substitution of variables of configuration final name ({}).",
                    finalname);
        }

        // Prefix with karaf base if the initial final name does not start with
        // a variable or the first variable was not substituted.
        if (!startsWithVariable || finalname.startsWith(markerVarBeg)) {
            final String basePath = System.getProperty("karaf.base");
            finalname = basePath + File.separator + finalname;
        }

        // Remove all unknown variables.
        while (finalname.contains(markerVarBeg) && finalname.contains(markerVarEnd)) {
            int beg = finalname.indexOf(markerVarBeg);
            int end = finalname.indexOf(markerVarEnd);
            final String rem = finalname.substring(beg, end + markerVarEnd.length());
            finalname = finalname.replace(rem, "");
        }

        return finalname;
    }

    private void installConfigurationFile(String fileLocation, String finalname, boolean override)
        throws IOException {
        finalname = substFinalName(finalname);

        File file = new File(finalname);
        if (file.exists()) {
            if (!override) {
                LOGGER.debug("Configuration file {} already exist, don't override it", finalname);
                return;
            } else {
                LOGGER.info("Configuration file {} already exist, overriding it", finalname);
            }
        } else {
            LOGGER.info("Creating configuration file {}", finalname);
        }

        // TODO: use download manager to download the configuration
        try (
                InputStream is = new BufferedInputStream(new URL(fileLocation).openStream())
        ) {
            if (!file.exists()) {
                File parentFile = file.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                }
                file.createNewFile();
            }
            try (
                    FileOutputStream fop = new FileOutputStream(file)
            ) {
                StreamUtils.copy(is, fop);
            }
        } catch (RuntimeException | MalformedURLException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    protected void updateStorage(ConfigId cid, TypedProperties props, boolean append)
        throws Exception {
        if (storage != null && configCfgStore) {
            File cfgFile = getConfigFile(cid);
            if (!cfgFile.exists()) {
                props.save(cfgFile);
            } else {
                updateExistingConfig(props, append, cfgFile);
            }
        }
    }

    private File getConfigFile(ConfigId cid) throws IOException, InvalidSyntaxException {
        Configuration cfg = findExistingConfiguration(configAdmin, cid);
        // update the cfg file depending of the configuration
        File cfgFile = new File(storage, cid.fullPid + ".cfg");
        if (cfg != null && cfg.getProperties() != null) {
            Object val = cfg.getProperties().get(FILEINSTALL_FILE_NAME);
            try {
                if (val instanceof URL) {
                    cfgFile = new File(((URL)val).toURI());
                }
                if (val instanceof URI) {
                    cfgFile = new File((URI)val);
                }
                if (val instanceof String) {
                    cfgFile = new File(new URL((String)val).toURI());
                }
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        LOGGER.trace("Update {}", cfgFile.getName());
        return cfgFile;
    }

    private void updateExistingConfig(TypedProperties props, boolean append, File cfgFile) throws IOException {
        TypedProperties properties = new TypedProperties();
        properties.load(cfgFile);
        for (String key : props.keySet()) {
            if (!isInternalKey(key)) {
                List<String> comments = props.getComments(key);
                Object value = props.get(key);
                if (!properties.containsKey(key)) {
                    properties.put(key, comments, value);
                } else if (!append) {
                    if (comments.isEmpty()) {
                        comments = properties.getComments(key);
                    }
                    properties.put(key, comments, value);
                }
            }
        }
        if (!append) {
            // remove "removed" properties from the cfg file
            ArrayList<String> propertiesToRemove = new ArrayList<>();
            for (String key : properties.keySet()) {
                if (!props.containsKey(key) && !isInternalKey(key)) {
                    propertiesToRemove.add(key);
                }
            }
            for (String key : propertiesToRemove) {
                properties.remove(key);
            }
        }
        storage.mkdirs();
        properties.save(cfgFile);
    }

    private boolean isInternalKey(String key) {
        return Constants.SERVICE_PID.equals(key)
            || ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
            || FILEINSTALL_FILE_NAME.equals(key);
    }

    class ConfigId {
        String fullPid;
        String pid;
        String factoryPid;
    }
}
