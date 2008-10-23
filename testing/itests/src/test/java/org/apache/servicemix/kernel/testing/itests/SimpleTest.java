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

import javax.xml.stream.XMLInputFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;

import org.apache.servicemix.kernel.testing.support.AbstractIntegrationTest;
import org.osgi.framework.Bundle;

public class SimpleTest extends AbstractIntegrationTest {

    static {
        System.setProperty("jaxp.debug", "true");
        System.setProperty("org.apache.servicemix.specs.debug", "true");
    }

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
            getBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.jaxp-api-1.3"),
		};
	}

    public void testDocumentBuilderFactory() throws Exception {
        try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            fail("Implementation should not have been found");
        } catch (Throwable t) {
        }
        Bundle b = installBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxp-ri", null, "jar");
        Thread.sleep(100);
		assertNotNull(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
        b.uninstall();
    }

    public void testTransformerFactory() throws Exception {
        try {
            TransformerFactory.newInstance().newTransformer();
            fail("Implementation should not have been found");
        } catch (Throwable t) {
        }
        Bundle b = installBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxp-ri", null, "jar");
        Thread.sleep(100);
        assertNotNull(TransformerFactory.newInstance().newTransformer());
        b.uninstall();
    }

    public void testSchemaFactory() throws Exception {
        try {
            SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema").newSchema();
            fail("Implementation should not have been found");
        } catch (Throwable t) {
        }
        Bundle b = installBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxp-ri", null, "jar");
        Thread.sleep(100);
        assertNotNull(SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema").newSchema());
        b.uninstall();
    }

    public void testWoodstox() throws Exception {
        try {
            XMLInputFactory.newInstance();
            fail("Implementation should not have been found");
        } catch (Throwable t) {
        }
        Bundle b = installBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxp-ri", null, "jar");
        assertNotNull(XMLInputFactory.newInstance());
        b.uninstall();
    }

}
