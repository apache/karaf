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
package org.apache.karaf.instance.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InstanceSettings {

    private final int sshPort;
    private final int rmiRegistryPort;
    private final int rmiServerPort;
    private final String location;
    private final String javaOpts;
    private final List<String> featureURLs;
    private final List<String> features;
    private final String address;
    private final Map<String, URL> textResources;
    private final Map<String, URL> binaryResources;
    private final List<String> profiles;

    public InstanceSettings(int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, List<String> featureURLs, List<String> features) {
        this(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, featureURLs, features, "0.0.0.0");
    }

    public InstanceSettings(int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, List<String> featureURLs, List<String> features, String address) {
        this(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, featureURLs, features, address, new HashMap<>(), new HashMap<>());
    }

    public InstanceSettings(int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, List<String> featureURLs, List<String> features, String address, Map<String, URL> textResources, Map<String, URL> binaryResources) {
        this(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, featureURLs, features, address, textResources, binaryResources, null);
    }

    public InstanceSettings(int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, List<String> featureURLs, List<String> features, String address, Map<String, URL> textResources, Map<String, URL> binaryResources, List<String> profiles) {
        this.sshPort = sshPort;
        this.rmiRegistryPort = rmiRegistryPort;
        this.rmiServerPort = rmiServerPort;
        this.location = location;
        this.javaOpts = javaOpts;
        this.featureURLs = featureURLs != null ? featureURLs : new ArrayList<>();
        this.features = features != null ? features : new ArrayList<>();
        this.address = address;
        this.textResources = textResources != null ? textResources : new HashMap<>();
        this.binaryResources = binaryResources != null ? binaryResources : new HashMap<>();
        this.profiles = profiles != null ? profiles : new ArrayList<>();
    }


    public int getSshPort() {
        return sshPort;
    }

    public int getRmiRegistryPort() {
        return rmiRegistryPort;
    }

    public int getRmiServerPort() {
        return rmiServerPort;
    }

    public String getLocation() {
        return location;
    }

    public String getJavaOpts() {
        return javaOpts;
    }

    public List<String> getFeatureURLs() {
        return Collections.unmodifiableList(featureURLs);
    }

    public List<String> getFeatures() {
        return Collections.unmodifiableList(features);
    }

    public Map<String, URL> getTextResources() {
        return Collections.unmodifiableMap(textResources);
    }

    public Map<String, URL> getBinaryResources() {
        return Collections.unmodifiableMap(binaryResources);
    }

    public String getAddress() {
        return this.address;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof InstanceSettings)) {
            return false;
        }
        InstanceSettings is = (InstanceSettings) o;
        return is.sshPort == sshPort &&
               is.rmiRegistryPort == rmiRegistryPort &&
               is.rmiServerPort == rmiServerPort &&
               Objects.equals(location, is.location) &&
               Objects.equals(javaOpts, is.javaOpts) &&
               Objects.equals(featureURLs, is.featureURLs) &&
               Objects.equals(features, is.features) &&
               Objects.equals(address, is.address) &&
               Objects.equals(profiles, is.profiles);
    }

    @Override
    public int hashCode() {
        int result = sshPort + rmiRegistryPort + rmiServerPort;
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (javaOpts != null ? javaOpts.hashCode() : 0);
        result = 31 * result + (featureURLs != null ? featureURLs.hashCode() : 0);
        result = 31 * result + (features != null ? features.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (profiles != null ? profiles.hashCode() : 0);
        return result;
    }

}
