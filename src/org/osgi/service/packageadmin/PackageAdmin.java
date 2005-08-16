/*
 * $Header: /cvshome/build/org.osgi.service.packageadmin/src/org/osgi/service/packageadmin/PackageAdmin.java,v 1.10 2005/05/13 20:32:34 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2001, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.service.packageadmin;

import org.osgi.framework.Bundle;

/**
 * Framework service which allows bundle programmers to inspect the packages
 * exported in the Framework and eagerly update or uninstall bundles.
 * 
 * If present, there will only be a single instance of this service registered
 * with the Framework.
 * 
 * <p>
 * The term <i>exported package </i> (and the corresponding interface
 * {@link ExportedPackage})refers to a package that has actually been exported
 * (as opposed to one that is available for export).
 * 
 * <p>
 * The information about exported packages returned by this service is valid
 * only until the next time {@link #refreshPackages}is called. If an
 * <code>ExportedPackage</code> object becomes stale, (that is, the package it
 * references has been updated or removed as a result of calling
 * <code>PackageAdmin.refreshPackages()</code>), its <code>getName()</code> and
 * <code>getSpecificationVersion()</code> continue to return their old values,
 * <code>isRemovalPending()</code> returns <code>true</code>, and
 * <code>getExportingBundle()</code> and <code>getImportingBundles()</code> return
 * <code>null</code>.
 * 
 * @version $Revision: 1.10 $
 */
public interface PackageAdmin {
	/**
	 * Gets the packages exported by the specified bundle.
	 * 
	 * @param bundle The bundle whose exported packages are to be returned, or
	 *        <code>null</code> if all the packages currently exported in the
	 *        Framework are to be returned. If the specified bundle is the
	 *        system bundle (that is, the bundle with id zero), this method
	 *        returns all the packages on the system classpath whose name does
	 *        not start with "java.". In an environment where the exhaustive
	 *        list of packages on the system classpath is not known in advance,
	 *        this method will return all currently known packages on the system
	 *        classpath, that is, all packages on the system classpath that
	 *        contains one or more classes that have been loaded.
	 * 
	 * @return The array of packages exported by the specified bundle, or
	 *         <code>null</code> if the specified bundle has not exported any
	 *         packages.
	 */
	public ExportedPackage[] getExportedPackages(Bundle bundle);

	/**
	 * Gets the <code>ExportedPackage</code> object with the specified package
	 * name. All exported packages will be checked for the specified name. The
	 * exported package with the highest version will be returned.
	 * <p>
	 * In an environment where the exhaustive list of packages on the system
	 * classpath is not known in advance, this method attempts to see if the
	 * named package is on the system classpath. This means that this method may
	 * discover an <code>ExportedPackage</code> object that was not present in the
	 * list returned by a prior call to <code>getExportedPackages()</code>.
	 * 
	 * @param name The name of the exported package to be returned.
	 * 
	 * @return The exported package with the specified name, or <code>null</code>
	 *         if no exported packages with that name exists.
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
	 * following steps in its own thread:
	 * 
	 * <ol>
	 * <li>Compute a graph of bundles starting with the specified bundles. If
	 * no bundles are specified, compute a graph of bundles starting with
	 * previously updated or uninstalled ones. Add to the graph any bundle that
	 * imports a package that is currently exported by a bundle in the graph.
	 * The graph is fully constructed when there is no bundle outside the graph
	 * that imports a package from a bundle in the graph. The graph may contain
	 * <code>UNINSTALLED</code> bundles that are currently still exporting
	 * packages.
	 * 
	 * <li>Each bundle in the graph that is in the <code>ACTIVE</code> state will
	 * be stopped as described in the <code>Bundle.stop</code> method.
	 * 
	 * <li>Each bundle in the graph that is in the <code>RESOLVED</code> state is
	 * moved to the <code>INSTALLED</code> state. The effect of this step is that
	 * bundles in the graph are no longer <code>RESOLVED</code>.
	 * 
	 * <li>Each bundle in the graph that is in the <code>UNINSTALLED</code> state
	 * is removed from the graph and is now completely removed from the
	 * Framework.
	 * 
	 * <li>Each bundle in the graph that was in the <code>ACTIVE</code> state
	 * prior to Step 2 is started as described in the <code>Bundle.start</code>
	 * method, causing all bundles required for the restart to be resolved. It
	 * is possible that, as a result of the previous steps, packages that were
	 * previously exported no longer are. Therefore, some bundles may be
	 * unresolvable until another bundle offering a compatible package for
	 * export has been installed in the Framework.
	 * <li>A framework event of type <code>FrameworkEvent.PACKAGES_REFRESHED</code>
	 * is broadcast.
	 * </ol>
	 * 
	 * <p>
	 * For any exceptions that are thrown during any of these steps, a
	 * <code>FrameworkEvent</code> of type <code>ERROR</code> is broadcast,
	 * containing the exception. The source bundle for these events should be
	 * the specific bundle to which the exception is related. If no specific
	 * bundle can be associated with the exception then the System Bundle must
	 * be used as the source bundle for the event.
	 * 
	 * @param bundles the bundles whose exported packages are to be updated or
	 *        removed, or <code>null</code> for all previously updated or
	 *        uninstalled bundles.
	 * 
	 * @exception SecurityException if the caller does not have the
	 *            <code>AdminPermission</code> and the Java runtime environment
	 *            supports permissions.
	 */
	public void refreshPackages(Bundle[] bundles);

	/**
	 * Get the <code>ExportedPackage</code> objects with the specified
	 * package name. All exported packages will be checked for the specified
	 * name.
	 * <p>
	 * In an environment where the exhaustive list of packages on the system
	 * classpath is not known in advance, this method attempts to see if the
	 * named package is on the system classpath. This means that this method may
	 * discover an <code>ExportedPackage</code> object that was not present in the
	 * list returned by a prior call to <code>getExportedPackages()</code>.
	 * 
	 * @param name The name of the exported packages to be returned.
	 * 
	 * @return An array of the exported packages with the specified name, or
	 *         <code>null</code> if no exported packages with that name exists.
	 * @since 1.2
	 */
	public ExportedPackage[] getExportedPackages(String name);

	/**
	 * Resolve the specified bundles. The Framework must attempt to resolve the
	 * specified bundles that are unresolved. Additional bundles that are not
	 * included in the specified bundles may be resolved as a result of calling
	 * this method. A permissible implementation of this method is to attempt to
	 * resolve all unresolved bundles installed in the framework.
	 * 
	 * <p>
	 * If <code>null</code> is specified then the Framework will attempt to
	 * resolve all unresolved bundles. This method must not cause any bundle to
	 * be refreshed, stopped, or started. This method will not return until the
	 * operation has completed.
	 * 
	 * @param bundles The bundles to resolve or <code>null</code> to resolve all
	 *        unresolved bundles installed in the Framework.
	 * @return <code>true</code> if all specified bundles are resolved;
	 * @since 1.2
	 */
	public boolean resolveBundles(Bundle[] bundles);

	/**
	 * Returns an array of RequiredBundles with the specified symbolic name. If
	 * the symbolic name argument is <code>null</code> then all RequiredBundles
	 * are returned.
	 * 
	 * @param symbolicName The symbolic name of the RequiredBundle or
	 *        <code>null</code> for all RequiredBundles in the Framework.
	 * @return An array of RequiredBundles with the specified symbolic name or
	 *         <code>null</code> if no RequiredBundles exist with that symbolic
	 *         name.
	 * @since 1.2
	 */
	public RequiredBundle[] getRequiredBundles(String symbolicName);

	/**
	 * Returns the bundles with the specified symbolic name within the specified
	 * version range. If no bundles are installed that have the specified
	 * symbolic name, then <code>null</code> is returned. If a version range is
	 * specified, then only the bundles that have the specified symbolic name
	 * and belong to the specified version range are returned. The returned
	 * bundles are ordered by version in descending version order so that the
	 * first element of the array contains the bundle with the highest version.
	 * 
	 * @see org.osgi.framework.Constants#BUNDLE_VERSION_ATTRIBUTE
	 * @param symbolicName The symbolic name of the desired bundles.
	 * @param versionRange The version range of the desired bundles, or
	 *        <code>null</code> if all versions are desired.
	 * @return An array of bundles with the specified name belonging to the
	 *         specified version range ordered in descending version order, or
	 *         <code>null</code> if no bundles are found.
	 * @since 1.2
	 */
	public Bundle[] getBundles(String symbolicName, String versionRange);

	/**
	 * Returns an array of attached fragment bundles for the specified bundle.
	 * If the specified bundle is a fragment then <code>null</code> is returned.
	 * If no fragments are attached to the specified bundle then <code>null</code>
	 * is returned.
	 * <p>
	 * This method does not attempt to resolve the specified bundle.  If the 
	 * specified bundle is not resolved then <code>null</code> is returned. 
	 * 
	 * @param bundle The bundle whose attached fragment bundles are to be
	 *        returned.
	 * @return An array of fragment bundles or <code>null</code> if the bundle
	 *         does not have any attached fragment bundles or the bundle is not
	 *         resolved.
	 * @since 1.2
	 */
	public Bundle[] getFragments(Bundle bundle);

	/**
	 * Returns an array of host bundles to which the specified fragment bundle
	 * is attached or <code>null</code> if the specified bundle is not attached to
	 * a host or is not a fragment bundle.
	 * 
	 * @param bundle The bundle whose host bundles are to be returned.
	 * @return An array of host bundles or <code>null</code> if the bundle does
	 *         not have any host bundles.
	 * @since 1.2
	 */
	public Bundle[] getHosts(Bundle bundle);

	/**
	 * Returns the bundle for which the specified class is loaded from. The
	 * classloader of the bundle returned must have been used to load the
	 * specified class. If the class was not loaded by a bundle classloader then
	 * <code>null</code> is returned.
	 * 
	 * @param clazz the class object to get a bundle for
	 * @return the bundle from which the specified class is loaded or
	 *         <code>null</code> if the class was not loaded by a bundle
	 *         classloader
	 * @since 1.2
	 */
	public Bundle getBundle(Class clazz);

	/**
	 * The bundle is a fragment bundle.
	 * 
	 * <p>
	 * The value of <code>BUNDLE_TYPE_FRAGMENT</code> is 0x00000001.
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
	 * @return The special type of the bundle.
	 * @since 1.2
	 */
	public int getBundleType(Bundle bundle);
}