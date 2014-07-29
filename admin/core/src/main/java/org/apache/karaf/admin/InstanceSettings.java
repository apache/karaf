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
package org.apache.karaf.admin;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstanceSettings implements Serializable {
    private final int sshPort;
    private final int rmiRegistryPort;
    private final int rmiServerPort;
    private final String location;
    private final String javaOpts;
    private final List<String> featureURLs;
    private final List<String> features;
    private final Map<String, URL> textResources;
    private final Map<String, URL> binaryResources;
    private final String address;

    public InstanceSettings(int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, List<String> featureURLs, List<String> features) {
       this(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, featureURLs, features, new HashMap<String, URL>(), new HashMap<String, URL>());
    }

    public InstanceSettings(int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, List<String> featureURLs, List<String> features, Map<String, URL> textResources, Map<String, URL> binaryResources) {
        this(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, featureURLs, features, textResources, binaryResources, "0.0.0.0");
    }
    
    public InstanceSettings(int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, List<String> featureURLs, List<String> features, Map<String, URL> textResources, Map<String, URL> binaryResources, String address) {
        this.sshPort = sshPort;
        this.rmiRegistryPort = rmiRegistryPort;
        this.rmiServerPort = rmiServerPort;
        this.location = location;
        this.javaOpts = javaOpts;
        this.featureURLs = featureURLs != null ? featureURLs : new ArrayList<String>();
        this.features = features != null ? features : new ArrayList<String>();
        this.textResources = textResources != null ? textResources : new HashMap<String, URL>();
        this.binaryResources = binaryResources != null ? binaryResources : new HashMap<String, URL>();
        this.address = address;
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
               (location == null ? is.location == null : location.equals(is.location)) &&
               (javaOpts == null ? is.javaOpts == null : javaOpts.equals(is.javaOpts)) &&
               (featureURLs == null ? is.featureURLs == null : featureURLs.equals(is.featureURLs)) &&
               (features == null ? is.features == null : features.equals(is.features)) &&
               (address == null ? is.address == null : address.equals(is.address));
    }

    @Override
    public int hashCode() {
        int result = sshPort + rmiRegistryPort + rmiServerPort;
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (javaOpts != null ? javaOpts.hashCode() : 0);
        result = 31 * result + (featureURLs != null ? featureURLs.hashCode() : 0);
        result = 31 * result + (features != null ? features.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }
}
