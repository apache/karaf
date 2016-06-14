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
    protected boolean prerequisite;
    @XmlAttribute
    protected boolean dependency;

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

    @Override
    public boolean isPrerequisite() {
        return prerequisite;
    }

    public void setPrerequisite(boolean prerequisite) {
        this.prerequisite = prerequisite;
    }

    @Override
    public boolean isDependency() {
        return dependency;
    }

    public void setDependency(boolean dependency) {
        this.dependency = dependency;
    }

    public String toString() {
        return getName() + Feature.VERSION_SEPARATOR + getVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dependency that = (Dependency) o;

        if (prerequisite != that.prerequisite) return false;
        if (dependency != that.dependency) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return version != null ? version.equals(that.version) : that.version == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (prerequisite ? 1 : 0);
        result = 31 * result + (dependency ? 1 : 0);
        return result;
    }
}
