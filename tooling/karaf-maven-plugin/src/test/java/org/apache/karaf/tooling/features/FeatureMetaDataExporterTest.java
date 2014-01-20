package org.apache.karaf.tooling.features;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

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
    public void testWriteFeature() throws XMLStreamException {
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
        
        assertEquals(formatString(expectedValue()), formatString(baos.toString()));
    }

    private String expectedValue() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<features>" +
                   "<feature name=\"example\">" +
                       "<bundle start-level=\"10\" name=\"example-1.0.0.jar\" groupId=\"org.apache.example\" artifactId=\"example\" type=\"jar\" version=\"1.0.0\">mvn:org.apache.example/example/1.0.0</bundle>" +
                       "<config name=\"example-1.0.0-exampleconfig.xml\" groupId=\"org.apache.example\" artifactId=\"example\" type=\"xml\" classifier=\"exampleconfig\" version=\"1.0.0\">mvn:org.apache.example/example/1.0.0/cfg</config>" +
                   "</feature>" +
               "</features>";
    }
    
    private String formatString(String string) {
        return string.replaceAll("\n", "").replaceAll("\r", "");
    }
}
