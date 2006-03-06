/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/ServiceReference.java,v 1.11 2005/05/13 20:32:55 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

import java.util.Dictionary;

/**
 * A reference to a service.
 * 
 * <p>
 * The Framework returns <code>ServiceReference</code> objects from the
 * <code>BundleContext.getServiceReference</code> and
 * <code>BundleContext.getServiceReferences</code> methods.
 * <p>
 * A <code>ServiceReference</code> object may be shared between bundles and can be
 * used to examine the properties of the service and to get the service object.
 * <p>
 * Every service registered in the Framework has a unique
 * <code>ServiceRegistration</code> object and may have multiple, distinct
 * <code>ServiceReference</code> objects referring to it.
 * <code>ServiceReference</code> objects associated with a
 * <code>ServiceRegistration</code> object have the same <code>hashCode</code>
 * and are considered equal (more specifically, their <code>equals()</code>
 * method will return <code>true</code> when compared).
 * <p>
 * If the same service object is registered multiple times,
 * <code>ServiceReference</code> objects associated with different
 * <code>ServiceRegistration</code> objects are not equal.
 * 
 * @version $Revision: 1.11 $
 * @see BundleContext#getServiceReference
 * @see BundleContext#getServiceReferences
 * @see BundleContext#getService
 */

public abstract interface ServiceReference {
	/**
	 * Returns the property value to which the specified property key is mapped
	 * in the properties <code>Dictionary</code> object of the service
	 * referenced by this <code>ServiceReference</code> object.
	 * 
	 * <p>
	 * Property keys are case-insensitive.
	 * 
	 * <p>
	 * This method must continue to return property values after the service has
	 * been unregistered. This is so references to unregistered services (for
	 * example, <code>ServiceReference</code> objects stored in the log) can
	 * still be interrogated.
	 * 
	 * @param key The property key.
	 * @return The property value to which the key is mapped; <code>null</code>
	 *         if there is no property named after the key.
	 */
	public abstract Object getProperty(String key);

	/**
	 * Returns an array of the keys in the properties <code>Dictionary</code>
	 * object of the service referenced by this <code>ServiceReference</code>
	 * object.
	 * 
	 * <p>
	 * This method will continue to return the keys after the service has been
	 * unregistered. This is so references to unregistered services (for
	 * example, <code>ServiceReference</code> objects stored in the log) can
	 * still be interrogated.
	 * 
	 * <p>
	 * This method is <i>case-preserving </i>; this means that every key in the
	 * returned array must have the same case as the corresponding key in the
	 * properties <code>Dictionary</code> that was passed to the
	 * {@link BundleContext#registerService(String[],Object,Dictionary)}or
	 * {@link ServiceRegistration#setProperties}methods.
	 * 
	 * @return An array of property keys.
	 */
	public abstract String[] getPropertyKeys();

	/**
	 * Returns the bundle that registered the service referenced by this
	 * <code>ServiceReference</code> object.
	 * 
	 * <p>
	 * This method must return <code>null</code> when the service has
	 * been unregistered. This can be used to determine if the service has been
	 * unregistered.
	 * 
	 * @return The bundle that registered the service referenced by this
	 *         <code>ServiceReference</code> object; <code>null</code> if
	 *         that service has already been unregistered.
	 * @see BundleContext#registerService(String[],Object,Dictionary)
	 */
	public abstract Bundle getBundle();

	/**
	 * Returns the bundles that are using the service referenced by this
	 * <code>ServiceReference</code> object. Specifically, this method returns
	 * the bundles whose usage count for that service is greater than zero.
	 * 
	 * @return An array of bundles whose usage count for the service referenced
	 *         by this <code>ServiceReference</code> object is greater than
	 *         zero; <code>null</code> if no bundles are currently using that
	 *         service.
	 * 
	 * @since 1.1
	 */
	public abstract Bundle[] getUsingBundles();

	/**
	 * Tests if the bundle that registered the service referenced by this
	 * <code>ServiceReference</code> and the specified bundle use the same source
	 * for the package of the specified class name.
	 * <p>
	 * This method performs the following checks:
	 * <ol>
	 * <li>Get the package name from the specified class name.</li>
	 * <li>For the bundle that registered the service referenced by this
	 * <code>ServiceReference</code> (registrant bundle); find the source for the
	 * package. If no source is found then return <code>true</code> if the
	 * registrant bundle is equal to the specified bundle; otherwise return
	 * <code>false</code>.</li>
	 * <li>If the package source of the registrant bundle is equal to the
	 * package source of the specified bundle then return <code>true</code>;
	 * otherwise return <code>false</code>.</li>
	 * </ol>
	 * 
	 * @param bundle The <code>Bundle</code> object to check.
	 * @param className The class name to check.
	 * @return <code>true</code> if the bundle which registered the service
	 *         referenced by this <code>ServiceReference</code> and the specified
	 *         bundle use the same source for the package of the specified class
	 *         name. Otherwise <code>false</code> is returned.
	 * 
	 * @since 1.3
	 */
	public abstract boolean isAssignableTo(Bundle bundle, String className);

}
