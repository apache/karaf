/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.tooling.features;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jmock.Expectations;
import org.jmock.Mockery;

/**
 * Test cases for {@link GenerateFeaturesFileMojo} 
 */
public class GenerateFeaturesFileMojoTest extends TestCase {
    
    private Mockery mockery;
    private GenerateFeaturesFileMojo mojo;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mojo = new GenerateFeaturesFileMojo();
        mockery = new Mockery();
    }
    
    public void testGetBestVersionForArtifactWithOneVersion() throws Exception {       
        final Artifact artifact = mockery.mock(Artifact.class);
        mockery.checking(new Expectations() {{
            allowing(artifact).getVersion(); will(returnValue("2.1.6"));
        }});

        assertEquals("2.1.6_1-SNAPSHOT", mojo.getBestVersionForArtifact(artifact, createVersionList("2.1.6_1-SNAPSHOT")));
    }
    
    public void testGetBestVersionForArtifactWithTwoVersions() throws Exception {       
        final Artifact artifact = mockery.mock(Artifact.class);
        mockery.checking(new Expectations() {{
            allowing(artifact).getVersion(); will(returnValue("2.1.6"));
        }});

        assertEquals("2.1.6_2-SNAPSHOT", mojo.getBestVersionForArtifact(artifact, createVersionList("2.1.6_1-SNAPSHOT", "2.1.6_2-SNAPSHOT")));
    }
    
    public void testGetBestVersionForArtifactWithSameMajorMinor() throws Exception {       
        final Artifact artifact = mockery.mock(Artifact.class);
        mockery.checking(new Expectations() {{
            allowing(artifact).getVersion(); will(returnValue("3.8.2"));
        }});

        assertEquals("3.8.1_1-SNAPSHOT", mojo.getBestVersionForArtifact(artifact, createVersionList("3.8.1_1-SNAPSHOT")));
    }

    
    public void testGetBestVersionForArtifactWithOneNonMatchingVersion() throws Exception {
        final Artifact artifact = mockery.mock(Artifact.class);
        mockery.checking(new Expectations() {{
            allowing(artifact).getVersion(); will(returnValue("9.1.0.1"));
        }});
        
        try {
            mojo.getBestVersionForArtifact(artifact, createVersionList("9.0_1-SNAPSHOT"));
            fail("ArtifactMetadataRetrievalException should have been thrown if no matching version was found");
        } catch (ArtifactMetadataRetrievalException e) {
            // this is expected
        }
    }
    
    public void testGetBestVersionForArtifactWithNoVersions() throws Exception {
        try {
            mojo.getBestVersionForArtifact(mockery.mock(Artifact.class), createVersionList());
            fail("ArtifactMetadataRetrievalException should have been thrown if no matching version was found");
        } catch (ArtifactMetadataRetrievalException e) {
            // this is expected
        }
    }
    
    private List<ArtifactVersion> createVersionList(String... versions) {
        List<ArtifactVersion> results = new ArrayList<ArtifactVersion>();
        for (final String version : versions) {
            results.add(new DefaultArtifactVersion(version));
        }
        return results;
    }
}
