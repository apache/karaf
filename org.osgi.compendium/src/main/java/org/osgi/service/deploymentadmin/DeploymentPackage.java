/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
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

package org.osgi.service.deploymentadmin;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * The <code>DeploymentPackage</code> object represents a deployment package
 * (already installed or being currently processed). A Deployment Package groups
 * resources as a unit of management. A deployment package is something that can
 * be installed, updated, and uninstalled as a unit. A deployment package is a
 * reified concept, like a bundle, in an OSGi Service Platform. It is not known
 * by the OSGi Framework, but it is managed by the Deployment Admin service. A
 * deployment package is a stream of resources (including bundles) which, once
 * processed, will result in new artifacts (effects on the system) being added
 * to the OSGi platform. These new artifacts can include installed Bundles, new
 * configuration objects added to the Configuration Admin service, new Wire
 * objects added to the Wire Admin service, or changed system properties, etc.
 * All the changes caused by the processing of a deployment package are
 * persistently associated with the deployment package, so that they can be
 * appropriately cleaned up when the deployment package is uninstalled. There is
 * a strict no overlap rule imposed on deployment packages. Two deployment
 * packages are not allowed to create or manipulate the same artifact.
 * Obviously, this means that a bundle cannot be in two different deployment
 * packages. Any violation of this no overlap rule is considered an error and
 * the install or update of the offending deployment package must be aborted.
 * <p>
 * 
 * The Deployment Admin service should do as much as possible to ensure
 * transactionality. It means that if a deployment package installation, update
 * or removal (uninstall) fails all the side effects caused by the process
 * should be disappeared and the system should be in the state in which it was
 * before the process.
 * <p>
 * 
 * If a deployment package is being updated the old version is visible through
 * the <code>DeploymentPackage</code> interface until the update process ends.
 * After the package is updated the updated version is visible and the old one
 * is not accessible any more.
 */
public interface DeploymentPackage {
	/**
	 * The name of the Deployment Package. This name is the same name as that
	 * specified in the DeploymentPackage-SymbolicName Manifest header.
	 * 
	 * @since 1.1
	 */
	String EVENT_DEPLOYMENTPACKAGE_NAME = "deploymentpackage.name";

	/**
	 * The human readable name of the DP localized to the default locale.
	 * 
	 * @since 1.1
	 */
	String EVENT_DEPLOYMENTPACKAGE_READABLENAME = "deploymentpackage.readablename";

	/**
	 * The currently installed version of the Deployment Package. The attribute
	 * is not present, if no version is installed:
	 * <ul>
	 * <li>in the INSTALL event, when an installDeploymentPackage was called and
	 * no earlier version is present
	 * <li>in the COMPLETE event after the _successfully_ completing an
	 * uninstallDeploymentPackage call
	 * </ul>
	 * The value for this event must be a Version object.
	 * 
	 * @since 1.1
	 */
	String EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION = "deploymentpackage.currentversion";

	/**
	 * The version of DP after the successful completion of the install
	 * operation (used in INSTALL event only).
	 * 
	 * The value for this event must be a Version object.
	 * 
	 * @since 1.1
	 */
	String EVENT_DEPLOYMENTPACKAGE_NEXTVERSION = "deploymentpackage.nextversion";

	/**
	 * Gives back the state of the deployment package whether it is stale or
	 * not). After uninstall of a deployment package it becomes stale. Any
	 * active method calls to a stale deployment package raise
	 * {@link IllegalStateException}. Active methods are the following:
	 * <p>
	 * 
	 * <ul>
	 * <li>{@link #getBundle(String)}</li>
	 * <li>{@link #getResourceProcessor(String)}</li>
	 * <li>{@link #uninstall()}</li>
	 * <li>{@link #uninstallForced()}</li>
	 * </ul>
	 * 
	 * @return <code>true</code> if the deployment package is stale.
	 *         <code>false</code> otherwise
	 * @see #uninstall
	 * @see #uninstallForced
	 */
	boolean isStale();

	/**
	 * Returns the Deployment Package Symbolic Name of the package.
	 * 
	 * @return The name of the deployment package. It cannot be null.
	 */
	String getName();

	/**
	 * Returns the Deployment Package human readable name.
	 * 
	 * This method returns the localized human readable name as set with the
	 * <code>DeploymentPackage-Name</code> manifest header using the default
	 * locale. If no header is set, this method will return <code>null</code>.
	 * 
	 * @return The human readable name of the deployment package or
	 *         <code>null</code> if header is not set.
	 * @since 1.1
	 */
	String getDisplayName();

	/**
	 * Returns the version of the deployment package.
	 * 
	 * @return version of the deployment package. It cannot be null.
	 */
	Version getVersion();

	/**
	 * Returns an array of {@link BundleInfo} objects representing the bundles
	 * specified in the manifest of this deployment package. Its size is equal
	 * to the number of the bundles in the deployment package.
	 * 
	 * @return array of <code>BundleInfo</code> objects
	 * @throws SecurityException
	 *             if the caller doesn't have the appropriate
	 *             {@link DeploymentAdminPermission} with "metadata" action
	 */
	BundleInfo[] getBundleInfos();

	/**
	 * Returns a URL pointing to an image that represents the icon for this
	 * Deployment Package.
	 * 
	 * The <code>DeploymentPackage-Icon</code> header can set an icon for the
	 * the deployment package. This method returns an absolute URL that is
	 * defined by this header. The Deployment Admin service must provide this
	 * icon as a local resource. That is, the Deployment Admin must make a local
	 * copy of the specified icon. The returned <code>URL</code>'s must point to
	 * a local resource.
	 * 
	 * @return An absolute URL to a local (device resident) image resource or
	 *         <code>null</code> if not found
	 * @since 1.1
	 */
	URL getIcon();

	/**
	 * Returns the bundle instance, which is part of this deployment package,
	 * that corresponds to the bundle's symbolic name passed in the
	 * <code>symbolicName</code> parameter. This method will return null for
	 * request for bundles that are not part of this deployment package.
	 * <p>
	 * 
	 * As this instance is transient (i.e. a bundle can be removed at any time
	 * because of the dynamic nature of the OSGi platform), this method may also
	 * return null if the bundle is part of this deployment package, but is not
	 * currently defined to the framework.
	 * 
	 * @param symbolicName
	 *            the symbolic name of the requested bundle
	 * @return The <code>Bundle</code> instance for a given bundle symbolic
	 *         name.
	 * @throws SecurityException
	 *             if the caller doesn't have the appropriate
	 *             {@link DeploymentAdminPermission} with "metadata" action
	 * @throws IllegalStateException
	 *             if the package is stale
	 */
	Bundle getBundle(String symbolicName);

	/**
	 * Returns an array of strings representing the resources (including
	 * bundles) that are specified in the manifest of this deployment package. A
	 * string element of the array is the same as the value of the "Name"
	 * attribute in the manifest. The array contains the bundles as well.
	 * <p>
	 * 
	 * E.g. if the "Name" section of the resource (or individual-section as the
	 * <a
	 * href="http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#Manifest%20Specification">Manifest
	 * Specification</a> calls it) in the manifest is the following
	 * 
	 * <pre>
	 *     Name: foo/readme.txt
	 *     Resource-Processor: foo.rp
	 * </pre>
	 * 
	 * then the corresponding array element is the "foo/readme.txt" string.
	 * <p>
	 * 
	 * @return The string array corresponding to resources. It cannot be null
	 *         but its length can be zero.
	 * @throws SecurityException
	 *             if the caller doesn't have the appropriate
	 *             {@link DeploymentAdminPermission} with "metadata" action
	 */
	String[] getResources();

	/**
	 * At the time of deployment, resource processor service instances are
	 * located to resources contained in a deployment package.
	 * <p>
	 * 
	 * This call returns a service reference to the corresponding service
	 * instance. If the resource is not part of the deployment package or this
	 * call is made during deployment, prior to the locating of the service to
	 * process a given resource, null will be returned. Services can be updated
	 * after a deployment package has been deployed. In this event, this call
	 * will return a reference to the updated service, not to the instance that
	 * was used at deployment time.
	 * 
	 * @param resource
	 *            the name of the resource (it is the same as the value of the
	 *            "Name" attribute in the deployment package's manifest)
	 * @return resource processor for the resource or <code>null</code>.
	 * @throws SecurityException if the caller doesn't have the appropriate {@link DeploymentAdminPermission} 
	 *         with "metadata" action
	 * @throws IllegalStateException if the package is stale
	 */
	ServiceReference getResourceProcessor(String resource);

	/**
	 * Returns the requested deployment package manifest header from the main
	 * section. Header names are case insensitive. If the header doesn't exist
	 * it returns null.
	 * <p>
	 * 
	 * If the header is localized then the localized value is returned (see OSGi
	 * Service Platform, Mobile Specification Release 4 - Localization related
	 * chapters).
	 * 
	 * @param header
	 *            the requested header
	 * @return the value of the header or <code>null</code> if the header does
	 *         not exist
	 * @throws SecurityException
	 *             if the caller doesn't have the appropriate
	 *             {@link DeploymentAdminPermission} with "metadata" action
	 */
	String getHeader(String header);

	/**
	 * Returns the requested deployment package manifest header from the name
	 * section determined by the resource parameter. Header names are case
	 * insensitive. If the resource or the header doesn't exist it returns null.
	 * <p>
	 * 
	 * If the header is localized then the localized value is returned (see OSGi
	 * Service Platform, Mobile Specification Release 4 - Localization related
	 * chapters).
	 * 
	 * @param resource
	 *            the name of the resource (it is the same as the value of the
	 *            "Name" attribute in the deployment package's manifest)
	 * @param header
	 *            the requested header
	 * @return the value of the header or <code>null</code> if the resource or
	 *         the header doesn't exist
	 * @throws SecurityException
	 *             if the caller doesn't have the appropriate
	 *             {@link DeploymentAdminPermission} with "metadata" action
	 */
	String getResourceHeader(String resource, String header);

	/**
	 * Uninstalls the deployment package. After uninstallation, the deployment
	 * package object becomes stale. This can be checked by using
	 * {@link #isStale()}, which will return <code>true</code> when stale.
	 * <p>
	 * 
	 * @throws DeploymentException
	 *             if the deployment package could not be successfully
	 *             uninstalled. For detailed error code description see
	 *             {@link DeploymentException}.
	 * @throws SecurityException
	 *             if the caller doesn't have the appropriate
	 *             {@link DeploymentAdminPermission}("&lt;filter&gt;",
	 *             "uninstall") permission.
	 * @throws IllegalStateException
	 *             if the package is stale
	 */
	void uninstall() throws DeploymentException;

	/**
	 * This method is called to completely uninstall a deployment package, which
	 * couldn't be uninstalled using traditional means ({@link #uninstall()})
	 * due to exceptions. After uninstallation, the deployment package object
	 * becomes stale. This can be checked by using {@link #isStale()}, which
	 * will return <code>true</code> when stale.
	 * <p>
	 * 
	 * The method forces removal of the Deployment Package from the repository
	 * maintained by the Deployment Admin service. This method follows the same
	 * steps as {@link #uninstall}. However, any errors or the absence of
	 * Resource Processor services are ignored, they must not cause a roll back.
	 * These errors should be logged.
	 * 
	 * @return true if the operation was successful
	 * @throws DeploymentException
	 *             only {@link DeploymentException#CODE_TIMEOUT} and
	 *             {@link DeploymentException#CODE_CANCELLED} can be thrown. For
	 *             detailed error code description see
	 *             {@link DeploymentException}.
	 * @throws SecurityException
	 *             if the caller doesn't have the appropriate
	 *             {@link DeploymentAdminPermission}("&lt;filter&gt;",
	 *             "uninstall_forced") permission.
	 * @throws IllegalStateException
	 *             if the package is stale
	 */
	boolean uninstallForced() throws DeploymentException;

	/**
	 * Returns a hash code value for the object.
	 * 
	 * @return a hash code value for this object
	 */
	int hashCode();

	/**
	 * Indicates whether some other object is "equal to" this one. Two
	 * deployment packages are equal if they have the same deployment package
	 * symbolic name and version.
	 * 
	 * @param other
	 *            the reference object with which to compare.
	 * @return true if this object is the same as the <code>other<code> argument; false
	 *         otherwise.
	 */
	boolean equals(Object other);

}
