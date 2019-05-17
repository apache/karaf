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

import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>Dependency of feature.</p>
 * <p>Java class for dependency complex type.</p>
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>
 * &lt;complexType name="dependency"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://karaf.apache.org/xmlns/features/v1.0.0&gt;featureName"&gt;
 *       &lt;attribute name="version" type="{http://www.w3.org/2001/XMLSchema}string" default="0.0.0" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dependency", propOrder = {"name"})
public class Dependency implements org.apache.karaf.features.Dependency {

    @XmlValue
    protected String name;
    @XmlAttribute
    protected String version;
    @XmlAttribute
    protected Boolean prerequisite;
    @XmlAttribute
    protected Boolean dependency;

    @XmlTransient
    private boolean blacklisted;

    public Dependency() {
        // Nothing to do
    }

    public Dependency(String name, String version) {
        this.name = name;
        this.version = version;
    }

    /**
     * Feature name should be non empty string.
     *
     * @return possible object is
     * {@link String }
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the version property.
     *
     * @return possible object is
     * {@link String }
     */
    @Override
    public String getVersion() {
        if (version == null) {
            return Feature.DEFAULT_VERSION;
        } else {
            return version;
        }
    }

    /**
     * Sets the value of the version property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Since version has a default value ("0.0.0"), returns whether or not the version has been set.
     */
    @Override
    public boolean hasVersion() {
        return version != null;
    }

    @Override
    public boolean isPrerequisite() {
        return prerequisite == null ? false : prerequisite;
    }

    public void setPrerequisite(Boolean prerequisite) {
        this.prerequisite = prerequisite;
    }

    @Override
    public boolean isDependency() {
        return dependency == null ? false : dependency;
    }

    public void setDependency(Boolean dependency) {
        this.dependency = dependency;
    }

    @Override
    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public String toString() {
        return getName() + Feature.VERSION_SEPARATOR + getVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dependency that = (Dependency) o;

        if (!Objects.equals(prerequisite, that.prerequisite)) return false;
        if (!Objects.equals(dependency, that.dependency)) return false;
        if (!Objects.equals(name, that.name)) return false;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (prerequisite != null ? prerequisite.hashCode() : 0);
        result = 31 * result + (dependency != null ? dependency.hashCode() : 0);
        return result;
    }
}
