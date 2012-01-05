/*
 * Copyright (c) OSGi Alliance (2001, 2010). All Rights Reserved.
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

/**
 * Framework service which allows bundle programmers to inspect the package
 * wiring state of bundles in the Framework as well as other functions related
 * to the class loader network among bundles.
 * 
 * <p>
 * If present, there will only be a single instance of this service registered
 * with the Framework.
 * 
 * @ThreadSafe
 * @noimplement
 * @version $Id: a268c3bdc986080fa16bdb2f56ba1d3800d030dd $
 * @deprecated This service has been replaced by the
 *             <code>org.osgi.framework.wiring</code> package.
 * @see org.osgi.service.packageadmin.ExportedPackage
 * @see org.osgi.service.packageadmin.RequiredBundle
 */
public interface PackageAdmin {
	/**
	 * Gets the exported packages for the specified bundle.
	 * 
	 * @param bundle The bundle whose exported packages are to be returned, or
	 *        {@code null} if all exported packages are to be returned. If
	 *        the specified bundle is the system bundle (that is, the bundle
	 *        with id zero), this method returns all the packages known to be
	 *        exported by the system bundle. This will include the package
	 *        specified by the {@code org.osgi.framework.system.packages}
	 *        system property as well as any other package exported by the
	 *        framework implementation.
	 * 
	 * @return An array of exported packages, or {@code null} if the
	 *         specified bundle has no exported packages.
	 * @throws IllegalArgumentException If the specified {@code Bundle} was
	 *         not created by the same framework instance that registered this
	 *         {@code PackageAdmin} service.
	 */
	public ExportedPackage[] getExportedPackages(Bundle bundle);

	/**
	 * Gets the exported packages for the specified package name.
	 * 
	 * @param name The name of the exported packages to be returned.
	 * 
	 * @return An array of the exported packages, or {@code null} if no
	 *         exported packages with the specified name exists.
	 * @since 1.2
	 */
	public ExportedPackage[] getExportedPackages(String name);

	/**
	 * Gets the exported package for the specified package name.
	 * 
	 * <p>
	 * If there are multiple exported packages with specified name, the exported
	 * package with the highest version will be returned.
	 * 
	 * @param name The name of the exported package to be returned.
	 * 
	 * @return The exported package, or {@code null} if no exported
	 *         package with the specified name exists.
	 * @see #getExportedPackages(String)
	 */
	public ExportedPackage getExportedPackage(String name);

	/**
	 * Forces the update (replacement) or removal of packages exported by the
	 * specified bundles.
	 * 
	 * <p>
	 * If no bundles are specified, this method will update or remove any
	 * packages exported by any bundles that were previously updated or
	 * uninstalled since the last call to this method. The technique by which
	 * this is accomplished may vary among different Framework implementations.
	 * One permissible implementation is to stop and restart the Framework.
	 * 
	 * <p>
	 * This method returns to the caller immediately and then performs the
	 * following steps on a separate thread:
	 * 
	 * <ol>
	 * <li>Compute a graph of bundles starting with the specified bundles. If no
	 * bundles are specified, compute a graph of bundles starting with bundle
	 * updated or uninstalled since the last call to this method. Add to the
	 * graph any bundle that is wired to a package that is currently exported by
	 * a bundle in the graph. The graph is fully constructed when there is no
	 * bundle outside the graph that is wired to a bundle in the graph. The
	 * graph may contain {@code UNINSTALLED} bundles that are currently
	 * still exporting packages.
	 * 
	 * <li>Each bundle in the graph that is in the {@code ACTIVE} state
	 * will be stopped as described in the {@code Bundle.stop} method.
	 * 
	 * <li>Each bundle in the graph that is in the {@code RESOLVED} state
	 * is unresolved and thus moved to the {@code INSTALLED} state. The
	 * effect of this step is that bundles in the graph are no longer
	 * {@code RESOLVED}.
	 * 
	 * <li>Each bundle in the graph that is in the {@code UNINSTALLED}
	 * state is removed from the graph and is now completely removed from the
	 * Framework.
	 * 
	 * <li>Each bundle in the graph that was in the {@code ACTIVE} state
	 * prior to Step 2 is started as described in the {@code Bundle.start}
	 * method, causing all bundles required for the restart to be resolved. It
	 * is possible that, as a result of the previous steps, packages that were
	 * previously exported no longer are. Therefore, some bundles may be
	 * unresolvable until another bundle offering a compatible package for
	 * export has been installed in the Framework.
	 * <li>A framework event of type
	 * {@code FrameworkEvent.PACKAGES_REFRESHED} is fired.
	 * </ol>
	 * 
	 * <p>
	 * For any exceptions that are thrown during any of these steps, a
	 * {@code FrameworkEvent} of type {@code ERROR} is fired
	 * containing the exception. The source bundle for these events should be
	 * the specific bundle to which the exception is related. If no specific
	 * bundle can be associated with the exception then the System Bundle must
	 * be used as the source bundle for the event.
	 * 
	 * @param bundles The bundles whose exported packages are to be updated or
	 *        removed, or {@code null} for all bundles updated or
	 *        uninstalled since the last call to this method.
	 * @throws SecurityException If the caller does not have
	 *         {@code AdminPermission[System Bundle,RESOLVE]} and the Java
	 *         runtime environment supports permissions.
	 * @throws IllegalArgumentException If the specified {@code Bundle}s
	 *         were not created by the same framework instance that registered
	 *         this {@code PackageAdmin} service.
	 */
	public void refreshPackages(Bundle[] bundles);

	/**
	 * Resolve the specified bundles. The Framework must attempt to resolve the
	 * specified bundles that are unresolved. Additional bundles that are not
	 * included in the specified bundles may be resolved as a result of calling
	 * this method. A permissible implementation of this method is to attempt to
	 * resolve all unresolved bundles installed in the framework.
	 * 
	 * <p>
	 * If {@code null} is specified then the Framework will attempt to
	 * resolve all unresolved bundles. This method must not cause any bundle to
	 * be refreshed, stopped, or started. This method will not return until the
	 * operation has completed.
	 * 
	 * @param bundles The bundles to resolve or {@code null} to resolve all
	 *        unresolved bundles installed in the Framework.
	 * @return {@code true} if all specified bundles are resolved;
	 * @throws SecurityException If the caller does not have
	 *         {@code AdminPermission[System Bundle,RESOLVE]} and the Java
	 *         runtime environment supports permissions.
	 * @throws IllegalArgumentException If the specified {@code Bundle}s
	 *         were not created by the same framework instance that registered
	 *         this {@code PackageAdmin} service.
	 * @since 1.2
	 */
	public boolean resolveBundles(Bundle[] bundles);

	/**
	 * Returns an array of required bundles having the specified symbolic name.
	 * 
	 * <p>
	 * If {@code null} is specified, then all required bundles will be
	 * returned.
	 * 
	 * @param symbolicName The bundle symbolic name or {@code null} for
	 *        all required bundles.
	 * @return An array of required bundles or {@code null} if no
	 *         required bundles exist for the specified symbolic name.
	 * @since 1.2
	 */
	public RequiredBundle[] getRequiredBundles(String symbolicName);

	/**
	 * Returns the bundles with the specified symbolic name whose bundle version
	 * is within the specified version range. If no bundles are installed that
	 * have the specified symbolic name, then {@code null} is returned.
	 * If a version range is specified, then only the bundles that have the
	 * specified symbolic name and whose bundle versions belong to the specified
	 * version range are returned. The returned bundles are ordered by version
	 * in descending version order so that the first element of the array
	 * contains the bundle with the highest version.
	 * 
	 * @see org.osgi.framework.Constants#BUNDLE_VERSION_ATTRIBUTE
	 * @param symbolicName The symbolic name of the desired bundles.
	 * @param versionRange The version range of the desired bundles, or
	 *        {@code null} if all versions are desired.
	 * @return An array of bundles with the specified name belonging to the
	 *         specified version range ordered in descending version order, or
	 *         {@code null} if no bundles are found.
	 * @since 1.2
	 */
	public Bundle[] getBundles(String symbolicName, String versionRange);

	/**
	 * Returns an array of attached fragment bundles for the specified bundle.
	 * If the specified bundle is a fragment then {@code null} is returned.
	 * If no fragments are attached to the specified bundle then
	 * {@code null} is returned.
	 * <p>
	 * This method does not attempt to resolve the specified bundle. If the
	 * specified bundle is not resolved then {@code null} is returned.
	 * 
	 * @param bundle The bundle whose attached fragment bundles are to be
	 *        returned.
	 * @return An array of fragment bundles or {@code null} if the bundle
	 *         does not have any attached fragment bundles or the bundle is not
	 *         resolved.
	 * @throws IllegalArgumentException If the specified {@code Bundle} was
	 *         not created by the same framework instance that registered this
	 *         {@code PackageAdmin} service.
	 * @since 1.2
	 */
	public Bundle[] getFragments(Bundle bundle);

	/**
	 * Returns the host bundles to which the specified fragment bundle is
	 * attached.
	 * 
	 * @param bundle The fragment bundle whose host bundles are to be returned.
	 * @return An array containing the host bundles to which the specified
	 *         fragment is attached or {@code null} if the specified bundle
	 *         is not a fragment or is not attached to any host bundles.
	 * @throws IllegalArgumentException If the specified {@code Bundle} was
	 *         not created by the same framework instance that registered this
	 *         {@code PackageAdmin} service.
	 * @since 1.2
	 */
	public Bundle[] getHosts(Bundle bundle);

	/**
	 * Returns the bundle from which the specified class is loaded. The class
	 * loader of the returned bundle must have been used to load the specified
	 * class. If the class was not loaded by a bundle class loader then
	 * {@code null} is returned.
	 * 
	 * @param clazz The class object from which to locate the bundle.
	 * @return The bundle from which the specified class is loaded or
	 *         {@code null} if the class was not loaded by a bundle class
	 *         loader created by the same framework instance that registered
	 *         this {@code PackageAdmin} service.
	 * @since 1.2
	 */
	public Bundle getBundle(Class clazz);

	/**
	 * Bundle type indicating the bundle is a fragment bundle.
	 * 
	 * <p>
	 * The value of {@code BUNDLE_TYPE_FRAGMENT} is 0x00000001.
	 * 
	 * @since 1.2
	 */
	public static final int	BUNDLE_TYPE_FRAGMENT	= 0x00000001;

	/**
	 * Returns the special type of the specified bundle. The bundle type values
	 * are:
	 * <ul>
	 * <li>{@link #BUNDLE_TYPE_FRAGMENT}
	 * </ul>
	 * 
	 * A bundle may be more than one type at a time. A type code is used to
	 * identify the bundle type for future extendability.
	 * 
	 * <p>
	 * If a bundle is not one or more of the defined types then 0x00000000 is
	 * returned.
	 * 
	 * @param bundle The bundle for which to return the special type.
	 * @return The special type of the bundle.
	 * @throws IllegalArgumentException If the specified {@code Bundle} was
	 *         not created by the same framework instance that registered this
	 *         {@code PackageAdmin} service.
	 * @since 1.2
	 */
	public int getBundleType(Bundle bundle);
}
