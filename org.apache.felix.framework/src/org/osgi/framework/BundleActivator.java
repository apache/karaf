/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/BundleActivator.java,v 1.8 2005/06/21 16:22:12 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

/**
 * Customizes the starting and stopping of a bundle.
 * <p>
 * <code>BundleActivator</code> is an interface that may be implemented when a
 * bundle is started or stopped. The Framework can create instances of a
 * bundle's <code>BundleActivator</code> as required. If an instance's
 * <code>BundleActivator.start</code> method executes successfully, it is
 * guaranteed that the same instance's <code>BundleActivator.stop</code> method
 * will be called when the bundle is to be stopped.
 * 
 * <p>
 * <code>BundleActivator</code> is specified through the <code>Bundle-Activator</code>
 * Manifest header. A bundle can only specify a single <code>BundleActivator</code>
 * in the Manifest file. Fragment bundles must not have a <code>BundleActivator</code>.
 * The form of the Manifest header is:
 * 
 * <pre>
 *  Bundle-Activator: &lt;i&gt;class-name&lt;/i&gt;
 * </pre>
 * 
 * where <code>class-name</code> is a fully qualified Java classname.
 * <p>
 * The specified <code>BundleActivator</code> class must have a public constructor
 * that takes no parameters so that a <code>BundleActivator</code> object can be
 * created by <code>Class.newInstance()</code>.
 * 
 * @version $Revision: 1.8 $
 */

public abstract interface BundleActivator {
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
	 * @exception java.lang.Exception If this method throws an exception, this
	 *            bundle is marked as stopped and the Framework will remove this
	 *            bundle's listeners, unregister all services registered by this
	 *            bundle, and release all services used by this bundle.
	 * @see Bundle#start
	 */
	public abstract void start(BundleContext context) throws Exception;

	/**
	 * Called when this bundle is stopped so the Framework can perform the
	 * bundle-specific activities necessary to stop the bundle. In general, this
	 * method should undo the work that the <code>BundleActivator.start</code>
	 * method started. There should be no active threads that were started by
	 * this bundle when this bundle returns. A stopped bundle must
	 * not call any Framework objects.
	 * 
	 * <p>
	 * This method must complete and return to its caller in a timely manner.
	 * 
	 * @param context The execution context of the bundle being stopped.
	 * @exception java.lang.Exception If this method throws an exception, the
	 *            bundle is still marked as stopped, and the Framework will
	 *            remove the bundle's listeners, unregister all services
	 *            registered by the bundle, and release all services used by the
	 *            bundle.
	 * @see Bundle#stop
	 */
	public abstract void stop(BundleContext context) throws Exception;
}

