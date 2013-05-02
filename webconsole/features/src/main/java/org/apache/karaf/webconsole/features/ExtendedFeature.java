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
package org.apache.karaf.webconsole.features;

import java.util.List;
import java.util.Map;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;

public class ExtendedFeature implements Feature {

    public enum State {
        INSTALLED, UNINSTALLED;

        @Override
        public String toString() {
            //only capitalize the first letter
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    protected final State state;
    protected final String repository;
    protected final Feature feature;

    public ExtendedFeature(State state, String repository, Feature feature) {
        this.state = state;
        this.repository = repository;
        this.feature = feature;
    }

    @Override
    public List<BundleInfo> getBundles() {
        return this.feature.getBundles();
    }

    @Override
    public Map<String, Map<String, String>> getConfigurations() {
        return this.feature.getConfigurations();
    }

    @Override
    public List<ConfigFileInfo> getConfigurationFiles() {
        return this.feature.getConfigurationFiles();
    }

    @Override
    public List<? extends Conditional> getConditional() {
        return this.feature.getConditional();
    }

    @Override
    public List<Dependency> getDependencies() {
        return this.feature.getDependencies();
    }

    @Override
    public String getId() {
        return this.feature.getId();
    }

    @Override
    public String getName() {
        return this.feature.getName();
    }

    @Override
    public String getVersion() {
        return this.feature.getVersion();
    }

    @Override
    public String getResolver() {
        return this.feature.getResolver();
    }

    @Override
    public String getDescription() {
        return this.feature.getDescription();
    }

    @Override
    public String getDetails() {
        return this.feature.getDetails();
    }

    public String getRepository() {
        return this.repository;
    }

    @Override
    public String getInstall() {
        return feature.getInstall();
    }

    public State getState() {
        return this.state;
    }

    @Override
    public int getStartLevel() {
        return 0;
    }

    @Override
    public boolean hasVersion() {
        return this.feature.hasVersion();
    }

    @Override
    public String getRegion() {
        return feature.getRegion();
    }

}
