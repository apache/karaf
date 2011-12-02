/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.karaf.features.internal.model;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.ConfigFileInfo;


/**
 * 
 * Definition of the Feature.
 *             
 * 
 * <p>Java class for feature complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="feature">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="details" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="config" type="{http://karaf.apache.org/xmlns/features/v1.0.0}config" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="configfile" type="{http://karaf.apache.org/xmlns/features/v1.0.0}configFile" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="feature" type="{http://karaf.apache.org/xmlns/features/v1.0.0}dependency" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="bundle" type="{http://karaf.apache.org/xmlns/features/v1.0.0}bundle" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://karaf.apache.org/xmlns/features/v1.0.0}featureName" />
 *       &lt;attribute name="version" type="{http://www.w3.org/2001/XMLSchema}string" default="0.0.0" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="resolver" type="{http://karaf.apache.org/xmlns/features/v1.0.0}resolver" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "feature", propOrder = {
    "details",
    "config",
    "configfile",
    "feature",
    "bundle"
})
public class Feature implements org.apache.karaf.features.Feature {
    public static String SPLIT_FOR_NAME_AND_VERSION = "_split_for_name_and_version_";
    public static String DEFAULT_VERSION = "0.0.0";


    protected String details;
    protected List<Config> config;
    protected List<ConfigFile> configfile;
    protected List<Dependency> feature;
    protected List<Bundle> bundle;
    @XmlAttribute(required = true)
    protected String name;
    @XmlAttribute
    protected String version;
    @XmlAttribute
    protected String description;
    @XmlAttribute
    protected String resolver;
    @XmlAttribute
    protected String install;
    @XmlAttribute(name = "start-level")
    protected Integer startLevel;
    @XmlAttribute
    protected String region;

    public Feature() {
    }

    public Feature(String name) {
        this.name = name;
    }

    public Feature(String name, String version) {
        this.name = name;
        this.version = version;
    }


    public static org.apache.karaf.features.Feature valueOf(String str) {
    	if (str.indexOf(SPLIT_FOR_NAME_AND_VERSION) >= 0) {
    		String strName = str.substring(0, str.indexOf(SPLIT_FOR_NAME_AND_VERSION));
        	String strVersion = str.substring(str.indexOf(SPLIT_FOR_NAME_AND_VERSION)
        			+ SPLIT_FOR_NAME_AND_VERSION.length(), str.length());
        	return new Feature(strName, strVersion);
    	} else {
    		return new Feature(str);
    	}


    }

    /**
     * Gets the value of the config property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the config property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConfig().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Config }
     * 
     * 
     */
    public List<Config> getConfig() {
        if (config == null) {
            config = new ArrayList<Config>();
        }
        return this.config;
    }

    /**
     * Gets the value of the configfile property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the configfile property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConfigfile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ConfigFile }
     * 
     * 
     */
    public List<ConfigFile> getConfigfile() {
        if (configfile == null) {
            configfile = new ArrayList<ConfigFile>();
        }
        return this.configfile;
    }

    /**
     * Gets the value of the feature property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the feature property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFeature().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Dependency }
     * 
     * 
     */
    public List<Dependency> getFeature() {
        if (feature == null) {
            feature = new ArrayList<Dependency>();
        }
        return this.feature;
    }

    /**
     * Gets the value of the bundle property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the bundle property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBundle().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Bundle }
     * 
     * 
     */
    public List<Bundle> getBundle() {
        if (bundle == null) {
            bundle = new ArrayList<Bundle>();
        }
        return this.bundle;
    }

    public String getId() {
        return getName() + "-" + getVersion();
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        if (version == null) {
            return "0.0.0";
        } else {
            return version;
        }
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Gets the value of the resolver property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResolver() {
        return resolver;
    }

    public String getInstall() {
        return install;
    }

    public void setInstall(String install) {
        this.install = install;
    }

    public List<org.apache.karaf.features.Dependency> getDependencies() {
        return Collections.<org.apache.karaf.features.Dependency>unmodifiableList(getFeature());
    }

    public List<BundleInfo> getBundles() {
        return Collections.<BundleInfo>unmodifiableList(getBundle());
    }

    public Map<String, Map<String, String>> getConfigurations() {
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        for (Config config: getConfig()) {
            String name = config.getName();
            StringReader propStream = new StringReader(config.getValue());
            Properties props = new Properties();
            try {
                props.load(propStream);
            } catch (IOException e) {
                //ignore??
            }
            interpolation(props);
            Map<String, String> propMap = new HashMap<String, String>();
            for (Map.Entry<Object, Object> entry: props.entrySet()) {
                propMap.put((String)entry.getKey(), (String)entry.getValue());
            }
            result.put(name, propMap);
        }
        return result;
    }

    public List<ConfigFileInfo> getConfigurationFiles() {
        return Collections.<ConfigFileInfo>unmodifiableList(getConfigfile());
    }

    /**
     * Sets the value of the resolver property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResolver(String value) {
        this.resolver = value;
    }
    
    /**
     * Gets the value of the startLevel property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getStartLevel() {
        return startLevel == null? 0: startLevel;
    }

    /**
     * Sets the value of the startLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setStartLevel(Integer value) {
        this.startLevel = value;
    }


    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String toString() {
    	String ret = getName() + SPLIT_FOR_NAME_AND_VERSION + getVersion();
    	return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Feature feature = (Feature) o;

        if (name != null ? !name.equals(feature.name) : feature.name != null) return false;
        if (version != null ? !version.equals(feature.version) : feature.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    protected void interpolation(Properties properties) {
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String val = properties.getProperty(key);
            Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(val);
            while (matcher.find()) {
                String rep = System.getProperty(matcher.group(1));
                if (rep != null) {
                    val = val.replace(matcher.group(0), rep);
                    matcher.reset(val);
                }
            }
            properties.put(key, val);
        }
    }

}
