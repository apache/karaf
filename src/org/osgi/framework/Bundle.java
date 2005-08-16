/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/Bundle.java,v 1.27 2005/06/21 16:37:35 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

/**
 * An installed bundle in the Framework.
 * 
 * <p>
 * A <code>Bundle</code> object is the access point to define the lifecycle of
 * an installed bundle. Each bundle installed in the OSGi environment must have
 * an associated <code>Bundle</code> object.
 * 
 * <p>
 * A bundle must have a unique identity, a <code>long</code>, chosen by the
 * Framework. This identity must not change during the lifecycle of a bundle,
 * even when the bundle is updated. Uninstalling and then reinstalling the
 * bundle must create a new unique identity.
 * 
 * <p>
 * A bundle can be in one of six states:
 * <ul>
 * <li>{@link #UNINSTALLED}
 * <li>{@link #INSTALLED}
 * <li>{@link #RESOLVED}
 * <li>{@link #STARTING}
 * <li>{@link #STOPPING}
 * <li>{@link #ACTIVE}
 * </ul>
 * <p>
 * Values assigned to these states have no specified ordering; they represent
 * bit values that may be ORed together to determine if a bundle is in one of
 * the valid states.
 * 
 * <p>
 * A bundle should only execute code when its state is one of
 * <code>STARTING</code>,<code>ACTIVE</code>, or <code>STOPPING</code>.
 * An <code>UNINSTALLED</code> bundle can not be set to another state; it is a
 * zombie and can only be reached because references are kept somewhere.
 * 
 * <p>
 * The Framework is the only entity that is allowed to create
 * <code>Bundle</code> objects, and these objects are only valid within the
 * Framework that created them.
 * 
 * @version $Revision: 1.27 $
 */
public abstract interface Bundle {
	/**
	 * This bundle is uninstalled and may not be used.
	 * 
	 * <p>
	 * The <code>UNINSTALLED</code> state is only visible after a bundle is
	 * uninstalled; the bundle is in an unusable state but references to the
	 * <code>Bundle</code> object may still be available and used for
	 * introspection.
	 * <p>
	 * The value of <code>UNINSTALLED</code> is 0x00000001.
	 */
	public static final int	UNINSTALLED	= 0x00000001;

	/**
	 * This bundle is installed but not yet resolved.
	 * 
	 * <p>
	 * A bundle is in the <code>INSTALLED</code> state when it has been
	 * installed in the Framework but cannot run.
	 * <p>
	 * This state is visible if the bundle's code dependencies are not resolved.
	 * The Framework may attempt to resolve an <code>INSTALLED</code> bundle's
	 * code dependencies and move the bundle to the <code>RESOLVED</code>
	 * state.
	 * <p>
	 * The value of <code>INSTALLED</code> is 0x00000002.
	 */
	public static final int	INSTALLED	= 0x00000002;

	/**
	 * This bundle is resolved and is able to be started.
	 * 
	 * <p>
	 * A bundle is in the <code>RESOLVED</code> state when the Framework has
	 * successfully resolved the bundle's dependencies. These dependencies
	 * include:
	 * <ul>
	 * <li>The bundle's class path from its {@link Constants#BUNDLE_CLASSPATH}
	 * Manifest header.
	 * <li>The bundle's package dependencies from its
	 * {@link Constants#EXPORT_PACKAGE}and {@link Constants#IMPORT_PACKAGE}
	 * Manifest headers.
	 * <li>The bundle's required bundle dependencies from its
	 * {@link Constants#REQUIRE_BUNDLE}Manifest header.
	 * <li>A fragment bundle's host dependency from its
	 * {@link Constants#FRAGMENT_HOST}Manifest header.
	 * </ul>
	 * <p>
	 * Note that the bundle is not active yet. A bundle must be put in the
	 * <code>RESOLVED</code> state before it can be started. The Framework may
	 * attempt to resolve a bundle at any time. 
	 * <p>
	 * The value of <code>RESOLVED</code> is 0x00000004.
	 */
	public static final int	RESOLVED	= 0x00000004;

	/**
	 * This bundle is in the process of starting.
	 * 
	 * <p>
	 * A bundle is in the <code>STARTING</code> state when the {@link #start}
	 * method is active. A bundle must be in this state when the bundle's
	 * {@link BundleActivator#start}is called. If this method completes without
	 * exception, then the bundle has successfully started and must move to the
	 * <code>ACTIVE</code> state.
	 * <p>
	 * The value of <code>STARTING</code> is 0x00000008.
	 */
	public static final int	STARTING	= 0x00000008;

	/**
	 * This bundle is in the process of stopping.
	 * 
	 * <p>
	 * A bundle is in the <code>STOPPING</code> state when the {@link #stop}
	 * method is active. A bundle must be in this state when the bundle's
	 * {@link BundleActivator#stop}method is called. When this method completes
	 * the bundle is stopped and must move to the <code>RESOLVED</code> state.
	 * <p>
	 * The value of <code>STOPPING</code> is 0x00000010.
	 */
	public static final int	STOPPING	= 0x00000010;

	/**
	 * This bundle is now running.
	 * 
	 * <p>
	 * A bundle is in the <code>ACTIVE</code> state when it has been
	 * successfully started.
	 * <p>
	 * The value of <code>ACTIVE</code> is 0x00000020.
	 */
	public static final int	ACTIVE		= 0x00000020;

	/**
	 * Returns this bundle's current state.
	 * 
	 * <p>
	 * A bundle can be in only one state at any time.
	 * 
	 * @return An element of <code>UNINSTALLED</code>,<code>INSTALLED</code>,
	 *         <code>RESOLVED</code>,<code>STARTING</code>,
	 *         <code>STOPPING</code>,<code>ACTIVE</code>.
	 */
	public abstract int getState();

	/**
	 * Starts this bundle.
	 * 
	 * <p>
	 * If the Framework implements the optional Start Level service and the
	 * current start level is less than this bundle's start level, then the
	 * Framework must persistently mark this bundle as started and delay the
	 * starting of this bundle until the Framework's current start level becomes
	 * equal or more than the bundle's start level.
	 * <p>
	 * Otherwise, the following steps are required to start a bundle:
	 * <ol>
	 * <li>If this bundle's state is <code>UNINSTALLED</code> then an
	 * <code>IllegalStateException</code> is thrown.
	 * 
	 * <li>If this bundle's state is <code>STARTING</code> or
	 * <code>STOPPING</code> then this method must wait for this bundle to
	 * change state before continuing. If this does not occur in a reasonable
	 * time, a <code>BundleException</code> is thrown to indicate this bundle
	 * was unable to be started.
	 * 
	 * <li>If this bundle's state is <code>ACTIVE</code> then this method
	 * returns immediately.
	 * 
	 * <li>Persistently record that this bundle has been started. When the
	 * Framework is restarted, this bundle must be automatically started.
	 * 
	 * <li>If this bundle's state is not <code>RESOLVED</code>, an attempt
	 * is made to resolve this bundle's package dependencies. If the Framework
	 * cannot resolve this bundle, a <code>BundleException</code> is thrown.
	 * 
	 * <li>This bundle's state is set to <code>STARTING</code>.
	 * 
	 * <li>The {@link BundleActivator#start}method of this bundle's
	 * <code>BundleActivator</code>, if one is specified, is called. If the
	 * <code>BundleActivator</code> is invalid or throws an exception, this
	 * bundle's state is set back to <code>RESOLVED</code>.<br>
	 * Any services registered by the bundle must be unregistered. <br>
	 * Any services used by the bundle must be released. <br>
	 * Any listeners registered by the bundle must be removed. <br>
	 * A <code>BundleException</code> is then thrown.
	 * 
	 * <li>If this bundle's state is <code>UNINSTALLED</code>, because the
	 * bundle was uninstalled while the <code>BundleActivator.start</code>
	 * method was running, a <code>BundleException</code> is thrown.
	 * 
	 * <li>This bundle's state is set to <code>ACTIVE</code>.
	 * 
	 * <li>A bundle event of type {@link BundleEvent#STARTED}is broadcast.
	 * </ol>
	 * 
	 * <b>Preconditions </b>
	 * <ul>
	 * <li><code>getState()</code> in {<code>INSTALLED</code>}, {
	 * <code>RESOLVED</code>}.
	 * </ul>
	 * <b>Postconditions, no exceptions thrown </b>
	 * <ul>
	 * <li>Bundle persistent state is marked as active.
	 * <li><code>getState()</code> in {<code>ACTIVE</code>}.
	 * <li><code>BundleActivator.start()</code> has been called and did not
	 * throw an exception.
	 * </ul>
	 * <b>Postconditions, when an exception is thrown </b>
	 * <ul>
	 * <li>Depending on when the exception occurred, bundle persistent state is
	 * marked as active.
	 * <li><code>getState()</code> not in {<code>STARTING</code>}, {
	 * <code>ACTIVE</code>}.
	 * </ul>
	 * 
	 * @exception BundleException If this bundle could not be started. This
	 *            could be because a code dependency could not be resolved or
	 *            the specified <code>BundleActivator</code> could not be
	 *            loaded or threw an exception.
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled or this bundle tries to change its own state.
	 * @exception java.lang.SecurityException If the caller does not have the
	 *            appropriate <code>AdminPermission[bundle, EXECUTE]</code>,
	 *            and the Java Runtime Environment supports permissions.
	 */
	public abstract void start() throws BundleException;

	/**
	 * Stops this bundle.
	 * 
	 * <p>
	 * The following steps are required to stop a bundle:
	 * <ol>
	 * <li>If this bundle's state is <code>UNINSTALLED</code> then an
	 * <code>IllegalStateException</code> is thrown.
	 * 
	 * <li>If this bundle's state is <code>STARTING</code> or
	 * <code>STOPPING</code> then this method must wait for this bundle to
	 * change state before continuing. If this does not occur in a reasonable
	 * time, a <code>BundleException</code> is thrown to indicate this bundle
	 * was unable to be stopped.
	 * 
	 * <li>Persistently record that this bundle has been stopped. When the
	 * Framework is restarted, this bundle must not be automatically started.
	 * 
	 * <li>If this bundle's state is not <code>ACTIVE</code> then this method
	 * returns immediately.
	 * 
	 * <li>This bundle's state is set to <code>STOPPING</code>.
	 * 
	 * <li>The {@link BundleActivator#stop}method of this bundle's
	 * <code>BundleActivator</code>, if one is specified, is called. If that
	 * method throws an exception, this method must continue to stop this
	 * bundle. A <code>BundleException</code> must be thrown after completion
	 * of the remaining steps.
	 * 
	 * <li>Any services registered by this bundle must be unregistered.
	 * <li>Any services used by this bundle must be released.
	 * <li>Any listeners registered by this bundle must be removed.
	 * 
	 * <li>If this bundle's state is <code>UNINSTALLED</code>, because the
	 * bundle was uninstalled while the <code>BundleActivator.stop</code>
	 * method was running, a <code>BundleException</code> must be thrown.
	 * 
	 * <li>This bundle's state is set to <code>RESOLVED</code>.
	 * 
	 * <li>A bundle event of type {@link BundleEvent#STOPPED}is broadcast.
	 * </ol>
	 * 
	 * <b>Preconditions </b>
	 * <ul>
	 * <li><code>getState()</code> in {<code>ACTIVE</code>}.
	 * </ul>
	 * <b>Postconditions, no exceptions thrown </b>
	 * <ul>
	 * <li>Bundle persistent state is marked as stopped.
	 * <li><code>getState()</code> not in {<code>ACTIVE</code>,
	 * <code>STOPPING</code>}.
	 * <li><code>BundleActivator.stop</code> has been called and did not
	 * throw an exception.
	 * </ul>
	 * <b>Postconditions, when an exception is thrown </b>
	 * <ul>
	 * <li>Bundle persistent state is marked as stopped.
	 * </ul>
	 * 
	 * @exception BundleException If this bundle's <code>BundleActivator</code>
	 *            could not be loaded or threw an exception.
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled or this bundle tries to change its own state.
	 * @exception java.lang.SecurityException If the caller does not have the
	 *            appropriate <code>AdminPermission[bundle, EXECUTE]</code>,
	 *            and the Java Runtime Environment supports permissions.
	 */
	public abstract void stop() throws BundleException;

	/**
	 * Updates this bundle.
	 * 
	 * <p>
	 * If this bundle's state is <code>ACTIVE</code>, it must be stopped
	 * before the update and started after the update successfully completes.
	 * 
	 * <p>
	 * If the bundle being updated has exported any packages, these packages
	 * must not be updated. Instead, the previous package version must remain
	 * exported until the <code>PackageAdmin.refreshPackages</code> method has
	 * been has been called or the Framework is relaunched.
	 * 
	 * <p>
	 * The following steps are required to update a bundle:
	 * <ol>
	 * <li>If this bundle's state is <code>UNINSTALLED</code> then an
	 * <code>IllegalStateException</code> is thrown.
	 * 
	 * <li>If this bundle's state is <code>ACTIVE</code>,
	 * <code>STARTING</code> or <code>STOPPING</code>, the bundle is
	 * stopped as described in the <code>Bundle.stop</code> method. If
	 * <code>Bundle.stop</code> throws an exception, the exception is rethrown
	 * terminating the update.
	 * 
	 * <li>The download location of the new version of this bundle is
	 * determined from either the bundle's
	 * {@link Constants#BUNDLE_UPDATELOCATION}Manifest header (if available) or
	 * the bundle's original location.
	 * 
	 * <li>The location is interpreted in an implementation dependent manner,
	 * typically as a URL, and the new version of this bundle is obtained from
	 * this location.
	 * 
	 * <li>The new version of this bundle is installed. If the Framework is
	 * unable to install the new version of this bundle, the original version of
	 * this bundle must be restored and a <code>BundleException</code> must be
	 * thrown after completion of the remaining steps.
	 * 
	 * <li>If the bundle has declared an Bundle-RequiredExecutionEnvironment
	 * header, then the listed execution environments must be verified against
	 * the installed execution environments. If they do not all match, the
	 * original version of this bundle must be restored and a
	 * <code>BundleException</code> must be thrown after completion of the
	 * remaining steps.
	 * 
	 * <li>This bundle's state is set to <code>INSTALLED</code>.
	 * 
	 * <li>If the new version of this bundle was successfully installed, a
	 * bundle event of type {@link BundleEvent#UPDATED}is broadcast.
	 * 
	 * <li>If this bundle's state was originally <code>ACTIVE</code>, the
	 * updated bundle is started as described in the <code>Bundle.start</code>
	 * method. If <code>Bundle.start</code> throws an exception, a Framework
	 * event of type {@link FrameworkEvent#ERROR}is broadcast containing the
	 * exception.
	 * </ol>
	 * 
	 * <b>Preconditions </b>
	 * <ul>
	 * <li><code>getState()</code> not in {<code>UNINSTALLED</code>}.
	 * </ul>
	 * <b>Postconditions, no exceptions thrown </b>
	 * <ul>
	 * <li><code>getState()</code> in {<code>INSTALLED</code>,
	 * <code>RESOLVED</code>,<code>ACTIVE</code>}.
	 * <li>This bundle has been updated.
	 * </ul>
	 * <b>Postconditions, when an exception is thrown </b>
	 * <ul>
	 * <li><code>getState()</code> in {<code>INSTALLED</code>,
	 * <code>RESOLVED</code>,<code>ACTIVE</code>}.
	 * <li>Original bundle is still used; no update occurred.
	 * </ul>
	 * 
	 * @exception BundleException If the update fails.
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled or this bundle tries to change its own state.
	 * @exception java.lang.SecurityException If the caller does not have the
	 *            appropriate <code>AdminPermission[bundle, LIFECYCLE]</code> for both
	 *            the current bundle and the updated bundle,
	 *            and the Java Runtime Environment supports permissions.
	 * @see #stop()
	 * @see #start()
	 */
	public abstract void update() throws BundleException;

	/**
	 * Updates this bundle from an <code>InputStream</code>.
	 * 
	 * <p>
	 * This method performs all the steps listed in <code>Bundle.update()</code>,
	 * except the bundle must be read from the supplied <code>InputStream</code>,
	 * rather than a <code>URL</code>.
	 * <p>
	 * This method must always close the <code>InputStream</code> when it is
	 * done, even if an exception is thrown.
	 * 
	 * @param in The <code>InputStream</code> from which to read the new
	 *        bundle.
	 * @exception BundleException If the provided stream cannot be read or the
	 *            update fails.
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled or this bundle tries to change its own state.
	 * @exception java.lang.SecurityException If the caller does not have the
	 *            appropriate <code>AdminPermission[bundle, LIFECYCLE]</code> for both
	 *            the current bundle and the updated bundle,
	 *            and the Java Runtime Environment supports permissions.
	 * @see #update()
	 */
	public abstract void update(InputStream in) throws BundleException;

	/**
	 * Uninstalls this bundle.
	 * 
	 * <p>
	 * This method causes the Framework to notify other bundles that this bundle
	 * is being uninstalled, and then puts this bundle into the
	 * <code>UNINSTALLED</code> state. The Framework must remove any resources
	 * related to this bundle that it is able to remove.
	 * 
	 * <p>
	 * If this bundle has exported any packages, the Framework must continue to
	 * make these packages available to their importing bundles until the
	 * <code>PackageAdmin.refreshPackages</code> method has been called or the
	 * Framework is relaunched.
	 * 
	 * <p>
	 * The following steps are required to uninstall a bundle:
	 * <ol>
	 * <li>If this bundle's state is <code>UNINSTALLED</code> then an
	 * <code>IllegalStateException</code> is thrown.
	 * 
	 * <li>If this bundle's state is <code>ACTIVE</code>,
	 * <code>STARTING</code> or <code>STOPPING</code>, this bundle is
	 * stopped as described in the <code>Bundle.stop</code> method. If
	 * <code>Bundle.stop</code> throws an exception, a Framework event of type
	 * {@link FrameworkEvent#ERROR}is broadcast containing the exception.
	 * 
	 * <li>This bundle's state is set to <code>UNINSTALLED</code>.
	 * 
	 * <li>A bundle event of type {@link BundleEvent#UNINSTALLED}is broadcast.
	 * 
	 * <li>This bundle and any persistent storage area provided for this bundle
	 * by the Framework are removed.
	 * </ol>
	 * 
	 * <b>Preconditions </b>
	 * <ul>
	 * <li><code>getState()</code> not in {<code>UNINSTALLED</code>}.
	 * </ul>
	 * <b>Postconditions, no exceptions thrown </b>
	 * <ul>
	 * <li><code>getState()</code> in {<code>UNINSTALLED</code>}.
	 * <li>This bundle has been uninstalled.
	 * </ul>
	 * <b>Postconditions, when an exception is thrown </b>
	 * <ul>
	 * <li><code>getState()</code> not in {<code>UNINSTALLED</code>}.
	 * <li>This Bundle has not been uninstalled.
	 * </ul>
	 * 
	 * @exception BundleException If the uninstall failed. This can occur if
	 *            another thread is attempting to change the bundle's state and
	 *            does not complete in a timely manner.
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled or this bundle tries to change its own state.
	 * @exception java.lang.SecurityException If the caller does not have the
	 *            appropriate <code>AdminPermission[bundle, LIFECYCLE]</code>,
	 *            and the Java Runtime Environment supports permissions.
	 * @see #stop()
	 */
	public abstract void uninstall() throws BundleException;

	/**
	 * Returns this bundle's Manifest headers and values. This method returns
	 * all the Manifest headers and values from the main section of the bundle's
	 * Manifest file; that is, all lines prior to the first blank line.
	 * 
	 * <p>
	 * Manifest header names are case-insensitive. The methods of the returned
	 * <code>Dictionary</code> object must operate on header names in a
	 * case-insensitive manner.
	 * 
	 * If a Manifest header value starts with &quot;%&quot;, it must be
	 * localized according to the default locale.
	 * 
	 * <p>
	 * For example, the following Manifest headers and values are included if
	 * they are present in the Manifest file:
	 * 
	 * <pre>
	 *        Bundle-Name
	 *        Bundle-Vendor
	 *        Bundle-Version
	 *        Bundle-Description
	 *        Bundle-DocURL
	 *        Bundle-ContactAddress
	 * </pre>
	 * 
	 * <p>
	 * This method must continue to return Manifest header information while
	 * this bundle is in the <code>UNINSTALLED</code> state.
	 * 
	 * @return A <code>Dictionary</code> object containing this bundle's
	 *         Manifest headers and values.
	 * 
	 * @exception java.lang.SecurityException If the caller does not have the appropriate
	 *            <code>AdminPermission[bundle, METADATA]</code>, and the
	 *            Java Runtime Environment supports permissions.
	 * 
	 * @see Constants#BUNDLE_LOCALIZATION
	 */
	public abstract Dictionary getHeaders();

	/**
	 * Returns this bundle's identifier. The bundle is assigned a unique
	 * identifier by the Framework when it is installed in the OSGi environment.
	 * 
	 * <p>
	 * A bundle's unique identifier has the following attributes:
	 * <ul>
	 * <li>Is unique and persistent.
	 * <li>Is a <code>long</code>.
	 * <li>Its value is not reused for another bundle, even after the bundle is
	 * uninstalled.
	 * <li>Does not change while the bundle remains installed.
	 * <li>Does not change when the bundle is updated.
	 * </ul>
	 * 
	 * <p>
	 * This method must continue to return this bundle's unique identifier while
	 * this bundle is in the <code>UNINSTALLED</code> state.
	 * 
	 * @return The unique identifier of this bundle.
	 */
	public abstract long getBundleId();

	/**
	 * Returns this bundle's location identifier.
	 * 
	 * <p>
	 * The bundle location identifier is the location passed to
	 * <code>BundleContext.installBundle</code> when a bundle is installed.
	 * The bundle location identifier does not change while the bundle remains
	 * installed, even if the bundle is updated.
	 * 
	 * <p>
	 * This method must continue to return this bundle's location identifier
	 * while this bundle is in the <code>UNINSTALLED</code> state.
	 * 
	 * @return The string representation of this bundle's location identifier.
	 * @exception java.lang.SecurityException If the caller does not have the
	 *            appropriate <code>AdminPermission[bundle, METADATA]</code>,
	 *            and the Java Runtime Environment supports permissions.
	 */
	public abstract String getLocation();

	/**
	 * Returns this bundle's <code>ServiceReference</code> list for all
	 * services it has registered or <code>null</code> if this bundle has no
	 * registered services.
	 * 
	 * <p>
	 * If the Java runtime supports permissions, a <code>ServiceReference</code>
	 * object to a service is included in the returned list only if the caller
	 * has the <code>ServicePermission</code> to get the service using at
	 * least one of the named classes the service was registered under.
	 * 
	 * <p>
	 * The list is valid at the time of the call to this method, however, as the
	 * Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 * 
	 * @return An array of <code>ServiceReference</code> objects or
	 *         <code>null</code>.
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled.
	 * @see ServiceRegistration
	 * @see ServiceReference
	 * @see ServicePermission
	 */
	public abstract ServiceReference[] getRegisteredServices();

	/**
	 * Returns this bundle's <code>ServiceReference</code> list for all
	 * services it is using or returns <code>null</code> if this bundle is not
	 * using any services. A bundle is considered to be using a service if its
	 * use count for that service is greater than zero.
	 * 
	 * <p>
	 * If the Java Runtime Environment supports permissions, a
	 * <code>ServiceReference</code> object to a service is included in the
	 * returned list only if the caller has the <code>ServicePermission</code>
	 * to get the service using at least one of the named classes the service
	 * was registered under.
	 * <p>
	 * The list is valid at the time of the call to this method, however, as the
	 * Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 * 
	 * @return An array of <code>ServiceReference</code> objects or
	 *         <code>null</code>.
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled.
	 * @see ServiceReference
	 * @see ServicePermission
	 */
	public abstract ServiceReference[] getServicesInUse();

	/**
	 * Determines if this bundle has the specified permissions.
	 * 
	 * <p>
	 * If the Java Runtime Environment does not support permissions, this method
	 * always returns <code>true</code>.
	 * <p>
	 * <code>permission</code> is of type <code>Object</code> to avoid
	 * referencing the <code>java.security.Permission</code> class directly.
	 * This is to allow the Framework to be implemented in Java environments
	 * which do not support permissions.
	 * 
	 * <p>
	 * If the Java Runtime Environment does support permissions, this bundle and
	 * all its resources including embedded JAR files, belong to the same
	 * <code>java.security.ProtectionDomain</code>; that is, they must share
	 * the same set of permissions.
	 * 
	 * @param permission The permission to verify.
	 * 
	 * @return <code>true</code> if this bundle has the specified permission
	 *         or the permissions possessed by this bundle imply the specified
	 *         permission; <code>false</code> if this bundle does not have the
	 *         specified permission or <code>permission</code> is not an
	 *         <code>instanceof</code> <code>java.security.Permission</code>.
	 * 
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled.
	 */
	public abstract boolean hasPermission(Object permission);

	/**
	 * Find the specified resource from this bundle.
	 * 
	 * This bundle's class loader is called to search for the named resource. If
	 * this bundle's state is <code>INSTALLED</code>, then only this bundle
	 * must be searched for the specified resource. Imported packages cannot be
	 * searched when a bundle has not been resolved. If this bundle is a
	 * fragment bundle then <code>null</code> is returned.
	 * 
	 * @param name The name of the resource. See
	 *        <code>java.lang.ClassLoader.getResource</code> for a description
	 *        of the format of a resource name.
	 * @return a URL to the named resource, or <code>null</code> if the
	 *         resource could not be found or if this bundle is a fragment
	 *         bundle or if the caller does not have the appropriate
	 *         <code>AdminPermission[bundle, RESOURCE]</code>, and the Java
	 *         Runtime Environment supports permissions.
	 * 
	 * @since 1.1
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled.
	 */
	public abstract URL getResource(String name);

	/**
	 * Returns this bundle's Manifest headers and values localized to the
	 * specifed locale.
	 * 
	 * <p>
	 * This method performs the same function as
	 * <code>Bundle.getHeaders()</code> except the manifest header values are
	 * localized to the specified locale.
	 * 
	 * If a Manifest header value starts with &quot;%&quot;, it must be
	 * localized according to the specified locale. If the specified locale
	 * cannot be found, then the header values must be returned using the
	 * default locale.
	 * 
	 * If <code>null</code> is specified as the locale string, the header
	 * values must be localized using the default locale. If the empty string
	 * (&quot;&quot;) is specified as the locale string, the header values must
	 * not be localized and the raw (unlocalized) header values, including any
	 * leading &quot;%&quot;, must be returned.
	 * 
	 * <p>
	 * This method must continue to return Manifest header information while
	 * this bundle is in the <code>UNINSTALLED</code> state, however the
	 * header values must only be available in the raw and default locale
	 * values.
	 * 
	 * @param locale The locale name into which the header values are to be
	 *        localized. If the specified locale is <code>null</code> then the
	 *        locale returned by <code>java.util.Locale.getDefault</code> is
	 *        used. If the specified locale is the empty string, this method
	 *        will return the raw (unlocalized) manifest headers including any
	 *        leading &quot;%&quot;.
	 * @return A <code>Dictionary</code> object containing this bundle's
	 *         Manifest headers and values.
	 * 
	 * @exception java.lang.SecurityException If the caller does not have the appropriate
	 *            <code>AdminPermission[bundle, METADATA]</code>, and the
	 *            Java Runtime Environment supports permissions.
	 * 
	 * @see #getHeaders()
	 * @see Constants#BUNDLE_LOCALIZATION
	 * @since 1.3
	 */
	public Dictionary getHeaders(String locale);

	/**
	 * Returns the symbolic name of this bundle as specified by its
	 * <code>Bundle-SymbolicName</code> manifest header. The name must be
	 * unique, it is recommended to use a reverse domain name naming convention
	 * like that used for java packages. If the bundle does not have a specified
	 * symbolic name then <code>null</code> is returned.
	 * 
	 * <p>
	 * This method must continue to return this bundle's symbolic name while
	 * this bundle is in the <code>UNINSTALLED</code> state.
	 * 
	 * @return The symbolic name of this bundle.
	 * @since 1.3
	 */
	public String getSymbolicName();

	/**
	 * 
	 * Loads the specified class using this bundle's classloader.
	 * 
	 * <p>
	 * If the bundle is a fragment bundle then this method must throw a
	 * <code>ClassNotFoundException</code>.
	 * 
	 * <p>
	 * If this bundle's state is <code>INSTALLED</code>, this method must
	 * attempt to resolve the bundle before attempting to load the class.
	 * 
	 * <p>
	 * If the bundle cannot be resolved, a Framework event of type
	 * {@link FrameworkEvent#ERROR}is broadcast containing a
	 * <code>BundleException</code> with details of the reason the bundle
	 * could not be resolved. This method must then throw a
	 * <code>ClassNotFoundException</code>.
	 * 
	 * <p>
	 * If this bundle's state is <code>UNINSTALLED</code>, then an
	 * <code>IllegalStateException</code> is thrown.
	 * 
	 * @param name The name of the class to load.
	 * @return The Class object for the requested class.
	 * @exception java.lang.ClassNotFoundException If no such class can be found
	 *            or if this bundle is a fragment bundle or if the caller does
	 *            not have the appropriate <code>AdminPermission[bundle, CLASS]</code>,
	 *            and the Java Runtime Environment supports permissions.
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled.
	 * @since 1.3
	 */
	public Class loadClass(String name) throws ClassNotFoundException;

	/**
	 * Find the specified resources from this bundle.
	 * 
	 * This bundle's class loader is called to search for the named resource. If
	 * this bundle's state is <code>INSTALLED</code>, then only this bundle
	 * must be searched for the specified resource. Imported packages cannot be
	 * searched when a bundle has not been resolved. If this bundle is a
	 * fragment bundle then <code>null</code> is returned.
	 * 
	 * @param name The name of the resource. See
	 *        <code>java.lang.ClassLoader.getResources</code> for a
	 *        description of the format of a resource name.
	 * @return an Enumeration of URLs to the named resources, or
	 *         <code>null</code> if the resource could not be found or if this
	 *         bundle is a fragment bundle or if the caller does not have the appropriate
	 *         <code>AdminPermission[bundle, RESOURCE]</code>, and the Java
	 *         Runtime Environment supports permissions.
	 * 
	 * @since 1.3
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled.
	 * @throws java.io.IOException If there is an I/O error.
	 */
	public Enumeration getResources(String name) throws IOException;

	/**
	 * Returns an Enumeration of all the paths (<code>String</code>) objects) to 
	 * entries within the bundle whose longest sub-path matches the supplied path 
	 * argument. The bundle's classloader is not used to search for entries. Only 
	 * the contents of the bundle is searched.  A specified path of &quot;/&quot; 
	 * indicates the root of the bundle.
	 * 
	 * <p>
	 * Returned paths indicating subdirectory paths end with a &quot;/&quot;.
	 * The returned paths are all relative to the root of the bundle.
	 * 
	 * <p>
	 * This method returns <code>null</code> if no entries could be found that
	 * match the specified path or if the caller does not have the appropriate
	 * <code>AdminPermission[bundle, RESOURCE]</code> and the Java 
	 * Runtime Environment supports permissions.
	 * 
	 * @param path the path name to get the entry path for.
	 * @return An Enumeration of the entry paths (<code>String</code> objects) 
	 *         or <code>null</code> if an entry could not be found or if the 
	 *         caller does not have the appropriate <code>AdminPermission[bundle, RESOURCE]</code> 
	 *         and the Java Runtime Environment supports permissions.
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled.
	 * @since 1.3
	 */
	public Enumeration getEntryPaths(String path);

	/**
	 * Returns a URL to the specified entry in this bundle. The bundle's
	 * classloader is not used to search for the specified entry. Only the
	 * contents of the bundle is searched for the specified entry. A specified
	 * path of &quot;/&quot; indicates the root of the bundle.
	 * 
	 * <p>
	 * This method returns a URL to the specified entry, or <code>null</code>
	 * if the entry could not be found or if the caller does not have the appropriate
	 * <code>AdminPermission[bundle, RESOURCE]</code> and the Java Runtime
	 * Environment supports permissions.
	 * 
	 * @param name The name of the entry. See
	 *        <code>java.lang.ClassLoader.getResource</code> for a description
	 *        of the format of a resource name.
	 * @return A URL to the specified entry, or <code>null</code> if the entry
	 *         could not be found or if the caller does not have the appropriate
	 *         <code>AdminPermission[bundle, RESOURCE]</code> and the Java
	 *         Runtime Environment supports permissions.
	 * 
	 * @exception java.lang.IllegalStateException If this bundle has been
	 *            uninstalled.
	 * @since 1.3
	 */
	public URL getEntry(String name);

	/**
	 * Returns the time when this bundle was last modified. A bundle is
	 * considered to be modified when it is installed, updated or uninstalled.
	 * 
	 * <p>
	 * The time value is the number of milliseconds since January 1, 1970,
	 * 00:00:00 GMT.
	 * 
	 * @return The time when this bundle was last modified.
	 * @since 1.3
	 */
	public long getLastModified();

	/**
	 * Returns entries in this bundle and its attached fragments. The bundle's
	 * classloader is not used to search for entries. Only the contents of the
	 * bundle and its attached fragments are searched for the specified
	 * entries.
	 * 
	 * If this bundle's state is <code>INSTALLED</code>, this method must
	 * attempt to resolve the bundle before attempting to find entries.<p>
	 * 
	 * This method is intended to be used to obtain configuration, setup,
	 * localization and other information from this bundle. This method takes
	 * into account that the &quot;contents&quot; of this bundle can be extended
	 * with fragments. This &quot;bundle space&quot; is not a namespace with
	 * unique members; the same entry name can be present multiple times. This
	 * method therefore returns an enumeration of URL objects. These URLs can
	 * come from different JARs but have the same path name. This method can
	 * either return only entries in the specified path or recurse into
	 * subdirectories returning entries in the directory tree beginning at the
	 * specified path. Fragments can be attached after this bundle is resolved,
	 * possibly changing the set of URLs returned by this method. If this bundle
	 * is not resolved, only the entries in the JAR file of this bundle are
	 * returned.
	 * <p>
	 * Examples:
	 * <pre>
	 * // List all XML files in the OSGI-INF directory and below
	 * Enumeration	e	= b.findEntries(&quot;OSGI-INF&quot;, &quot;*.xml&quot;, true);
	 * 
	 * // Find a specific localization file
	 * Enumeration e = b.findEntries(&quot;OSGI-INF/l10n&quot;, 
	 *    &quot;bundle_nl_DU.properties&quot;, false);
	 * if (e.hasMoreElements())
	 * 	return (URL) e.nextElement();
	 * </pre>
	 * 
	 * @param path The path name in which to look. A specified path of
	 *        &quot;/&quot; indicates the root of the bundle. Path is relative
	 *        to the root of the bundle and must not be null.
	 * @param filePattern The file name pattern for selecting entries in the
	 *        specified path. The pattern is only matched against the last
	 *        element of the entry path and it supports substring matching, as
	 *        specified in the Filter specification, using the wildcard
	 *        character (&quot;*&quot;). If null is specified, this is
	 *        equivalent to &quot;*&quot; and matches all files.
	 * @param recurse If <code>true</code>, recurse into subdirectories.
	 *        Otherwise only return entries from the given directory.
	 * @return An enumeration of URL objects for each matching entry, or
	 *         <code>null</code> if an entry could not be found or if the
	 *         caller does not have the appropriate
	 *         <code>AdminPermission[bundle, RESOURCE]</code>, and the Java
	 *         Runtime Environment supports permissions. The URLs are sorted
	 *         such that entries from this bundle are returned first followed by
	 *         the entries from attached fragments in ascending bundle id order.
	 *         If this bundle is a fragment, then only matching entries in this
	 *         fragment are returned.
	 * @since 1.3
	 */
	public Enumeration findEntries(String path, String filePattern,
			boolean recurse);
}