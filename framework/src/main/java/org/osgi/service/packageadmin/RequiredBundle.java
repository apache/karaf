/*
 * Copyright (c) OSGi Alliance (2004, 2008). All Rights Reserved.
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

package org.osgi.service.packageadmin;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * A required bundle.
 * 
 * Objects implementing this interface are created by the Package Admin service.
 * 
 * <p>
 * The term <i>required bundle</i> refers to a resolved bundle that has a
 * bundle symbolic name and is not a fragment. That is, a bundle that may be
 * required by other bundles. This bundle may or may not be currently required
 * by other bundles.
 * 
 * <p>
 * The information about a required bundle provided by this object may change. A
 * <code>RequiredBundle</code> object becomes stale if an exported package of
 * the bundle it references has been updated or removed as a result of calling
 * <code>PackageAdmin.refreshPackages()</code>).
 * 
 * If this object becomes stale, its <code>getSymbolicName()</code> and
 * <code>getVersion()</code> methods continue to return their original values,
 * <code>isRemovalPending()</code> returns true, and <code>getBundle()</code>
 * and <code>getRequiringBundles()</code> return <code>null</code>.
 * 
 * @since 1.2
 * @ThreadSafe
 * @version $Revision: 5673 $
 */
public interface RequiredBundle {
	/**
	 * Returns the symbolic name of this required bundle.
	 * 
	 * @return The symbolic name of this required bundle.
	 */
	public String getSymbolicName();

	/**
	 * Returns the bundle associated with this required bundle.
	 * 
	 * @return The bundle, or <code>null</code> if this
	 *         <code>RequiredBundle</code> object has become stale.
	 */
	public Bundle getBundle();

	/**
	 * Returns the bundles that currently require this required bundle.
	 * 
	 * <p>
	 * If this required bundle is required and then re-exported by another
	 * bundle then all the requiring bundles of the re-exporting bundle are
	 * included in the returned array.
	 * 
	 * @return An array of bundles currently requiring this required bundle, or
	 *         <code>null</code> if this <code>RequiredBundle</code> object
	 *         has become stale. The array will be empty if no bundles require
	 *         this required package.
	 */
	public Bundle[] getRequiringBundles();

	/**
	 * Returns the version of this required bundle.
	 * 
	 * @return The version of this required bundle, or
	 *         {@link Version#emptyVersion} if no version information is
	 *         available.
	 */
	public Version getVersion();

	/**
	 * Returns <code>true</code> if the bundle associated with this
	 * <code>RequiredBundle</code> object has been updated or uninstalled.
	 * 
	 * @return <code>true</code> if the required bundle has been updated or
	 *         uninstalled, or if the <code>RequiredBundle</code> object has
	 *         become stale; <code>false</code> otherwise.
	 */
	public boolean isRemovalPending();
}
