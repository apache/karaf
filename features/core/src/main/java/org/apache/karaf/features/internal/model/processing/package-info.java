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
@XmlSchema(namespace = "http://karaf.apache.org/xmlns/features-processing/v1.0.0",
        elementFormDefault = XmlNsForm.QUALIFIED, attributeFormDefault = XmlNsForm.UNQUALIFIED,
        xmlns = {
                @XmlNs(prefix = "", namespaceURI = FEATURES_PROCESSING_NS
                ),
                @XmlNs(prefix = "f", namespaceURI = FeaturesNamespaces.URI_CURRENT)
        }
)
@XmlAccessorType(XmlAccessType.FIELD)
package org.apache.karaf.features.internal.model.processing;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;

import org.apache.karaf.features.FeaturesNamespaces;

import static org.apache.karaf.features.internal.model.processing.FeaturesProcessing.FEATURES_PROCESSING_NS;
