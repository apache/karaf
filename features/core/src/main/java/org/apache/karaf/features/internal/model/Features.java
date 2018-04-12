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
import java.util.ListIterator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.karaf.features.Blacklisting;

/**
 * <p>Root element of Feature definition. It contains optional attribute which allow
 * name of repository. This name will be used in shell to display source repository
 * of given feature.</p>
 * <p>Java class for featuresRoot complex type.</p>
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>
 * &lt;complexType name="features"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="repository" type="{http://www.w3.org/2001/XMLSchema}anyURI" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="resource-repository" type="{http://www.w3.org/2001/XMLSchema}anyURI" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="feature" type="{http://karaf.apache.org/xmlns/features/v1.0.0}feature" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlRootElement(name = "features", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "features", propOrder = {"repository", "resourceRepository", "feature"})
public class Features implements Blacklisting {

    @XmlSchemaType(name = "anyURI")
    @XmlElement(name = "repository", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<String> repository;
    @XmlSchemaType(name = "anyURI") 
    @XmlElement(name = "resource-repository", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<String> resourceRepository;
    @XmlElement(name = "feature", namespace=org.apache.karaf.features.FeaturesNamespaces.URI_CURRENT)
    protected List<Feature> feature;
    @XmlAttribute
    protected String name;
    @XmlTransient
    private String namespace;
    @XmlTransient
    private boolean blacklisted;

    /**
     * <p>Get the value of the repository property.</p>
     *
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the repository property.</p>
     *
     * <p>For example, to add a new item, do as follows:</p>
     *
     * <pre>
     *    getRepository().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list
     * {@link String}.</p>
     *
     * @return the list of inner repositories.
     */
    public List<String> getRepository() {
        if (repository == null) {
            repository = new ArrayList<>();
        }
        return this.repository;
    }

    /**
     * <p>Get the value of the resource repository property.</p>
     *
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.</p>
     *
     * <p>This is why there is not a <CODE>set</CODE> method for
     * the resource repository property.</p>
     *
     * <p>For example, to add a new item, do as follows:</p>
     *
     * <pre>
     *    getResourceRepository().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list
     * {@link String}</p>
     *
     * @return the list of inner resource repositories.
     */
    public List<String> getResourceRepository() {
        if (resourceRepository == null) {
            resourceRepository = new ArrayList<>();
        }
        return this.resourceRepository;
    }

    /**
     * <p>Get the value of the feature property.</p>
     *
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.</p>
     *
     * <p>This is why there is not a <CODE>set</CODE> method
     * for the feature property.</p>
     *
     * <p>For example, to add a new item, do as follows:</p>
     *
     * <pre>
     *    getFeatures().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list
     * {@link Feature}.</p>
     *
     * @return the list of inner features.
     */
    public List<Feature> getFeature() {
        if (feature == null) {
            feature = new ArrayList<>();
        }
        return this.feature;
    }

    /**
     * Get the value of the name property.
     *
     * @return possible object is {@link String}.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of the name property.
     *
     * @param value allowed object is {@link String}.
     */
    public void setName(String value) {
        this.name = value;
    }

    public void postUnmarshall(String repositoryUri) {
        if (feature != null) {
            for (Feature f : feature) {
                f.setRepositoryUrl(repositoryUri);
                f.setNamespace(namespace);
                f.setResourceRepositories(getResourceRepository());
                f.postUnmarshall();
            }
        }
        trim(repository);
        trim(resourceRepository);
    }

    private static void trim(List<String> list) {
        if (list != null) {
            for (ListIterator<String> it = list.listIterator(); it.hasNext();) {
                it.set(it.next().trim());
            }
        }
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

}
