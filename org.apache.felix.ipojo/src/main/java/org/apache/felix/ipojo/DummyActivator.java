/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.felix.ipojo;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This class is just if you start ipojo. It does nothing but avoid the launch of the Activator class on the iPOJO bundle
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 *
 */
public class DummyActivator implements BundleActivator {

	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext arg0) throws Exception {
		System.out.println("iPOJO Started");
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext arg0) throws Exception {
		System.out.println("iPOJO Stopped");
	}

}
