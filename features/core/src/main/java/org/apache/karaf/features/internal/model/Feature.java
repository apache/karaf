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
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.felix.utils.version.VersionCleaner;


/**
 * <p>Definition of the Feature.</p>
 * <p>Java class for feature complex type.</p>
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>
 * &lt;complexType name="feature"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="details" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="config" type="{http://karaf.apache.org/xmlns/features/v1.0.0}config" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="configfile" type="{http://karaf.apache.org/xmlns/features/v1.0.0}configFile" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="feature" type="{http://karaf.apache.org/xmlns/features/v1.0.0}dependency" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="bundle" type="{http://karaf.apache.org/xmlns/features/v1.0.0}bundle" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="conditional" type="{http://karaf.apache.org/xmlns/features/v1.0.0}conditional" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="capability" type="{http://karaf.apache.org/xmlns/features/v1.0.0}capability" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="requirement" type="{http://karaf.apache.org/xmlns/features/v1.0.0}requirement" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="name" use="required" type="{http://karaf.apache.org/xmlns/features/v1.0.0}featureName" /&gt;
 *       &lt;attribute name="version" type="{http://www.w3.org/2001/XMLSchema}string" default="0.0.0" /&gt;
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="resolver" type="{http://karaf.apache.org/xmlns/features/v1.0.0}resolver" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "feature", propOrder = {
        "details",
        "config",
        "configfile",
        "feature",
        "bundle",
        "conditional",
        "capability",
        "requirement",
        "library",
        "scoping"
        })
public class Feature extends Content implements org.apache.karaf.features.Feature {

    public static final char VERSION_SEPARATOR = '/';
    public static final String DEFAULT_VERSION = "0.0.0";

    @XmlElement(name = "details", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected String details;
    @XmlAttribute(required = true)
    protected String name;
    @XmlTransient
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
    protected Boolean hidden;
    @XmlElement(name = "conditional", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<Conditional> conditional;
    @XmlElement(name = "capability", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<Capability> capability;
    @XmlElement(name = "requirement", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<Requirement> requirement;
    @XmlElement(name = "scoping", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected Scoping scoping;
    @XmlElement(name = "library", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<Library> library;
    @XmlTransient
    protected String namespace;
    @XmlTransient
    protected List<String> resourceRepositories;
    @XmlTransient
    protected String repositoryUrl;
    @XmlTransient
    private boolean blacklisted;

    public Feature() {
    }

    public Feature(String name) {
        this.name = name;
    }

    public Feature(String name, String version) {
        this.name = name;
        this.version = VersionCleaner.clean(version);
    }

    public static org.apache.karaf.features.Feature valueOf(String str) {
        int idx = str.indexOf(VERSION_SEPARATOR);
        if (idx >= 0) {
            String strName = str.substring(0, idx);
            String strVersion = str.substring(idx + 1, str.length());
            return new Feature(strName, strVersion);
        } else {
            return new Feature(str);
        }


    }

    public String getId() {
        return getName() + VERSION_SEPARATOR + getVersion();
    }

    /**
     * Get the value of the name property.
     *
     * @return possible object is {@link String}
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of the name property.
     *
     * @param value allowed object is {@link String}
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Get the value of the version property.
     *
     * @return possible object is {@link String}
     */
    public String getVersion() {
        if (version == null) {
            return DEFAULT_VERSION;
        } else {
            return version;
        }
    }

    /**
     * Set the value of the version property.
     *
     * @param value allowed object is {@link String}
     */
    @XmlAttribute
    public void setVersion(String value) {
        this.version = VersionCleaner.clean(value);
    }

    /**
     * Since version has a default value ("0.0.0"), returns
     * whether or not the version has been set.
     *
     * @return true if the feature has a version, false else.
     */
    public boolean hasVersion() {
        return this.version != null;
    }

    /**
     * Get the value of the description property.
     *
     * @return possible object is {@link String}.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the value of the description property.
     *
     * @param value allowed object is {@link String}.
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Get the feature details.
     *
     * @return the feature details.
     */
    public String getDetails() {
        return details;
    }

    /**
     * Set the feature details.
     *
     * @param details the feature details.
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Get the value of the resolver property.
     *
     * @return possible object is {@link String}.
     */
    public String getResolver() {
        return resolver;
    }

    /**
     * Get the feature install flag.
     *
     * @return the feature install flags.
     */
    public String getInstall() {
        return install;
    }

    /**
     * Set the feature install flag.
     *
     * @param install the feature install flag.
     */
    public void setInstall(String install) {
        this.install = install;
    }

    /**
     * Set the value of the resolver property.
     *
     * @param value allowed object is {@link String}.
     */
    public void setResolver(String value) {
        this.resolver = value;
    }

    /**
     * Get the value of the startLevel property.
     *
     * @return possible object is {@link Integer}.
     */
    public int getStartLevel() {
        return startLevel == null ? 0 : startLevel;
    }

    /**
     * Set the value of the startLevel property.
     *
     * @param value allowed object is {@link Integer}.
     */
    public void setStartLevel(Integer value) {
        this.startLevel = value;
    }

    /**
     * Get the value of the hidden property.
     *
     * @return true if the feature is hidden, false else.
     */
    public boolean isHidden() {
        return hidden == null ? false : hidden;
    }

    /**
     * Set the value of the hidden property.
     *
     * @param value true to set the feature as hidden, false else.
     */
    public void setHidden(Boolean value) {
        this.hidden = value;
    }

    /**
     * Get the value of the conditional property.
     *
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the feature property.
     *
     * For example, to add a new item, do as follows:
     *
     * <pre>
     *    getConditionals().add(newItem);
     * </pre>
     *
     * Objects of the following type(s) are allowed in the list
     * {@link Conditional}.
     *
     * @return the list of feature conditions.
     */
    public List<Conditional> getConditional() {
        if (conditional == null) {
            conditional = new ArrayList<>();
        }
        return this.conditional;
    }

    /**
     * Get the feature capabilities.
     *
     * @return the feature capabilities as a {@link List}..
     */
    public List<Capability> getCapabilities() {
        if (capability == null) {
            capability = new ArrayList<>();
        }
        return this.capability;
    }

    /**
     * Get the feature requirements.
     *
     * @return the feature requirements as a {@link List}.
     */
    public List<Requirement> getRequirements() {
        if (requirement == null) {
            requirement = new ArrayList<>();
        }
        return this.requirement;
    }

    /**
     * Get the feature scoping.
     *
     * @return the feature scoping.
     */
    public Scoping getScoping() {
        return scoping;
    }

    /**
     * Set the feature scoping.
     *
     * @param scoping the feature scoping.
     */
    public void setScoping(Scoping scoping) {
        this.scoping = scoping;
    }

    /**
     * Return a string representation of the feature.
     *
     * @return the feature string representation.
     */
    public String toString() {
        return getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Feature feature = (Feature) o;
        if (name != null ? !name.equals(feature.name) : feature.name != null) {
            return false;
        }
        return version != null ? version.equals(feature.version) : feature.version == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    @SuppressWarnings("rawtypes")
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

    public List<Library> getLibraries() {
        if (library == null) {
            library = new ArrayList<>();
        }
        return this.library;
    }

    public void postUnmarshall() {
        if (conditional != null) {
            for (Conditional c : conditional) {
                c.setOwner(this);
            }
        }
        if (config != null) {
            for (Config c : config) {
                String v = c.getValue();
                v = Stream.of(v.split("\n"))
                        .map(String::trim)
                        .collect(Collectors.joining("\n", "", "\n"));
                c.setValue(v);
            }
        }
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public List<String> getResourceRepositories() {
        return resourceRepositories != null
                ? resourceRepositories
                : Collections.emptyList();
    }

    public void setResourceRepositories(List<String> resourceRepositories) {
        this.resourceRepositories = resourceRepositories;
    }

    @Override
    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    @Override
    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

}
