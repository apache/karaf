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
package org.apache.karaf.features.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.ConfigInfo;
import org.apache.karaf.features.Feature;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureConfigInstaller {
	private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesServiceImpl.class);
    private static final String CONFIG_KEY = "org.apache.karaf.features.configKey";

    private final ConfigurationAdmin configAdmin;
    
    public FeatureConfigInstaller(ConfigurationAdmin configAdmin) {
		this.configAdmin = configAdmin;
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

    void installFeatureConfigs(Feature feature, boolean verbose) throws IOException, InvalidSyntaxException {
//        for (String config : feature.getConfigurations().keySet()) {
//            Dictionary<String,String> props = new Hashtable<String, String>(feature.getConfigurations().get(config));
//            String[] pid = parsePid(config);
//            Configuration cfg = findExistingConfiguration(configAdmin, pid[0], pid[1]);
//            if (cfg == null) {
//                cfg = createConfiguration(configAdmin, pid[0], pid[1]);
//                String key = createConfigurationKey(pid[0], pid[1]);
//                props.put(CONFIG_KEY, key);
//                if (cfg.getBundleLocation() != null) {
//                    cfg.setBundleLocation(null);
//                }
//                cfg.update(props);
//            }
//        }
    	for (ConfigInfo config : feature.getConfigurations()) {
    		String name = config.getName();
			Properties props = config.getProperties();

			// interpolation(props);
            

			String[] pid = parsePid(config.getName());
			Configuration cfg = findExistingConfiguration(configAdmin, pid[0],
					pid[1]);
			if (cfg == null) {
				Dictionary<String, String> cfgProps = convertToDict(props);

				cfg = createConfiguration(configAdmin, pid[0], pid[1]);
				String key = createConfigurationKey(pid[0], pid[1]);
				cfgProps.put(CONFIG_KEY, key);
				cfg.update(cfgProps);
			} else if (config.isAppend()) {
				Dictionary<String,Object> properties = cfg.getProperties();
				for (Enumeration<String> propKeys = properties.keys(); propKeys
						.hasMoreElements();) {
					String key = propKeys.nextElement();
					// remove existing entry, since it's about appending.
					if (props.containsKey(key)) {
						props.remove(key);
					} 
				}
				if (props.size() > 0) {
					// convert props to dictionary
					Dictionary<String, String> cfgProps = convertToDict(props);
					cfg.update(cfgProps);
				}
			}
		}
        for (ConfigFileInfo configFile : feature.getConfigurationFiles()) {
            installConfigurationFile(configFile.getLocation(), configFile.getFinalname(), configFile.isOverride(), verbose);
        }
    }

	private Dictionary<String, String> convertToDict(Properties props) {
		Dictionary<String, String> cfgProps = new Hashtable<String, String>();
		for (@SuppressWarnings("rawtypes")
		Enumeration e = props.propertyNames(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			String val = props.getProperty(key);
			cfgProps.put(key, val);
		}
		return cfgProps;
	}

    private String createConfigurationKey(String pid, String factoryPid) {
        return factoryPid == null ? pid : pid + "-" + factoryPid;
    }

    private void installConfigurationFile(String fileLocation, String finalname, boolean override, boolean verbose) throws IOException {
    	LOGGER.debug("Checking configuration file " + fileLocation);
        if (verbose) {
            System.out.println("Checking configuration file " + fileLocation);
        }
    	
    	String basePath = System.getProperty("karaf.base");
    	
    	if (finalname.indexOf("${") != -1) {
    		//remove any placeholder or variable part, this is not valid.
    		int marker = finalname.indexOf("}");
    		finalname = finalname.substring(marker+1);
    	}
    	
    	finalname = basePath + File.separator + finalname;
    	
    	File file = new File(finalname); 
    	if (file.exists() && !override) {
    		LOGGER.debug("configFile already exist, don't override it");
    		return;
    	}

        InputStream is = null;
        FileOutputStream fop = null;
        try {
            is = new BufferedInputStream(new URL(fileLocation).openStream());

            if (!file.exists()) {
                File parentFile = file.getParentFile();
                if (parentFile != null)
                    parentFile.mkdirs();
                file.createNewFile();
            }

            fop = new FileOutputStream(file);
        
            int bytesRead;
            byte[] buffer = new byte[1024];
            
            while ((bytesRead = is.read(buffer)) != -1) {
                fop.write(buffer, 0, bytesRead);
            }
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage());
            throw e;
        } catch (MalformedURLException e) {
        	LOGGER.error(e.getMessage());
            throw e;
		} finally {
			if (is != null)
				is.close();
            if (fop != null) {
			    fop.flush();
			    fop.close();
            }
		}
            
    }
    
}
