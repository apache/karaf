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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.karaf.features.BundleInfo;

/**
 * <p>Deployable element to install.</p>
 *
 * <p>Java class for bundle complex type.</p>
 *
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 *
 * <pre>
 * &lt;complexType name="bundle"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;anyURI"&gt;
 *       &lt;attribute name="start-level" type="{http://www.w3.org/2001/XMLSchema}int" /&gt;
 *       &lt;attribute name="start" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="dependency" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "bundle", propOrder = {
        "value"
        })
public class Bundle implements BundleInfo {

    @XmlValue
    @XmlSchemaType(name = "anyURI")
    protected String value;
    /** Original value may be queried if {@link #isOverriden()} is <code>true</code> */
    @XmlTransient
    protected String originalValue;
    @XmlAttribute(name = "start-level")
    protected Integer startLevel;
    @XmlAttribute
    protected Boolean start; // = true;
    @XmlAttribute
    protected Boolean dependency;
    @XmlTransient
    private boolean blacklisted = false;
    @XmlTransient
    private BundleInfo.BundleOverrideMode overriden = BundleInfo.BundleOverrideMode.NONE;

    public Bundle() {
    }

    public Bundle(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the value property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getLocation() {
        return value.trim();
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLocation(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the startLevel property.
     *
     * @return possible object is
     * {@link Integer }
     */
    public int getStartLevel() {
        return startLevel == null ? 0 : startLevel;
    }

    /**
     * Sets the value of the startLevel property.
     *
     * @param value allowed object is
     *              {@link Integer }
     */
    public void setStartLevel(Integer value) {
        this.startLevel = value;
    }

    /**
     * Gets the value of the start property.
     *
     * @return possible object is
     * {@link Boolean }
     */
    public boolean isStart() {
        return start == null ? true : start;
    }

    /**
     * Sets the value of the start property.
     *
     * @param value allowed object is
     *              {@link Boolean }
     */
    public void setStart(Boolean value) {
        this.start = value;
    }

    /**
     * Gets the value of the dependency property.
     *
     * @return possible object is
     * {@link Boolean }
     */
    public boolean isDependency() {
        return dependency == null ? false : dependency;
    }

    /**
     * Sets the value of the dependency property.
     *
     * @param value allowed object is
     *              {@link Boolean }
     */
    public void setDependency(Boolean value) {
        this.dependency = value;
    }

    @Override
    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    @Override
    public BundleInfo.BundleOverrideMode isOverriden() {
        return overriden;
    }

    public void setOverriden(BundleInfo.BundleOverrideMode overriden) {
        this.overriden = overriden;
    }

    @Override
    public String getOriginalLocation() {
        return originalValue;
    }

    public void setOriginalLocation(String originalLocation) {
        this.originalValue = originalLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Bundle bundle = (Bundle) o;
        if (dependency != bundle.dependency) {
            return false;
        }
        if (start != bundle.start) {
            return false;
        }
        if ((startLevel != null ? startLevel : 0) != (bundle.startLevel != null ? bundle.startLevel : 0)) {
            return false;
        }
        return value != null ? value.equals(bundle.value) : bundle.value == null;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + getStartLevel();
        result = 31 * result + (isStart() ? 1 : 0);
        result = 31 * result + (isDependency() ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return value;
    }
}
