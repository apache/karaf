/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.jmood.core;
import junit.framework.TestCase;

import org.apache.felix.jmood.FelixLauncher;

public class TestHarness extends TestCase {
	private FelixLauncher launcher;
	protected void setUp() throws Exception {
		//ISSUE: this is not a unit test, but maven runs it as one
		//before it builds the jar
        super.setUp();
        launcher=new FelixLauncher();
        String jmood="file:target/org.apache.felix.jmood-0.8.0-SNAPSHOT.jar";
        launcher.addBundle(jmood);
        launcher.addPackage("org.osgi.framework");
        launcher.addPackage("org.osgi.util.tracker");
        launcher.addPackage("org.osgi.service.log");
        launcher.addPackage("org.osgi.service.packageadmin");
        launcher.addPackage("org.osgi.service.startlevel");
        launcher.addPackage("org.osgi.service.permissionadmin");
        launcher.addPackage("org.osgi.service.useradmin");
        launcher.addPackage("org.osgi.service.cm");
        launcher.addPackage("javax.management");
        launcher.addPackage("javax.management.remote");
        launcher.blockingStart();
	}
	protected void tearDown() throws Exception {
		super.tearDown();
		launcher.shutdown();
	}
	public void testDummy() {
		//to avoid test not found assertion error
	}
}
