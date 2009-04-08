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
package org.apache.felix.transaction.itests;

import javax.transaction.TransactionManager;

import org.apache.servicemix.kernel.testing.support.AbstractIntegrationTest;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Constants;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 */
public class Test extends AbstractIntegrationTest {

    private static final String PLATFORM_TRANSACTION_MANAGER_CLASS = "org.springframework.transaction.PlatformTransactionManager";

    public void test() throws Exception {
        ServiceTracker tracker = new ServiceTracker(bundleContext, TransactionManager.class.getName(), null);
        tracker.open();

        tracker.waitForService(5000L);
        ServiceReference ref = tracker.getServiceReference();
        assertNotNull(ref);
        String[] objClass = (String[]) ref.getProperty(Constants.OBJECTCLASS);
        assertNotNull(objClass);
        boolean found = false;
        for (String clazz : objClass) {
            found |= PLATFORM_TRANSACTION_MANAGER_CLASS.equals(clazz);
        }
        assertFalse(found);

        Bundle bundle = ref.getBundle();
        bundle.stop();
        installBundle("org.springframework", "spring-tx", null, "jar");
        getOsgiService(PackageAdmin.class).refreshPackages(new Bundle[] { bundle });
        System.err.println("Bundle refreshed");
        Thread.sleep(500);
        System.err.println("Starting bundle");
        bundle.start();

        tracker.waitForService(5000L);
        ref = tracker.getServiceReference();
        assertNotNull(ref);
        objClass = (String[]) ref.getProperty(Constants.OBJECTCLASS);
        assertNotNull(objClass);
        found = false;
        for (String clazz : objClass) {
            found |= PLATFORM_TRANSACTION_MANAGER_CLASS.equals(clazz);
        }
        assertTrue(found);

        tracker.close();
    }

    //============= Plumbing ==============/

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
		return "classpath:org/apache/felix/transaction/MANIFEST.MF";
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
            getBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-j2ee-connector_1.5_spec"),
            getBundle("org.apache.felix", "org.apache.felix.transaction"),
		};
	}

}