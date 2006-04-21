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
package org.apache.felix.scr;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This activator is used to cover requirement described in section 112.8.1
 * When SCR is implemented as a bundle, any component configurations
 * activated by SCR must be deactivated when the SCR bundle is stopped. When
 * the SCR bundle is started, it must process any components that are declared
 * in active bundles. 
 *
 */
public class Activator implements BundleActivator {

	public void start(BundleContext arg0) throws Exception {
		//TODO: pending
	}

	public void stop(BundleContext arg0) throws Exception {
		// TODO: pending
	}

}
