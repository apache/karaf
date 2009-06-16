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
import org.jmock.Expectations;
import org.jmock.Mockery;

import junit.framework.TestCase;

/**
 * Test cases for {@link GenerateFeaturesXmlMojo}
 */
public class GenerateFeaturesXmlMojoTest extends TestCase {
    
    private Mockery mockery;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mockery = new Mockery();
    }
    
    public void testToString() throws Exception {
        final Artifact artifact = mockery.mock(Artifact.class);
        mockery.checking(new Expectations() {{
            allowing(artifact).getGroupId(); will(returnValue("org.apache.servicemix.test"));
            allowing(artifact).getArtifactId(); will(returnValue("test-artifact"));
            allowing(artifact).getVersion(); will(returnValue("1.2.3"));
        }});
        assertEquals("org.apache.servicemix.test/test-artifact/1.2.3", GenerateFeaturesXmlMojo.toString(artifact));
    }

}
