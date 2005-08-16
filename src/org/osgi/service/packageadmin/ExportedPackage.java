/*
 * $Header: /cvshome/build/org.osgi.service.packageadmin/src/org/osgi/service/packageadmin/ExportedPackage.java,v 1.8 2005/05/13 20:32:34 hargrave Exp $
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
 * An exported package.
 * 
 * Instances implementing this interface are created by the Package Admin
 * service.
 * 
 * <p>
 * The information about an exported package provided by this object is valid
 * only until the next time <code>PackageAdmin.refreshPackages()</code> is called.
 * If an <code>ExportedPackage</code> object becomes stale (that is, the package
 * it references has been updated or removed as a result of calling
 * <code>PackageAdmin.refreshPackages()</code>), its <code>getName()</code> and
 * <code>getSpecificationVersion()</code> continue to return their old values,
 * <code>isRemovalPending()</code> returns <code>true</code>, and
 * <code>getExportingBundle()</code> and <code>getImportingBundles()</code> return
 * <code>null</code>.
 * 
 * @version $Revision: 1.8 $
 */
public interface ExportedPackage {
	/**
	 * Returns the name of the package associated with this
	 * <code>ExportedPackage</code> object.
	 * 
	 * @return The name of this <code>ExportedPackage</code> object.
	 */
	public String getName();

	/**
	 * Returns the bundle exporting the package associated with this
	 * <code>ExportedPackage</code> object.
	 * 
	 * @return The exporting bundle, or <code>null</code> if this
	 *         <code>ExportedPackage</code> object has become stale.
	 */
	public Bundle getExportingBundle();

	/**
	 * Returns the resolved bundles that are currently importing the package
	 * associated with this <code>ExportedPackage</code> object.
	 * 
	 * <p>
	 * Bundles which require the exporting bundle associated with this
	 * <code>ExportedPackage</code> object are considered to be importing bundles
	 * and are included in the returned array. See
	 * {@link RequiredBundle#getRequiringBundles()}
	 * 
	 * @return The array of resolved bundles currently importing the package
	 *         associated with this <code>ExportedPackage</code> object, or
	 *         <code>null</code> if this <code>ExportedPackage</code> object has
	 *         become stale.
	 */
	public Bundle[] getImportingBundles();

	/**
	 * Returns the specification version of this <code>ExportedPackage</code>, as
	 * specified in the exporting bundle's manifest file.
	 * 
	 * @return The specification version of this <code>ExportedPackage</code>
	 *         object, or <code>null</code> if no version information is
	 *         available.
	 */
	public String getSpecificationVersion();

	/**
	 * Returns <code>true</code> if the package associated with this
	 * <code>ExportedPackage</code> object has been exported by a bundle that has
	 * been updated or uninstalled.
	 * 
	 * @return <code>true</code> if the associated package is being exported by a
	 *         bundle that has been updated or uninstalled, or if this
	 *         <code>ExportedPackage</code> object has become stale;
	 *         <code>false</code> otherwise.
	 */
	public boolean isRemovalPending();
}