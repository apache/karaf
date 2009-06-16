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
package org.apache.felix.karaf.tooling.features;

import org.apache.maven.artifact.Artifact;
import org.easymock.EasyMock;

import static org.easymock.EasyMock.*;

import junit.framework.TestCase;

/**
 * Test cases for {@link GenerateFeaturesXmlMojo}
 */
public class GenerateFeaturesXmlMojoTest extends TestCase {
    
    public void testToString() throws Exception {
        Artifact artifact = EasyMock.createMock(Artifact.class);

        expect(artifact.getGroupId()).andReturn("org.apache.felix.karaf.test");
        expect(artifact.getArtifactId()).andReturn("test-artifact");
        expect(artifact.getVersion()).andReturn("1.2.3");
        
        replay(artifact);
        
        assertEquals("org.apache.felix.karaf.test/test-artifact/1.2.3", GenerateFeaturesXmlMojo.toString(artifact));
    }

}
