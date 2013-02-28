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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Feature;

/**
 * A feature
 */
public class FeatureImpl implements Feature {

    private String id;
    private String name;
    private String description;
    private String details;
    private String version;
    private String resolver;
    private String install = DEFAULT_INSTALL_MODE;
    private List<Feature> dependencies = new ArrayList<Feature>();
    private List<BundleInfo> bundles = new ArrayList<BundleInfo>();
    private Map<String, Map<String,String>> configs = new HashMap<String, Map<String,String>>();
    private List<ConfigFileInfo> configurationFiles = new ArrayList<ConfigFileInfo>();
    public static String SPLIT_FOR_NAME_AND_VERSION = "_split_for_name_and_version_";
    public static String DEFAULT_VERSION = "0.0.0";
    public static String VERSION_PREFIX = "version=";

    public FeatureImpl() {
    }

    public FeatureImpl(String name) {
        this(name, DEFAULT_VERSION);
    }
    
    public FeatureImpl(String name, String version) {
    	this.name = name;
    	this.version = version;
        this.id = name + "-" + version;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getVersion() {
		return version;
	}
    
	public void setVersion(String version) {
		this.version = version;
	}

    public String getResolver() {
        return resolver;
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public String getInstall() {
        return install;
    }

    public void setInstall(String install) {
        this.install = install;
    }

    public List<Feature> getDependencies() {
        return dependencies;
    }

    public List<BundleInfo> getBundles() {
        return bundles;
    }

    public Map<String, Map<String, String>> getConfigurations() {
        return configs;
    }
    
	public List<ConfigFileInfo> getConfigurationFiles() {
		return configurationFiles;
	}
    public void addDependency(Feature dependency) {
        dependencies.add(dependency);
    }

    public void addBundle(BundleInfo bundle) {
        bundles.add(bundle);
    }

    public void addConfig(String name, Map<String,String> properties) {
        configs.put(name, properties);
    }
    
    public void addConfigurationFile(ConfigFileInfo configurationFileInfo) {
    	configurationFiles.add(configurationFileInfo);
    }

    public String toString() {
    	String ret = getName() + SPLIT_FOR_NAME_AND_VERSION + getVersion();
    	return ret;
    }
    
    public static Feature valueOf(String str) {
    	if (str.indexOf(SPLIT_FOR_NAME_AND_VERSION) >= 0) {
    		String strName = str.substring(0, str.indexOf(SPLIT_FOR_NAME_AND_VERSION));
        	String strVersion = str.substring(str.indexOf(SPLIT_FOR_NAME_AND_VERSION) 
        			+ SPLIT_FOR_NAME_AND_VERSION.length(), str.length());
        	return new FeatureImpl(strName, strVersion);
    	} else {
    		return new FeatureImpl(str);
    	}
    			
    	
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeatureImpl feature = (FeatureImpl) o;

        if (!name.equals(feature.name)) return false;
        if (!version.equals(feature.version)) return false;

        return true;
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}
