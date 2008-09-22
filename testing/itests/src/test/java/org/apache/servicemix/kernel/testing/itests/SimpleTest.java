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
package org.apache.servicemix.kernel.testing.itests;

import java.util.Properties;

import javax.xml.stream.XMLInputFactory;

import org.apache.servicemix.kernel.testing.support.AbstractIntegrationTest;

public class SimpleTest extends AbstractIntegrationTest {

    private Properties dependencies;

    /**
	 * The manifest to use for the "virtual bundle" created
	 * out of the test classes and resources in this project
	 *
	 * This is actually the boilerplate manifest with one additional
	 * import-package added. We should provide a simpler customization
	 * point for such use cases that doesn't require duplication
	 * of the entire manifest...
	 */
	protected String getManifestLocation() {
		return "classpath:org/apache/servicemix/MANIFEST.MF";
	}

	/**
	 * The location of the packaged OSGi bundles to be installed
	 * for this test. Values are Spring resource paths. The bundles
	 * we want to use are part of the same multi-project maven
	 * build as this project is. Hence we use the localMavenArtifact
	 * helper method to find the bundles produced by the package
	 * phase of the maven build (these tests will run after the
	 * packaging phase, in the integration-test phase).
	 *
	 * JUnit, commons-logging, spring-core and the spring OSGi
	 * test bundle are automatically included so do not need
	 * to be specified here.
	 */
	protected String[] getTestBundlesNames() {
        return new String[] {
            getBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.stax-api-1.0"),
		};
	}

    public void testWoodstox() throws Exception {
        Thread.currentThread().setContextClassLoader(XMLInputFactory.class.getClassLoader());
        System.err.println(XMLInputFactory.class.getClassLoader());
        System.err.println(getClass().getClassLoader());
        XMLInputFactory factory = null;
        try {
            factory = XMLInputFactory.newInstance();
            fail("Factory should not have been found");
        } catch (Throwable t) {
            System.err.println(t.getMessage());
        }
        assertNull(factory);
        installBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.woodstox", null, "jar");
        assertNotNull(XMLInputFactory.newInstance());
    }

}
