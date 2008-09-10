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
package org.apache.servicemix.gshell.features.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.servicemix.gshell.features.Feature;

/**
 * A feature
 */
public class FeatureImpl implements Feature {

    private String name;
    private String version;
    private List<String> dependencies = new ArrayList<String>();
    private List<String> bundles = new ArrayList<String>();
    private Map<String, Map<String,String>> configs = new HashMap<String, Map<String,String>>();
    
    public FeatureImpl(String name) {
        this.name = name;
        this.version = "0.0.0";
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

    public List<String> getBundles() {
        return bundles;
    }

    public Map<String, Map<String, String>> getConfigurations() {
        return configs;
    }

    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }

    public void addBundle(String bundle) {
        bundles.add(bundle);
    }

    public void addConfig(String name, Map<String,String> properties) {
        configs.put(name, properties);
    }

}
