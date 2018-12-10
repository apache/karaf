/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.main;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;

public class MainStartTest {

    private Main main;

    @After
    public void tearDown() throws Exception {
        if(main != null){
            main.destroy();
        }

		System.clearProperty("karaf.home");
		System.clearProperty("karaf.data");
		System.clearProperty("karaf.log");
    }

    @Test
    public void testAutoStart() throws Exception {
        File basedir = new File(getClass().getClassLoader().getResource("foo").getPath()).getParentFile();
        File home = new File(basedir, "test-karaf-home");
        // generate an unique folder name to avoid conflict with folder created by other unit tests (KARAF-2558)
        File data = new File(home, "data" + System.currentTimeMillis());
		File log = new File(home, "log" + System.currentTimeMillis());

		String[] args = new String[0];
		System.setProperty("karaf.home", home.toString());
		System.setProperty("karaf.data", data.toString());
		System.setProperty("karaf.log", log.toString());

		main = new Main(args);
		main.launch();
		Framework framework = main.getFramework();
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertEquals(3, bundles.length);
		
		// Give the framework some time to start the bundles
		Thread.sleep(1000);

		Bundle bundle1 = framework.getBundleContext().getBundle("mvn:org.apache.aries.blueprint/org.apache.aries.blueprint.api/1.0.0");
		Assert.assertEquals(Bundle.ACTIVE, bundle1.getState());

		Bundle bundle2 = framework.getBundleContext().getBundle("pax-url-mvn.jar");
		Assert.assertEquals(Bundle.ACTIVE, bundle2.getState());
	}

}
