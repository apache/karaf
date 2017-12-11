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
package org.apache.karaf.features.internal.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.karaf.features.Feature;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditional", propOrder = {
        "condition",
        "config",
        "configfile",
        "feature",
        "bundle"
        })
public class Conditional extends Content implements org.apache.karaf.features.Conditional {

    // TODO: use type that really reflects <xs:element name="condition" type="tns:dependency" /> ?
    // i.e., org.apache.karaf.features.internal.model.Dependency
    @XmlElement(name = "condition", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<String> condition;

    @XmlTransient
    protected Feature owner;

    public Feature getOwner() {
        return owner;
    }

    public void setOwner(Feature owner) {
        this.owner = owner;
    }

    public List<String> getCondition() {
        if (condition == null) {
            this.condition = new ArrayList<>();
        }
        return condition;
    }

    @Override
    public Feature asFeature() {
        if (owner == null) {
            throw new IllegalStateException("No owner set for conditional");
        }
        String name = owner.getName();
        String version = owner.getVersion();
        String conditionName = name + "-condition-" + getConditionId().replaceAll("[^A-Za-z0-9 ]", "_");
        org.apache.karaf.features.internal.model.Feature f = new org.apache.karaf.features.internal.model.Feature(conditionName, version);
        f.getBundle().addAll(getBundle());
        f.getConfig().addAll(getConfig());
        f.getConfigfile().addAll(getConfigfile());
        f.getFeature().addAll(getFeature());
        return f;
    }

    private String getConditionId() {
        StringBuffer sb = new StringBuffer();
        for (String cond : getCondition()) {
            if (sb.length() > 0) {
                sb.append("_");
            }
            sb.append(cond);
        }
        return sb.toString();
    }
}
