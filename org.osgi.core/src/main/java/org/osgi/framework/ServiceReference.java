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

import java.util.Dictionary;

/**
 * A reference to a service.
 * 
 * <p>
 * The Framework returns <code>ServiceReference</code> objects from the
 * <code>BundleContext.getServiceReference</code> and
 * <code>BundleContext.getServiceReferences</code> methods.
 * <p>
 * A <code>ServiceReference</code> object may be shared between bundles and
 * can be used to examine the properties of the service and to get the service
 * object.
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
 * @see BundleContext#getServiceReference
 * @see BundleContext#getServiceReferences
 * @see BundleContext#getService
 * @ThreadSafe
 * @version $Revision: 6374 $
 */

public interface ServiceReference extends Comparable {
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
	public Object getProperty(String key);

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
	 * {@link BundleContext#registerService(String[],Object,Dictionary)} or
	 * {@link ServiceRegistration#setProperties} methods.
	 * 
	 * @return An array of property keys.
	 */
	public String[] getPropertyKeys();

	/**
	 * Returns the bundle that registered the service referenced by this
	 * <code>ServiceReference</code> object.
	 * 
	 * <p>
	 * This method must return <code>null</code> when the service has been
	 * unregistered. This can be used to determine if the service has been
	 * unregistered.
	 * 
	 * @return The bundle that registered the service referenced by this
	 *         <code>ServiceReference</code> object; <code>null</code> if that
	 *         service has already been unregistered.
	 * @see BundleContext#registerService(String[],Object,Dictionary)
	 */
	public Bundle getBundle();

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
	public Bundle[] getUsingBundles();

	/**
	 * Tests if the bundle that registered the service referenced by this
	 * <code>ServiceReference</code> and the specified bundle use the same
	 * source for the package of the specified class name.
	 * <p>
	 * This method performs the following checks:
	 * <ol>
	 * <li>Get the package name from the specified class name.</li>
	 * <li>For the bundle that registered the service referenced by this
	 * <code>ServiceReference</code> (registrant bundle); find the source for
	 * the package. If no source is found then return <code>true</code> if the
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
	 *         referenced by this <code>ServiceReference</code> and the
	 *         specified bundle use the same source for the package of the
	 *         specified class name. Otherwise <code>false</code> is returned.
	 * @throws IllegalArgumentException If the specified <code>Bundle</code> was
	 *         not created by the same framework instance as this
	 *         <code>ServiceReference</code>.
	 * @since 1.3
	 */
	public boolean isAssignableTo(Bundle bundle, String className);

	/**
	 * Compares this <code>ServiceReference</code> with the specified
	 * <code>ServiceReference</code> for order.
	 * 
	 * <p>
	 * If this <code>ServiceReference</code> and the specified
	 * <code>ServiceReference</code> have the same {@link Constants#SERVICE_ID
	 * service id} they are equal. This <code>ServiceReference</code> is less
	 * than the specified <code>ServiceReference</code> if it has a lower
	 * {@link Constants#SERVICE_RANKING service ranking} and greater if it has a
	 * higher service ranking. Otherwise, if this <code>ServiceReference</code>
	 * and the specified <code>ServiceReference</code> have the same
	 * {@link Constants#SERVICE_RANKING service ranking}, this
	 * <code>ServiceReference</code> is less than the specified
	 * <code>ServiceReference</code> if it has a higher
	 * {@link Constants#SERVICE_ID service id} and greater if it has a lower
	 * service id.
	 * 
	 * @param reference The <code>ServiceReference</code> to be compared.
	 * @return Returns a negative integer, zero, or a positive integer if this
	 *         <code>ServiceReference</code> is less than, equal to, or greater
	 *         than the specified <code>ServiceReference</code>.
	 * @throws IllegalArgumentException If the specified
	 *         <code>ServiceReference</code> was not created by the same
	 *         framework instance as this <code>ServiceReference</code>.
	 * @since 1.4
	 */
	public int compareTo(Object reference);
}
