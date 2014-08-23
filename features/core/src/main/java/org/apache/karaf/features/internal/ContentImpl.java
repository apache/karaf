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
import org.apache.karaf.features.ConfigInfo;
import org.apache.karaf.features.Feature;

public class ContentImpl {

    private List<Feature> dependencies = new ArrayList<Feature>();
    private List<BundleInfo> bundles = new ArrayList<BundleInfo>();
	private List<ConfigInfo> configs = new ArrayList<ConfigInfo>();
    private List<ConfigFileInfo> configurationFiles = new ArrayList<ConfigFileInfo>();

    public List<Feature> getDependencies() {
        return dependencies;
    }

    public List<BundleInfo> getBundles() {
        return bundles;
    }

	public List<ConfigInfo> getConfigurations() {
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

	public void addConfig(ConfigInfo configInfo) {
		configs.add(configInfo);
    }

    public void addConfigurationFile(ConfigFileInfo configurationFileInfo) {
        configurationFiles.add(configurationFileInfo);
    }

}
