/*
 * Copyright (c) OSGi Alliance (2000, 2009). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.framework;

/**
 * Customizes the starting and stopping of a bundle.
 * <p>
 * <code>BundleActivator</code> is an interface that may be implemented when a
 * bundle is started or stopped. The Framework can create instances of a
 * bundle's <code>BundleActivator</code> as required. If an instance's
 * <code>BundleActivator.start</code> method executes successfully, it is
 * guaranteed that the same instance's <code>BundleActivator.stop</code>
 * method will be called when the bundle is to be stopped. The Framework must
 * not concurrently call a <code>BundleActivator</code> object.
 * 
 * <p>
 * <code>BundleActivator</code> is specified through the
 * <code>Bundle-Activator</code> Manifest header. A bundle can only specify a
 * single <code>BundleActivator</code> in the Manifest file. Fragment bundles
 * must not have a <code>BundleActivator</code>. The form of the Manifest
 * header is:
 * 
 * <p>
 * <code>Bundle-Activator: <i>class-name</i></code>
 * 
 * <p>
 * where <code><i>class-name</i></code> is a fully qualified Java classname.
 * <p>
 * The specified <code>BundleActivator</code> class must have a public
 * constructor that takes no parameters so that a <code>BundleActivator</code>
 * object can be created by <code>Class.newInstance()</code>.
 * 
 * @NotThreadSafe
 * @version $Revision: 6361 $
 */

public interface BundleActivator {
	/**
	 * Called when this bundle is started so the Framework can perform the
	 * bundle-specific activities necessary to start this bundle. This method
	 * can be used to register services or to allocate any resources that this
	 * bundle needs.
	 * 
	 * <p>
	 * This method must complete and return to its caller in a timely manner.
	 * 
	 * @param context The execution context of the bundle being started.
	 * @throws Exception If this method throws an exception, this
	 *         bundle is marked as stopped and the Framework will remove this
	 *         bundle's listeners, unregister all services registered by this
	 *         bundle, and release all services used by this bundle.
	 */
	public void start(BundleContext context) throws Exception;

	/**
	 * Called when this bundle is stopped so the Framework can perform the
	 * bundle-specific activities necessary to stop the bundle. In general, this
	 * method should undo the work that the <code>BundleActivator.start</code>
	 * method started. There should be no active threads that were started by
	 * this bundle when this bundle returns. A stopped bundle must not call any
	 * Framework objects.
	 * 
	 * <p>
	 * This method must complete and return to its caller in a timely manner.
	 * 
	 * @param context The execution context of the bundle being stopped.
	 * @throws Exception If this method throws an exception, the
	 *         bundle is still marked as stopped, and the Framework will remove
	 *         the bundle's listeners, unregister all services registered by the
	 *         bundle, and release all services used by the bundle.
	 */
	public void stop(BundleContext context) throws Exception;
}
