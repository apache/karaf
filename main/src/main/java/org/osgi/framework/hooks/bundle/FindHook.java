/*
 * Copyright (c) OSGi Alliance (2011). All Rights Reserved.
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
package org.osgi.framework.hooks.bundle;

import java.util.Collection;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * OSGi Framework Bundle Context Hook Service.
 * 
 * <p>
 * Bundles registering this service will be called during framework bundle find
 * (get bundles) operations.
 * 
 * @ThreadSafe
 * @version $Id: 4492a677df650072fe6acaea9ea35571f31eb5a9 $
 */
public interface FindHook {
	/**
	 * Find hook method. This method is called for the following:
	 * <ul>
	 * <li>Bundle find operations using {@link BundleContext#getBundle(long)}
	 * and {@link BundleContext#getBundles()} methods. The find method can
	 * filter the result of the find operation. Note that a find operation using
	 * the {@link BundleContext#getBundle(String)} method does not cause the
	 * find method to be called.</li>
	 * <li>Bundle install operations when an existing bundle is already
	 * installed at a given location. In this case, the find method is called to
	 * determine if the context performing the install operation is able to find
	 * the bundle. If the context cannot find the existing bundle then the
	 * install operation must fail with a
	 * {@link BundleException#REJECTED_BY_HOOK} exception.</li>
	 * </ul>
	 * 
	 * @param context
	 *            The bundle context of the bundle performing the find
	 *            operation.
	 * @param bundles
	 *            A collection of Bundles to be returned as a result of the find
	 *            operation. The implementation of this method may remove
	 *            bundles from the collection to prevent the bundles from being
	 *            returned to the bundle performing the find operation. The
	 *            collection supports all the optional {@code Collection}
	 *            operations except {@code add} and {@code addAll}. Attempting
	 *            to add to the collection will result in an
	 *            {@code UnsupportedOperationException}. The collection is not
	 *            synchronized.
	 */
	void find(BundleContext context, Collection<Bundle> bundles);
}
