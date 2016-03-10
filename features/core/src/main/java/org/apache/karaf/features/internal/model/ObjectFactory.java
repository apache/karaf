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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * <p>This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the org.apache.karaf.features.wrapper package.</p>
 * <p>An ObjectFactory allows you to programmatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.</p>
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName FEATURES_QNAME = new QName("http://karaf.apache.org/xmlns/features/v1.0.0", "features");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.apache.karaf.features.wrapper
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ConfigFile}
     *
     * @return the config file.
     */
    public ConfigFile createConfigFile() {
        return new ConfigFile();
    }

    /**
     * Create an instance of {@link Dependency}
     *
     * @return the dependency.
     */
    public Dependency createDependency() {
        return new Dependency();
    }

    /**
     * Create an instance of {@link Bundle}
     *
     * @return the bundle.
     */
    public Bundle createBundle() {
        return new Bundle();
    }

    /**
     * Create an instance of {@link Features}
     *
     * @return the features.
     */
    public Features createFeaturesRoot() {
        return new Features();
    }

    /**
     * Create an instance of {@link Config}
     *
     * @return the config.
     */
    public Config createConfig() {
        return new Config();
    }

    /**
     * Create an instance of {@link Feature}
     *
     * @return the feature.
     */
    public Feature createFeature() {
        return new Feature();
    }

    /**
     * Create an instance of {@link JAXBElement}{@code &lt;}{@link Features }{@code &gt;}
     *
     * @param value the features
     * @return the JAXB representation of the given features.
     */
    @XmlElementDecl(namespace = "http://karaf.apache.org/xmlns/features/v1.0.0", name = "features")
    public JAXBElement<Features> createFeatures(Features value) {
        return new JAXBElement<>(FEATURES_QNAME, Features.class, null, value);
    }

}
