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
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.ConfigInfo;

@XmlTransient
public class Content {
    
    @XmlElement(name = "config", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<Config> config;
    @XmlElement(name = "configfile", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<ConfigFile> configfile;
    @XmlElement(name = "feature", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<Dependency> feature;
    @XmlElement(name = "bundle", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<Bundle> bundle;

    /**
     * <p>Gets the value of the config property.</p>
     *
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the config property.</p>
     *
     * <p>For example, to add a new item, do as follows:</p>
     * <pre>
     *    getConfig().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list
     * {@link Config }</p>
     *
     * @return the list of config elements in a feature.
     */
    public List<Config> getConfig() {
        if (config == null) {
            config = new ArrayList<>();
        }
        return this.config;
    }

    /**
     * <p>Gets the value of the configfile property.</p>
     *
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the configfile property.</p>
     *
     * <p>For example, to add a new item, do as follows:</p>
     * <pre>
     *    getConfigfile().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list
     * {@link ConfigFile }</p>
     *
     * @return the list of configfile elements in a feature.
     */
    public List<ConfigFile> getConfigfile() {
        if (configfile == null) {
            configfile = new ArrayList<>();
        }
        return this.configfile;
    }

    /**
     * <p>Gets the value of the feature property.</p>
     *
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the feature property.</p>
     *
     * <p>For example, to add a new item, do as follows:</p>
     * <pre>
     *    getFeatures().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list
     * {@link Dependency }</p>
     *
     * @return the list of dependency features in a feature.
     */
    public List<Dependency> getFeature() {
        if (feature == null) {
            feature = new ArrayList<>();
        }
        return this.feature;
    }

    /**
     * <p>Gets the value of the bundle property.</p>
     *
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the bundle property.</p>
     *
     * <p>For example, to add a new item, do as follows:</p>
     * <pre>
     *    getBundle().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list
     * {@link Bundle }</p>
     *
     * @return the list of bundle elements in a feature.
     */
    public List<Bundle> getBundle() {
        if (bundle == null) {
            bundle = new ArrayList<>();
        }
        return this.bundle;
    }

    public List<org.apache.karaf.features.Dependency> getDependencies() {
        return Collections.unmodifiableList(getFeature());
    }

    public List<BundleInfo> getBundles() {
        return Collections.unmodifiableList(getBundle());
    }

    public List<ConfigInfo> getConfigurations() {
    	return Collections.unmodifiableList(getConfig());
    }

    public List<ConfigFileInfo> getConfigurationFiles() {
        return Collections.unmodifiableList(getConfigfile());
    }

}
