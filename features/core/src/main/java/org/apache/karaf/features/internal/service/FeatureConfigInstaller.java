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
import org.apache.felix.utils.properties.InterpolationHelper.SubstitutionCallback;
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

    private String[] parsePid(String pid) {
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]{pid, factoryPid};
        } else {
            return new String[]{pid, null};
        }
    }

    private Configuration createConfiguration(ConfigurationAdmin configurationAdmin,
                                              String pid, String factoryPid) throws IOException, InvalidSyntaxException {
        if (factoryPid != null) {
            return configurationAdmin.createFactoryConfiguration(pid, null);
        } else {
            return configurationAdmin.getConfiguration(pid, null);
        }
    }

    private Configuration findExistingConfiguration(ConfigurationAdmin configurationAdmin,
                                                    String pid, String factoryPid) throws IOException, InvalidSyntaxException {
        String filter;
        if (factoryPid == null) {
            filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
        } else {
            String key = createConfigurationKey(pid, factoryPid);
            filter = "(" + CONFIG_KEY + "=" + key + ")";
        }
        Configuration[] configurations = configurationAdmin.listConfigurations(filter);
        if (configurations != null && configurations.length > 0) {
            return configurations[0];
        }
        return null;
    }

    public void installFeatureConfigs(Feature feature) throws IOException, InvalidSyntaxException {
    	for (ConfigInfo config : feature.getConfigurations()) {
            TypedProperties props = new TypedProperties();
            // trim lines
            String val = config.getValue().replaceAll("\n\\s+", "\n");
            props.load(new StringReader(val));
			String[] pid = parsePid(config.getName());
			Configuration cfg = findExistingConfiguration(configAdmin, pid[0], pid[1]);
			if (cfg == null) {
				Dictionary<String, Object> cfgProps = convertToDict(props);
				cfg = createConfiguration(configAdmin, pid[0], pid[1]);
				String key = createConfigurationKey(pid[0], pid[1]);
				cfgProps.put(CONFIG_KEY, key);
				cfg.update(cfgProps);
                try {
                    updateStorage(pid[0], pid[1], props, false);
                } catch (Exception e) {
                    LOGGER.warn("Can't update cfg file", e);
                }
			} else if (config.isAppend()) {
                boolean update = false;
				Dictionary<String,Object> properties = cfg.getProperties();
                for (String key : props.keySet()) {
                    if (properties.get(key) == null) {
                        properties.put(key, props.get(key));
                        update = true;
                    }
                }
                if (update) {
                    cfg.update(properties);
                    try {
                        updateStorage(pid[0], pid[1], props, true);
                    } catch (Exception e) {
                        LOGGER.warn("Can't update cfg file", e);
                    }
                }
			}
		}
        for (ConfigFileInfo configFile : feature.getConfigurationFiles()) {
            installConfigurationFile(configFile.getLocation(), configFile.getFinalname(), configFile.isOverride());
        }
    }

	private Dictionary<String, Object> convertToDict(Map<String, Object> props) {
		Dictionary<String, Object> cfgProps = new Hashtable<>();
        for (Map.Entry<String, Object> e : props.entrySet()) {
            cfgProps.put(e.getKey(), e.getValue());
        }
		return cfgProps;
	}

    private String createConfigurationKey(String pid, String factoryPid) {
        return factoryPid == null ? pid : pid + "-" + factoryPid;
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
            finalname = InterpolationHelper.substVars(finalname, dummyKey, null, null, (SubstitutionCallback) null,
                    true, true, false);
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

    private void installConfigurationFile(String fileLocation, String finalname, boolean override) throws IOException {
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

    protected void updateStorage(String pid, String factoryPid, TypedProperties props, boolean append) throws Exception {
        if (storage != null && configCfgStore) {
            // get the cfg file
            File cfgFile;
            if (factoryPid != null) {
                cfgFile = new File(storage, pid + "-" + factoryPid + ".cfg");
            } else {
                cfgFile = new File(storage, pid + ".cfg");
            }
            Configuration cfg = findExistingConfiguration(configAdmin, factoryPid, pid);
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
                    throw new IOException(e.getMessage(), e);
                }
            }
            LOGGER.trace("Update {}", cfgFile.getName());
            // update the cfg file
            if (!cfgFile.exists()) {
                props.save(cfgFile);
            } else {
                TypedProperties properties = new TypedProperties();
                properties.load( cfgFile );
                for (String key : props.keySet()) {
                    if (!Constants.SERVICE_PID.equals(key)
                            && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                            && !FILEINSTALL_FILE_NAME.equals(key)) {
                        List<String> comments = props.getComments(key);
                        List<String> value = props.getRaw(key);
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
                        if (!props.containsKey(key)
                                && !Constants.SERVICE_PID.equals(key)
                                && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                                && !FILEINSTALL_FILE_NAME.equals(key)) {
                            propertiesToRemove.add(key);
                        }
                    }
                    for (String key : propertiesToRemove) {
                        properties.remove(key);
                    }
                }
                // save the cfg file
                storage.mkdirs();
                properties.save( cfgFile );
            }
        }
    }

}
