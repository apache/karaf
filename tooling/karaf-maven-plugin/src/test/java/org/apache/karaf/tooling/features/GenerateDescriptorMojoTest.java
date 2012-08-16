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
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

public class GenerateDescriptorMojoTest {

    @Test
    public void testReadXml() throws JAXBException, SAXException, ParserConfigurationException, XMLStreamException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("input-features.xml");
        Features featuresRoot = JaxbUtil.unmarshal(in, false);
        assert featuresRoot.getRepository().size() == 1;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JaxbUtil.marshal(featuresRoot, baos);
        String s = new String(baos.toByteArray());
        assert s.indexOf("repository") > -1;
        assert s.indexOf("http://karaf.apache.org/xmlns/features/v1.0.0") > -1;
    }
    

    /** FIXME xmlns 1.1.0 validation */
    @Ignore
    @Test
    public void testAttributes() throws Exception {

        InputStream in = getClass().getClassLoader().getResourceAsStream("input-features-1.1.0.xml");
        Features featuresRoot = JaxbUtil.unmarshal(in, false);
        
        List<Feature> featuresList = featuresRoot.getFeature();
        
        assertEquals(featuresList.size(), 1);
        
        Feature feature = featuresList.get(0);

        assertEquals(feature.getInstall(), "auto");


    }
    
}
