/*
 * $Header: /cvshome/build/org.osgi.service.packageadmin/src/org/osgi/service/packageadmin/RequiredBundle.java,v 1.5 2005/05/13 20:32:34 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2004, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.service.packageadmin;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * A required bundle.
 * 
 * Instances implementing this interface are created by the Package Admin
 * service.
 * 
 * <p>
 * The information about a <code>RequiredBundle</code> provided by this object is
 * valid only until the next time <code>PackageAdmin.refreshPackages()</code>
 * called. If a <code>RequiredBundle</code> object becomes stale (that is, the
 * bundle it references has been updated or removed as a result of calling
 * <code>PackageAdmin.refreshPackages()</code>), its <code>getSymbolicName()</code>
 * and <code>getVersion()</code> continue to return their old values,
 * <code>isRemovalPending()</code> returns true, and <code>getBundle()</code> and
 * <code>getRequiringBundles()</code> return <code>null</code>.
 * 
 * @since 1.2
 */
public interface RequiredBundle {
	/**
	 * Returns the bundle which defines this RequiredBundle.
	 * 
	 * @return The bundle, or <code>null</code> if this <code>RequiredBundle</code>
	 *         object has become stale.
	 */
	public Bundle getBundle();

	/**
	 * Returns the resolved bundles that currently require this bundle. If this
	 * <code>RequiredBundle</code> object is required and re-exported by another
	 * bundle then all the requiring bundles of the re-exporting bundle are
	 * included in the returned array.
	 * 
	 * @return An array of resolved bundles currently requiring this bundle, or
	 *         <code>null</code> if this <code>RequiredBundle</code> object has
	 *         become stale.
	 */
	public Bundle[] getRequiringBundles();

	/**
	 * Returns the symbolic name of the bundle.
	 * 
	 * @return The symbolic name of the bundle.
	 */
	public String getSymbolicName();

	/**
	 * Returns the version of the bundle.
	 * 
	 * @return The version of the bundle.
	 */
	public Version getVersion();

	/**
	 * Returns <code>true</code> if the bundle has been updated or uninstalled.
	 * 
	 * @return <code>true</code> if the bundle has been updated or uninstalled, or
	 *         if the <code>RequiredBundle</code> object has become stale;
	 *         <code>false</code> otherwise.
	 */
	public boolean isRemovalPending();
}