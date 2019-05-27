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

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.karaf.features.ConfigInfo;

/**
 * <p>Configuration entries which should be created during feature installation. This configuration may be used with OSGi Configuration Admin.</p>
 * <p>Java class for config complex type.</p>
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>
 * &lt;complexType name="config"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "config", propOrder = {"value"})
public class Config implements ConfigInfo {

    @XmlValue
    protected String value;
    @XmlAttribute(required = true)
    protected String name;
    @XmlAttribute(required = false)
	private Boolean append = false;
    @XmlAttribute
	private Boolean external = false;
    @XmlAttribute
    private Boolean override = false;

    /**
     * Gets the value of the value property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value) {
        this.name = value;
    }

	/**
	 * @return the append
	 */
	public boolean isAppend() {
		return append;
	}

	/**
	 * @param append the append to set
	 */
	public void setAppend(boolean append) {
		this.append = append;
	}

	public boolean isExternal() {
		return external;
	}

	public void setExternal(boolean external) {
		this.external = external;
	}

	/**
	 * Gets the value of the override property.
	 *
	 * @return possible object is
	 * {@link Boolean }
	 */
	public boolean isOverride() {
		return override == null ? false : override;
	}

	/**
	 * Sets the value of the override property.
	 *
	 * @param value allowed object is
	 *              {@link Boolean }
	 */
	public void setOverride(Boolean value) {
		this.override = value;
	}

	public Properties getProperties() {
		Properties props = new Properties();
		try {
			org.apache.felix.utils.properties.Properties properties
					= new org.apache.felix.utils.properties.Properties();
			properties.load(new StringReader(value));
			for (Map.Entry<String, String> e : properties.entrySet()) {
				props.put(e.getKey(), e.getValue());
			}
		} catch (IOException e) {
			// ignore??
		}
		return props;
	}

}
