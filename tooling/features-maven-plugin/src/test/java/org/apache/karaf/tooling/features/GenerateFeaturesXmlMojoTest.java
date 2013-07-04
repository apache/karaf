/**
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
package org.apache.karaf.tooling.features;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;

import org.apache.karaf.tooling.features.GenerateFeaturesXmlMojo.Feature;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.EasyMock;

import static org.easymock.EasyMock.*;

import junit.framework.TestCase;
import org.junit.Ignore;

/**
 * Test cases for {@link GenerateFeaturesXmlMojo}
 */
public class GenerateFeaturesXmlMojoTest extends TestCase {
    
    private GenerateFeaturesXmlMojo mojo;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mojo = new GenerateFeaturesXmlMojo();
    }

    public void testToString() throws Exception {
        Artifact artifact = EasyMock.createMock(Artifact.class);

        expect(artifact.getGroupId()).andReturn("org.apache.karaf.test");
        expect(artifact.getArtifactId()).andReturn("test-artifact");
        expect(artifact.getVersion()).andReturn("1.2.3");
        expect(artifact.getType()).andReturn("jar");
        expect(artifact.hasClassifier()).andReturn(false);
        
        replay(artifact);
        
        assertEquals("org.apache.karaf.test/test-artifact/1.2.3", GenerateFeaturesXmlMojo.toString(artifact));
    }

    public void testToStringWithClassifier() throws Exception {
        Artifact artifact = EasyMock.createMock(Artifact.class);

        expect(artifact.getGroupId()).andReturn("org.apache.karaf.test");
        expect(artifact.getArtifactId()).andReturn("test-artifact");
        expect(artifact.getVersion()).andReturn("1.2.3");
        expect(artifact.getType()).andReturn("zip").times(2);
        expect(artifact.hasClassifier()).andReturn(true);
        expect(artifact.getClassifier()).andReturn("linux");

        replay(artifact);

        assertEquals("org.apache.karaf.test/test-artifact/1.2.3/zip/linux", GenerateFeaturesXmlMojo.toString(artifact));
    }

    @Ignore("Disable temporarly")
    public void testInstallMode() throws Exception {
    	
        Artifact artifact = EasyMock.createMock(Artifact.class);

        expect(artifact.getGroupId()).andReturn("org.apache.karaf.test").anyTimes();
        expect(artifact.getArtifactId()).andReturn("test-artifact").anyTimes();
        expect(artifact.getBaseVersion()).andReturn("1.2.3").anyTimes();
        expect(artifact.getVersion()).andReturn("1.2.3").anyTimes();
        expect(artifact.getType()).andReturn("jar").anyTimes();
        expect(artifact.hasClassifier()).andReturn(false).anyTimes();
        
        replay(artifact);
        
        mojo.installMode="auto";
        
        Feature feature = mojo.new Feature(artifact);
        
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        
        PrintStream out = new PrintStream(byteStream);
        
		feature.write(out);
		
		String source = byteStream.toString("UTF-8");
		
		System.out.println(source);
		
		String target = FileUtils.fileRead("./src/test/resources/features-01.xml", "UTF-8");
		
		//assertTrue(target.contains(source));
        
    } 

}
