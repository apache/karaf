/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.features.internal.model.processing;

import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "overrideBundleDependency", propOrder = {
        "repositories",
        "features",
        "bundles"
})
public class OverrideBundleDependency {

    @XmlElement(name = "repository")
    private List<OverrideDependency> repositories = new LinkedList<>();
    @XmlElement(name = "feature")
    private List<OverrideFeatureDependency> features = new LinkedList<>();
    @XmlElement(name = "bundle")
    private List<OverrideDependency> bundles = new LinkedList<>();

    public List<OverrideDependency> getRepositories() {
        return repositories;
    }

    public List<OverrideFeatureDependency> getFeatures() {
        return features;
    }

    public List<OverrideDependency> getBundles() {
        return bundles;
    }

    @XmlType(name = "overrideDependency")
    public static class OverrideDependency {
        @XmlAttribute
        private String uri;
        @XmlAttribute
        private boolean dependency = false;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public boolean isDependency() {
            return dependency;
        }

        public void setDependency(boolean dependency) {
            this.dependency = dependency;
        }
    }

    @XmlType(name = "overrideFeatureDependency")
    public static class OverrideFeatureDependency {
        @XmlAttribute
        private String name;
        @XmlAttribute
        private String version;
        @XmlAttribute
        private boolean dependency = false;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public boolean isDependency() {
            return dependency;
        }

        public void setDependency(boolean dependency) {
            this.dependency = dependency;
        }
    }

}
