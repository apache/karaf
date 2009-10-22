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
package org.apache.felix.karaf.admin;

import java.util.List;

public class InstanceSettings {
    private final int port;
    private final String location;
    private final List<String> featureURLs;
    private final List<String> features;

    public InstanceSettings(int port, String location, List<String> featureURLs, List<String> features) {
        this.port = port;
        this.location = location;
        this.featureURLs = featureURLs;
        this.features = features;
    }

    public int getPort() {
        return port;
    }

    public String getLocation() {
        return location;
    }

    public List<String> getFeatureURLs() {
        return featureURLs;
    }

    public List<String> getFeatures() {
        return features;
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
        return is.port == port &&
               (location == null ? is.location == null : location.equals(is.location)) &&
               (featureURLs == null ? is.featureURLs == null : featureURLs.equals(is.featureURLs)) &&
               (features == null ? is.features == null : features.equals(is.features));
    }

    @Override
    public int hashCode() {
        int rc = 17;
        rc = 37 * port;
        if (location != null) {
            rc = 37 * location.hashCode();
        }
        if (featureURLs != null) {
            rc = 37 * featureURLs.hashCode();
        }
        if (features != null) {
            rc = 37 * features.hashCode();
        }
        return rc;
    }
    
    
}
