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
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import org.apache.karaf.features.FeaturesNamespaces;
import org.apache.karaf.features.internal.model.Feature;

@XmlType(name = "featureReplacements", propOrder = {
        "replacements"
})
public class FeatureReplacements {

    @XmlElement(name = "replacement")
    private List<OverrideFeature> replacements = new LinkedList<>();

    public List<OverrideFeature> getReplacements() {
        return replacements;
    }

    @XmlType(name = "featureOverrideMode")
    @XmlEnum
    public enum FeatureOverrideMode {
        @XmlEnumValue("replace")
        REPLACE,
        @XmlEnumValue("merge")
        MERGE,
        @XmlEnumValue("remove")
        REMOVE
    }

    @XmlType(name = "overrideFeature", propOrder = {
            "feature"
    })
    public static class OverrideFeature {
        @XmlAttribute
        private FeatureOverrideMode mode = FeatureOverrideMode.REPLACE;
        @XmlElement
        private Feature feature;

        public FeatureOverrideMode getMode() {
            return mode;
        }

        public void setMode(FeatureOverrideMode mode) {
            this.mode = mode;
        }

        public Feature getFeature() {
            return feature;
        }

        public void setFeature(Feature feature) {
            this.feature = feature;
        }
    }

}
