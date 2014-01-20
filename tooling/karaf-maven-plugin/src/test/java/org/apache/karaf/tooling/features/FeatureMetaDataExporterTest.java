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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.stream.XMLStreamException;

import org.apache.karaf.tooling.features.model.ArtifactRef;
import org.apache.karaf.tooling.features.model.BundleRef;
import org.apache.karaf.tooling.features.model.Feature;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.Test;

public class FeatureMetaDataExporterTest {

    @Test
    public void testWriteFeature() throws XMLStreamException, UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FeatureMetaDataExporter featureMetaDataExporter = new FeatureMetaDataExporter(baos);
        
        BundleRef bundle = new BundleRef("mvn:org.apache.example/example/1.0.0", 10);
        Artifact bundleArtifact = new DefaultArtifact("org.apache.example", "example", "1.0.0",
                                                      null, "jar", null,
                                                      new DefaultArtifactHandler());
        bundle.setArtifact(bundleArtifact);

        ArtifactRef configFile = new ArtifactRef("mvn:org.apache.example/example/1.0.0/cfg");
        Artifact configFileArtifact = new DefaultArtifact("org.apache.example", "example", "1.0.0",
                                                          null, "xml", "exampleconfig",
                                                          new DefaultArtifactHandler());
        configFile.setArtifact(configFileArtifact);

        Feature feature = new Feature("example");
        feature.addBundle(bundle);
        feature.addConfigFile(configFile);

        featureMetaDataExporter.writeFeature(feature);
        featureMetaDataExporter.close();
        
        assertTrue(formatString(baos.toString("UTF-8")).contains(expectedValue()));
    }

    private String expectedValue() {
        return formatString(
              "<features>" +
                   "<feature name=\"example\">" +
                       "<bundle start-level=\"10\" name=\"example-1.0.0.jar\" groupId=\"org.apache.example\" artifactId=\"example\" type=\"jar\" version=\"1.0.0\">mvn:org.apache.example/example/1.0.0</bundle>" +
                       "<config name=\"example-1.0.0-exampleconfig.xml\" groupId=\"org.apache.example\" artifactId=\"example\" type=\"xml\" classifier=\"exampleconfig\" version=\"1.0.0\">mvn:org.apache.example/example/1.0.0/cfg</config>" +
                   "</feature>" +
               "</features>");
    }
    
    private String formatString(String string) {
        return string.replaceAll("\n", "").replaceAll("\r", "");
    }
}
