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

package org.apache.karaf.tooling.features;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.karaf.features.FeaturesNamespaces;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.junit.Test;
import org.xml.sax.SAXException;

public class GenerateDescriptorMojoTest {

    @Test
    public void testReadXml100() throws JAXBException, SAXException, ParserConfigurationException, XMLStreamException {

        URL url = getClass().getClassLoader().getResource("input-features-1.0.0.xml");

        Features featuresRoot = JaxbUtil.unmarshal(url.toExternalForm(), false);

        assertEquals(featuresRoot.getRepository().size(), 1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        JaxbUtil.marshal(featuresRoot, baos);

        String text = new String(baos.toByteArray());

        assertTrue(text.contains("repository"));

        assertTrue(text.contains(FeaturesNamespaces.URI_CURRENT));
    }

    @Test
    public void testReadXml1() throws Exception {

        URL url = getClass().getClassLoader().getResource("input-features-1.1.0.xml");

        Features featuresRoot = JaxbUtil.unmarshal(url.toExternalForm(), false);

        List<Feature> featuresList = featuresRoot.getFeature();

        assertEquals(featuresList.size(), 1);

        Feature feature = featuresList.get(0);

        assertEquals(feature.getInstall(), "auto");
    }

}
