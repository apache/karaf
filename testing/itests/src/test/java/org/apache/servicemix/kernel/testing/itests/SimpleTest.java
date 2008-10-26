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
        return new String[0];
	}

    /**
     * Do not include the jaxp-ri bundle by default, as we want to test it
     * @return
     */
    @Override
    protected String[] getTestFrameworkBundlesNames() {
        return new String[] {
            getBundle("org.apache.geronimo.specs", "geronimo-servlet_2.5_spec"),
            getBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.jaxp-api-1.3"),
            getBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.stax-api-1.0"),
            getBundle("org.apache.felix", "org.osgi.compendium"),
            getBundle("org.apache.felix", "org.apache.felix.configadmin"),
            getBundle("org.ops4j.pax.logging", "pax-logging-api"),
            getBundle("org.ops4j.pax.logging", "pax-logging-service"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxp-ri"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.aopalliance"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.junit"),
            getBundle("org.springframework", "spring-beans"),
            getBundle("org.springframework", "spring-core"),
            getBundle("org.springframework", "spring-context"),
            getBundle("org.springframework", "spring-aop"),
            getBundle("org.springframework", "spring-test"),
            getBundle("org.springframework.osgi", "spring-osgi-core"),
            getBundle("org.springframework.osgi", "spring-osgi-io"),
            getBundle("org.springframework.osgi", "spring-osgi-extender"),
            getBundle("org.springframework.osgi", "spring-osgi-test"),
            getBundle("org.springframework.osgi", "spring-osgi-annotation"),
            getBundle("org.apache.servicemix.kernel.testing", "org.apache.servicemix.kernel.testing.support"),
		};
    }

    public void testDocumentBuilderFactory() throws Exception {
        try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            fail("Implementation should not have been found");
        } catch (Throwable t) {
        }
        Bundle b = installBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxp-ri", null, "jar");
        try {
		    assertNotNull(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
        } finally {
            b.uninstall();
        }
    }

    public void testTransformerFactory() throws Exception {
        try {
            TransformerFactory.newInstance().newTransformer();
            fail("Implementation should not have been found");
        } catch (Throwable t) {
        }
        Bundle b = installBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxp-ri", null, "jar");
        try {
            assertNotNull(TransformerFactory.newInstance().newTransformer());
        } finally {
            b.uninstall();
        }
    }

    public void testSchemaFactory() throws Exception {
        try {
            SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema").newSchema();
            fail("Implementation should not have been found");
        } catch (Throwable t) {
        }
        Bundle b = installBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxp-ri", null, "jar");
        try {
            assertNotNull(SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema").newSchema());
        } finally {
            b.uninstall();
        }
    }

    public void testStax() throws Exception {
        try {
            XMLInputFactory.newInstance();
            fail("Implementation should not have been found");
        } catch (Throwable t) {
        }
        Bundle b = installBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxp-ri", null, "jar");
        try {
            assertNotNull(XMLInputFactory.newInstance());
        } finally {
            b.uninstall();
        }
    }

}
