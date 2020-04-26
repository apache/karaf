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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.felix.cm.json.Configurations;
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

    private static final Pattern JSON_PATTERN = Pattern.compile("\\s*\\{[\\s\\S]*");

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

    private ConfigId parsePid(final String pid) {
        final ConfigId cid = new ConfigId();
        cid.pid = pid;
        final int n = pid.contains("~") ? pid.indexOf('~') : pid.indexOf('-');
        if (n > 0) {
            cid.isFactoryPid = true;
            cid.factoryPid = pid.substring(0, n);
            if (pid.contains("~")) {
                cid.name = pid.substring(n + 1);
            }
        }
        return cid;
    }

    private Configuration createConfiguration(ConfigurationAdmin configurationAdmin, ConfigId cid)
        throws IOException, InvalidSyntaxException {
        if (cid.isFactoryPid) {
            if (Objects.nonNull(cid.name)) {
                return configurationAdmin.getFactoryConfiguration(cid.factoryPid, cid.name, null);
            } else {
                return configurationAdmin.createFactoryConfiguration(cid.factoryPid, null);
            }
        } else {
            return configurationAdmin.getConfiguration(cid.pid, null);
        }
    }

    private Configuration findExistingConfiguration(ConfigurationAdmin configurationAdmin, ConfigId cid)
        throws IOException, InvalidSyntaxException {
        String filter;
        if (!cid.isFactoryPid) {
            filter = "(" + Constants.SERVICE_PID + "=" + cid.pid + ")";
        } else {
            filter = "(" + CONFIG_KEY + "=" + cid.pid + ")";
        }
        Configuration[] configurations = configurationAdmin.listConfigurations(filter);
        return (configurations != null && configurations.length > 0) ? configurations[0] : null;
    }

    public void installFeatureConfigs(Feature feature) throws IOException, InvalidSyntaxException {
        for (ConfigInfo config : feature.getConfigurations()) {
            final ConfigType configType;

            Dictionary<String, Object> cfgJson = null; // *.cfg.json
            TypedProperties props = null; // *.cfg and *.config

            // trim lines
            String val = config.getValue();
            if (config.isExternal()) {
                final URL url =  new URL(val);
                final byte[] bytes;
                try {
                    bytes = loadConfig(url);
                } catch (MalformedURLException e) {
                    throw new IOException("Failed to load configuration from URL [" + val + "] for feature [" + feature.getName() + "/" + feature.getVersion() + "].", e);
                }

                String contents = new String(bytes, StandardCharsets.UTF_8);
                if (JSON_PATTERN.matcher(contents).matches()) {
                    configType = ConfigType.JSON;
                    cfgJson = Configurations.buildReader().build(new StringReader(contents)).readConfiguration();
                } else {
                    configType = ConfigType.PROPERTIES;
                    contents = new String(bytes, StandardCharsets.ISO_8859_1);
                    props = new TypedProperties();
                    props.load(new StringReader(contents));
                }
            } else {
                if (JSON_PATTERN.matcher(val).matches()) {
                    configType = ConfigType.JSON;
                    cfgJson = Configurations.buildReader().build(new StringReader(val)).readConfiguration();
                } else {
                    configType = ConfigType.PROPERTIES;
                    props = new TypedProperties();
                    props.load(new StringReader(val));
                }
            }

            ConfigId cid = parsePid(config.getName());
            Configuration cfg = findExistingConfiguration(configAdmin, cid);
            if (cfg == null || config.isOverride()) {

                File cfgFile = null;
                if (storage != null) {
                    cfgFile = new File(storage, cid.pid + ".cfg");
                }
                if (!cfgFile.exists() || config.isOverride()) {
                    final Dictionary<String, Object> cfgProps = ConfigType.PROPERTIES.equals(configType) ? convertToDict(props) : cfgJson;
                    cfg = createConfiguration(configAdmin, cid);
                    cfgProps.put(CONFIG_KEY, cid.pid);
                    if (ConfigType.PROPERTIES.equals(configType)) {
                        props.put(CONFIG_KEY, cid.pid);
                    } else {
                        cfgJson.put(CONFIG_KEY, cid.pid);
                    }
                    if (storage != null && configCfgStore) {
                        cfgProps.put(FILEINSTALL_FILE_NAME, cfgFile.getAbsoluteFile().toURI().toString());
                    }
                    cfg.update(cfgProps);
                    try {
                        if (ConfigType.JSON.equals(configType)) {
                            props = toTypedProperties(cfgJson);
                        }
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
                final Enumeration<String> keySet= ConfigType.PROPERTIES.equals(configType) ? new Vector<>(props.keySet()).elements() : cfgJson.keys();
                while (keySet.hasMoreElements()) {
                    final String key = keySet.nextElement();
                    if (properties.get(key) == null) {
                        final Object value = ConfigType.PROPERTIES.equals(configType) ? props.get(key) : cfgJson.get(key);
                        properties.put(key, value);
                        update = true;
                    }
                }
                if (update) {
                    cfg.update(properties);
                    try {
                        if (ConfigType.JSON.equals(configType)) {
                            props = toTypedProperties(cfgJson);
                        }
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
                        cfgFile = new File(storage, configId.pid + ".cfg");
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
        File cfgFile = new File(storage, cid.pid + ".cfg");
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

    private byte[] loadConfig(final URL url) throws MalformedURLException, IOException {
        try (final InputStream inputStream = new BufferedInputStream(url.openStream())) {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            StreamUtils.copy(inputStream, outputStream);
            return outputStream.toByteArray();
        }
    }

    private TypedProperties toTypedProperties(final Dictionary<String, Object> cfgJson) {
        final TypedProperties typedProperties = new TypedProperties();
        final Enumeration<String> keys = cfgJson.keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            typedProperties.put(key, cfgJson.get(key));
        }
        return typedProperties;
    }

    private static final class ConfigId {
        boolean isFactoryPid;
        String pid;
        String factoryPid;
        String name;
    }

    private enum ConfigType {
        PROPERTIES,
        JSON
    }

}
