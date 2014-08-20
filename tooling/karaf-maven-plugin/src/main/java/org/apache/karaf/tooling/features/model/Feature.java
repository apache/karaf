/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.features.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Feature {

    private String name;
    private String version;
    private List<String> dependencies = new ArrayList<String>();
    private List<BundleRef> bundles = new ArrayList<BundleRef>();
	private List<ConfigRef> configs = new ArrayList<ConfigRef>();
    private List<ArtifactRef> configFiles = new ArrayList<ArtifactRef>();

    public Feature(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public List<BundleRef> getBundles() {
        return bundles;
    }

	public List<ConfigRef> getConfigurations() {
		return configs;
    }

    public List<ArtifactRef> getConfigFiles() {
        return configFiles;
    }

    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }

    public void addBundle(BundleRef bundle) {
        bundles.add(bundle);
    }

	public void addConfig(ConfigRef config) {
		configs.add(config);
    }

    public void addConfigFile(ArtifactRef configFile) {
        configFiles.add(configFile);
    }
}