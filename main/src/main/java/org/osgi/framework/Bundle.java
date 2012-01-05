/*
 * Copyright (c) OSGi Alliance (2000, 2011). All Rights Reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.osgi.framework.wiring.FrameworkWiring;

/**
 * An installed bundle in the Framework.
 * 
 * <p>
 * A {@code Bundle} object is the access point to define the lifecycle of an
 * installed bundle. Each bundle installed in the OSGi environment must have an
 * associated {@code Bundle} object.
 * 
 * <p>
 * A bundle must have a unique identity, a {@code long}, chosen by the
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
 * A bundle should only have active threads of execution when its state is one
 * of {@code STARTING},{@code ACTIVE}, or {@code STOPPING}. An {@code
 * UNINSTALLED} bundle can not be set to another state; it is a zombie and can
 * only be reached because references are kept somewhere.
 * 
 * <p>
 * The Framework is the only entity that is allowed to create {@code Bundle}
 * objects, and these objects are only valid within the Framework that created
 * them.
 * 
 * <p>
 * Bundles have a natural ordering such that if two {@code Bundle}s have the
 * same {@link #getBundleId() bundle id} they are equal. A {@code Bundle} is
 * less than another {@code Bundle} if it has a lower {@link #getBundleId()
 * bundle id} and is greater if it has a higher bundle id.
 * 
 * @ThreadSafe
 * @noimplement
 * @version $Id: 239108e8d54ff493587b9cdfe1688bdefc5a714c $
 */
public interface Bundle extends Comparable<Bundle> {
	/**
	 * The bundle is uninstalled and may not be used.
	 * 
	 * <p>
	 * The {@code UNINSTALLED} state is only visible after a bundle is
	 * uninstalled; the bundle is in an unusable state but references to the
	 * {@code Bundle} object may still be available and used for introspection.
	 * <p>
	 * The value of {@code UNINSTALLED} is 0x00000001.
	 */
	int	UNINSTALLED				= 0x00000001;

	/**
	 * The bundle is installed but not yet resolved.
	 * 
	 * <p>
	 * A bundle is in the {@code INSTALLED} state when it has been installed in
	 * the Framework but is not or cannot be resolved.
	 * <p>
	 * This state is visible if the bundle's code dependencies are not resolved.
	 * The Framework may attempt to resolve an {@code INSTALLED} bundle's code
	 * dependencies and move the bundle to the {@code RESOLVED} state.
	 * <p>
	 * The value of {@code INSTALLED} is 0x00000002.
	 */
	int	INSTALLED				= 0x00000002;

	/**
	 * The bundle is resolved and is able to be started.
	 * 
	 * <p>
	 * A bundle is in the {@code RESOLVED} state when the Framework has
	 * successfully resolved the bundle's code dependencies. These dependencies
	 * include:
	 * <ul>
	 * <li>The bundle's class path from its {@link Constants#BUNDLE_CLASSPATH}
	 * Manifest header.
	 * <li>The bundle's package dependencies from its
	 * {@link Constants#EXPORT_PACKAGE} and {@link Constants#IMPORT_PACKAGE}
	 * Manifest headers.
	 * <li>The bundle's required bundle dependencies from its
	 * {@link Constants#REQUIRE_BUNDLE} Manifest header.
	 * <li>A fragment bundle's host dependency from its
	 * {@link Constants#FRAGMENT_HOST} Manifest header.
	 * </ul>
	 * <p>
	 * Note that the bundle is not active yet. A bundle must be put in the
	 * {@code RESOLVED} state before it can be started. The Framework may
	 * attempt to resolve a bundle at any time.
	 * <p>
	 * The value of {@code RESOLVED} is 0x00000004.
	 */
	int	RESOLVED				= 0x00000004;

	/**
	 * The bundle is in the process of starting.
	 * 
	 * <p>
	 * A bundle is in the {@code STARTING} state when its {@link #start(int)
	 * start} method is active. A bundle must be in this state when the bundle's
	 * {@link BundleActivator#start} is called. If the {@code
	 * BundleActivator.start} method completes without exception, then the
	 * bundle has successfully started and must move to the {@code ACTIVE}
	 * state.
	 * <p>
	 * If the bundle has a {@link Constants#ACTIVATION_LAZY lazy activation
	 * policy}, then the bundle may remain in this state for some time until the
	 * activation is triggered.
	 * <p>
	 * The value of {@code STARTING} is 0x00000008.
	 */
	int	STARTING				= 0x00000008;

	/**
	 * The bundle is in the process of stopping.
	 * 
	 * <p>
	 * A bundle is in the {@code STOPPING} state when its {@link #stop(int)
	 * stop} method is active. A bundle must be in this state when the bundle's
	 * {@link BundleActivator#stop} method is called. When the {@code
	 * BundleActivator.stop} method completes the bundle is stopped and must
	 * move to the {@code RESOLVED} state.
	 * <p>
	 * The value of {@code STOPPING} is 0x00000010.
	 */
	int	STOPPING				= 0x00000010;

	/**
	 * The bundle is now running.
	 * 
	 * <p>
	 * A bundle is in the {@code ACTIVE} state when it has been successfully
	 * started and activated.
	 * <p>
	 * The value of {@code ACTIVE} is 0x00000020.
	 */
	int	ACTIVE					= 0x00000020;

	/**
	 * The bundle start operation is transient and the persistent autostart
	 * setting of the bundle is not modified.
	 * 
	 * <p>
	 * This bit may be set when calling {@link #start(int)} to notify the
	 * framework that the autostart setting of the bundle must not be modified.
	 * If this bit is not set, then the autostart setting of the bundle is
	 * modified.
	 * 
	 * @since 1.4
	 * @see #start(int)
	 */
	int	START_TRANSIENT			= 0x00000001;

	/**
	 * The bundle start operation must activate the bundle according to the
	 * bundle's declared {@link Constants#BUNDLE_ACTIVATIONPOLICY activation
	 * policy}.
	 * 
	 * <p>
	 * This bit may be set when calling {@link #start(int)} to notify the
	 * framework that the bundle must be activated using the bundle's declared
	 * activation policy.
	 * 
	 * @since 1.4
	 * @see Constants#BUNDLE_ACTIVATIONPOLICY
	 * @see #start(int)
	 */
	int	START_ACTIVATION_POLICY	= 0x00000002;

	/**
	 * The bundle stop is transient and the persistent autostart setting of the
	 * bundle is not modified.
	 * 
	 * <p>
	 * This bit may be set when calling {@link #stop(int)} to notify the
	 * framework that the autostart setting of the bundle must not be modified.
	 * If this bit is not set, then the autostart setting of the bundle is
	 * modified.
	 * 
	 * @since 1.4
	 * @see #stop(int)
	 */
	int	STOP_TRANSIENT			= 0x00000001;

	/**
	 * Request that all certificates used to sign the bundle be returned.
	 * 
	 * @since 1.5
	 * @see #getSignerCertificates(int)
	 */
	int	SIGNERS_ALL				= 1;

	/**
	 * Request that only certificates used to sign the bundle that are trusted
	 * by the framework be returned.
	 * 
	 * @since 1.5
	 * @see #getSignerCertificates(int)
	 */
	int	SIGNERS_TRUSTED			= 2;

	/**
	 * Returns this bundle's current state.
	 * 
	 * <p>
	 * A bundle can be in only one state at any time.
	 * 
	 * @return An element of {@code UNINSTALLED},{@code INSTALLED},
	 *         {@code RESOLVED}, {@code STARTING}, {@code STOPPING},
	 *         {@code ACTIVE}.
	 */
	int getState();

	/**
	 * Starts this bundle.
	 * 
	 * <p>
	 * If this bundle's state is {@code UNINSTALLED} then an
	 * {@code IllegalStateException} is thrown.
	 * <p>
	 * If the current start level is less than this bundle's start level:
	 * <ul>
	 * <li>If the {@link #START_TRANSIENT} option is set, then a
	 * {@code BundleException} is thrown indicating this bundle cannot be
	 * started due to the Framework's current start level.
	 * 
	 * <li>Otherwise, the Framework must set this bundle's persistent autostart
	 * setting to <em>Started with declared activation</em> if the
	 * {@link #START_ACTIVATION_POLICY} option is set or
	 * <em>Started with eager activation</em> if not set.
	 * </ul>
	 * <p>
	 * When the Framework's current start level becomes equal to or more than
	 * this bundle's start level, this bundle will be started.
	 * <p>
	 * Otherwise, the following steps are required to start this bundle:
	 * <ol>
	 * <li>If this bundle is in the process of being activated or deactivated
	 * then this method must wait for activation or deactivation to complete
	 * before continuing. If this does not occur in a reasonable time, a
	 * {@code BundleException} is thrown to indicate this bundle was unable to
	 * be started.
	 * 
	 * <li>If this bundle's state is {@code ACTIVE} then this method returns
	 * immediately.
	 * 
	 * <li>If the {@link #START_TRANSIENT} option is not set then set this
	 * bundle's autostart setting to <em>Started with declared activation</em>
	 * if the {@link #START_ACTIVATION_POLICY} option is set or
	 * <em>Started with eager activation</em> if not set. When the Framework is
	 * restarted and this bundle's autostart setting is not <em>Stopped</em>,
	 * this bundle must be automatically started.
	 * 
	 * <li>If this bundle's state is not {@code RESOLVED}, an attempt is made to
	 * resolve this bundle. If the Framework cannot resolve this bundle, a
	 * {@code BundleException} is thrown.
	 * 
	 * <li>If the {@link #START_ACTIVATION_POLICY} option is set and this
	 * bundle's declared activation policy is {@link Constants#ACTIVATION_LAZY
	 * lazy} then:
	 * <ul>
	 * <li>If this bundle's state is {@code STARTING} then this method returns
	 * immediately.
	 * <li>This bundle's state is set to {@code STARTING}.
	 * <li>A bundle event of type {@link BundleEvent#LAZY_ACTIVATION} is fired.
	 * <li>This method returns immediately and the remaining steps will be
	 * followed when this bundle's activation is later triggered.
	 * </ul>
	 * <i></i>
	 * <li>This bundle's state is set to {@code STARTING}.
	 * 
	 * <li>A bundle event of type {@link BundleEvent#STARTING} is fired.
	 * 
	 * <li>The {@link BundleActivator#start} method of this bundle's
	 * {@code BundleActivator}, if one is specified, is called. If the
	 * {@code BundleActivator} is invalid or throws an exception then:
	 * <ul>
	 * <li>This bundle's state is set to {@code STOPPING}.
	 * <li>A bundle event of type {@link BundleEvent#STOPPING} is fired.
	 * <li>Any services registered by this bundle must be unregistered.
	 * <li>Any services used by this bundle must be released.
	 * <li>Any listeners registered by this bundle must be removed.
	 * <li>This bundle's state is set to {@code RESOLVED}.
	 * <li>A bundle event of type {@link BundleEvent#STOPPED} is fired.
	 * <li>A {@code BundleException} is then thrown.
	 * </ul>
	 * <i></i>
	 * <li>If this bundle's state is {@code UNINSTALLED}, because this bundle
	 * was uninstalled while the {@code BundleActivator.start} method was
	 * running, a {@code BundleException} is thrown.
	 * 
	 * <li>This bundle's state is set to {@code ACTIVE}.
	 * 
	 * <li>A bundle event of type {@link BundleEvent#STARTED} is fired.
	 * </ol>
	 * 
	 * <b>Preconditions </b>
	 * <ul>
	 * <li>{@code getState()} in &#x007B; {@code INSTALLED}, {@code RESOLVED}
	 * &#x007D; or &#x007B; {@code INSTALLED}, {@code RESOLVED},
	 * {@code STARTING} &#x007D; if this bundle has a lazy activation policy.
	 * </ul>
	 * <b>Postconditions, no exceptions thrown </b>
	 * <ul>
	 * <li>Bundle autostart setting is modified unless the
	 * {@link #START_TRANSIENT} option was set.
	 * <li>{@code getState()} in &#x007B; {@code ACTIVE} &#x007D; unless the
	 * lazy activation policy was used.
	 * <li>{@code BundleActivator.start()} has been called and did not throw an
	 * exception unless the lazy activation policy was used.
	 * </ul>
	 * <b>Postconditions, when an exception is thrown </b>
	 * <ul>
	 * <li>Depending on when the exception occurred, bundle autostart setting is
	 * modified unless the {@link #START_TRANSIENT} option was set.
	 * <li>{@code getState()} not in &#x007B; {@code STARTING}, {@code ACTIVE}
	 * &#x007D;.
	 * </ul>
	 * 
	 * @param options The options for starting this bundle. See
	 *        {@link #START_TRANSIENT} and {@link #START_ACTIVATION_POLICY}. The
	 *        Framework must ignore unrecognized options.
	 * @throws BundleException If this bundle could not be started.
	 *         BundleException types thrown by this method include:
	 *         {@link BundleException#START_TRANSIENT_ERROR},
	 *         {@link BundleException#NATIVECODE_ERROR},
	 *         {@link BundleException#RESOLVE_ERROR},
	 *         {@link BundleException#STATECHANGE_ERROR}, and
	 *         {@link BundleException#ACTIVATOR_ERROR}.
	 * @throws IllegalStateException If this bundle has been uninstalled or this
	 *         bundle tries to change its own state.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,EXECUTE]}, and the Java Runtime
	 *         Environment supports permissions.
	 * @since 1.4
	 */
	void start(int options) throws BundleException;

	/**
	 * Starts this bundle with no options.
	 * 
	 * <p>
	 * This method performs the same function as calling {@code start(0)}.
	 * 
	 * @throws BundleException If this bundle could not be started.
	 *         BundleException types thrown by this method include:
	 *         {@link BundleException#NATIVECODE_ERROR},
	 *         {@link BundleException#RESOLVE_ERROR},
	 *         {@link BundleException#STATECHANGE_ERROR}, and
	 *         {@link BundleException#ACTIVATOR_ERROR}.
	 * @throws IllegalStateException If this bundle has been uninstalled or this
	 *         bundle tries to change its own state.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,EXECUTE]}, and the Java Runtime
	 *         Environment supports permissions.
	 * @see #start(int)
	 */
	void start() throws BundleException;

	/**
	 * Stops this bundle.
	 * 
	 * <p>
	 * The following steps are required to stop a bundle:
	 * <ol>
	 * <li>If this bundle's state is {@code UNINSTALLED} then an
	 * {@code IllegalStateException} is thrown.
	 * 
	 * <li>If this bundle is in the process of being activated or deactivated
	 * then this method must wait for activation or deactivation to complete
	 * before continuing. If this does not occur in a reasonable time, a
	 * {@code BundleException} is thrown to indicate this bundle was unable to
	 * be stopped.
	 * <li>If the {@link #STOP_TRANSIENT} option is not set then then set this
	 * bundle's persistent autostart setting to to <em>Stopped</em>. When the
	 * Framework is restarted and this bundle's autostart setting is
	 * <em>Stopped</em>, this bundle must not be automatically started.
	 * 
	 * <li>If this bundle's state is not {@code STARTING} or {@code ACTIVE} then
	 * this method returns immediately.
	 * 
	 * <li>This bundle's state is set to {@code STOPPING}.
	 * 
	 * <li>A bundle event of type {@link BundleEvent#STOPPING} is fired.
	 * 
	 * <li>If this bundle's state was {@code ACTIVE} prior to setting the state
	 * to {@code STOPPING}, the {@link BundleActivator#stop} method of this
	 * bundle's {@code BundleActivator}, if one is specified, is called. If that
	 * method throws an exception, this method must continue to stop this bundle
	 * and a {@code BundleException} must be thrown after completion of the
	 * remaining steps.
	 * 
	 * <li>Any services registered by this bundle must be unregistered.
	 * <li>Any services used by this bundle must be released.
	 * <li>Any listeners registered by this bundle must be removed.
	 * 
	 * <li>If this bundle's state is {@code UNINSTALLED}, because this bundle
	 * was uninstalled while the {@code BundleActivator.stop} method was
	 * running, a {@code BundleException} must be thrown.
	 * 
	 * <li>This bundle's state is set to {@code RESOLVED}.
	 * 
	 * <li>A bundle event of type {@link BundleEvent#STOPPED} is fired.
	 * </ol>
	 * 
	 * <b>Preconditions </b>
	 * <ul>
	 * <li>{@code getState()} in &#x007B; {@code ACTIVE} &#x007D;.
	 * </ul>
	 * <b>Postconditions, no exceptions thrown </b>
	 * <ul>
	 * <li>Bundle autostart setting is modified unless the
	 * {@link #STOP_TRANSIENT} option was set.
	 * <li>{@code getState()} not in &#x007B; {@code ACTIVE}, {@code STOPPING}
	 * &#x007D;.
	 * <li>{@code BundleActivator.stop} has been called and did not throw an
	 * exception.
	 * </ul>
	 * <b>Postconditions, when an exception is thrown </b>
	 * <ul>
	 * <li>Bundle autostart setting is modified unless the
	 * {@link #STOP_TRANSIENT} option was set.
	 * </ul>
	 * 
	 * @param options The options for stopping this bundle. See
	 *        {@link #STOP_TRANSIENT}. The Framework must ignore unrecognized
	 *        options.
	 * @throws BundleException BundleException types thrown by this method
	 *         include: {@link BundleException#STATECHANGE_ERROR} and
	 *         {@link BundleException#ACTIVATOR_ERROR}.
	 * @throws IllegalStateException If this bundle has been uninstalled or this
	 *         bundle tries to change its own state.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,EXECUTE]}, and the Java Runtime
	 *         Environment supports permissions.
	 * @since 1.4
	 */
	void stop(int options) throws BundleException;

	/**
	 * Stops this bundle with no options.
	 * 
	 * <p>
	 * This method performs the same function as calling {@code stop(0)}.
	 * 
	 * @throws BundleException BundleException types thrown by this method
	 *         include: {@link BundleException#STATECHANGE_ERROR} and
	 *         {@link BundleException#ACTIVATOR_ERROR}.
	 * @throws IllegalStateException If this bundle has been uninstalled or this
	 *         bundle tries to change its own state.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,EXECUTE]}, and the Java Runtime
	 *         Environment supports permissions.
	 * @see #start(int)
	 */
	void stop() throws BundleException;

	/**
	 * Updates this bundle from an {@code InputStream}.
	 * 
	 * <p>
	 * If the specified {@code InputStream} is {@code null}, the Framework must
	 * create the {@code InputStream} from which to read the updated bundle by
	 * interpreting, in an implementation dependent manner, this bundle's
	 * {@link Constants#BUNDLE_UPDATELOCATION Bundle-UpdateLocation} Manifest
	 * header, if present, or this bundle's original location.
	 * 
	 * <p>
	 * If this bundle's state is {@code ACTIVE}, it must be stopped before the
	 * update and started after the update successfully completes.
	 * 
	 * <p>
	 * If this bundle has exported any packages that are imported by another
	 * bundle, these packages must remain exported until the
	 * {@link FrameworkWiring#refreshBundles(java.util.Collection, FrameworkListener...)
	 * FrameworkWiring.refreshBundles} method has been has been called or the
	 * Framework is relaunched.
	 * 
	 * <p>
	 * The following steps are required to update a bundle:
	 * <ol>
	 * <li>If this bundle's state is {@code UNINSTALLED} then an
	 * {@code IllegalStateException} is thrown.
	 * 
	 * <li>If this bundle's state is {@code ACTIVE}, {@code STARTING} or
	 * {@code STOPPING}, this bundle is stopped as described in the
	 * {@code Bundle.stop} method. If {@code Bundle.stop} throws an exception,
	 * the exception is rethrown terminating the update.
	 * 
	 * <li>The updated version of this bundle is read from the input stream and
	 * installed. If the Framework is unable to install the updated version of
	 * this bundle, the original version of this bundle must be restored and a
	 * {@code BundleException} must be thrown after completion of the remaining
	 * steps.
	 * 
	 * <li>This bundle's state is set to {@code INSTALLED}.
	 * 
	 * <li>If the updated version of this bundle was successfully installed, a
	 * bundle event of type {@link BundleEvent#UPDATED} is fired.
	 * 
	 * <li>If this bundle's state was originally {@code ACTIVE}, the updated
	 * bundle is started as described in the {@code Bundle.start} method. If
	 * {@code Bundle.start} throws an exception, a Framework event of type
	 * {@link FrameworkEvent#ERROR} is fired containing the exception.
	 * </ol>
	 * 
	 * <b>Preconditions </b>
	 * <ul>
	 * <li>{@code getState()} not in &#x007B; {@code UNINSTALLED} &#x007D;.
	 * </ul>
	 * <b>Postconditions, no exceptions thrown </b>
	 * <ul>
	 * <li>{@code getState()} in &#x007B; {@code INSTALLED}, {@code RESOLVED},
	 * {@code ACTIVE} &#x007D;.
	 * <li>This bundle has been updated.
	 * </ul>
	 * <b>Postconditions, when an exception is thrown </b>
	 * <ul>
	 * <li>{@code getState()} in &#x007B; {@code INSTALLED}, {@code RESOLVED},
	 * {@code ACTIVE} &#x007D;.
	 * <li>Original bundle is still used; no update occurred.
	 * </ul>
	 * 
	 * @param input The {@code InputStream} from which to read the new bundle or
	 *        {@code null} to indicate the Framework must create the input
	 *        stream from this bundle's {@link Constants#BUNDLE_UPDATELOCATION
	 *        Bundle-UpdateLocation} Manifest header, if present, or this
	 *        bundle's original location. The input stream must always be closed
	 *        when this method completes, even if an exception is thrown.
	 * @throws BundleException If this bundle could not be updated.
	 *         BundleException types thrown by this method include:
	 *         {@link BundleException#READ_ERROR},
	 *         {@link BundleException#DUPLICATE_BUNDLE_ERROR},
	 *         {@link BundleException#MANIFEST_ERROR},
	 *         {@link BundleException#NATIVECODE_ERROR},
	 *         {@link BundleException#RESOLVE_ERROR},
	 *         {@link BundleException#STATECHANGE_ERROR}, and
	 *         {@link BundleException#ACTIVATOR_ERROR}.
	 * @throws IllegalStateException If this bundle has been uninstalled or this
	 *         bundle tries to change its own state.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,LIFECYCLE]} for both the current
	 *         bundle and the updated bundle, and the Java Runtime Environment
	 *         supports permissions.
	 * @see #stop()
	 * @see #start()
	 */
	void update(InputStream input) throws BundleException;

	/**
	 * Updates this bundle.
	 * 
	 * <p>
	 * This method performs the same function as calling
	 * {@link #update(InputStream)} with a {@code null} InputStream.
	 * 
	 * @throws BundleException If this bundle could not be updated.
	 *         BundleException types thrown by this method include:
	 *         {@link BundleException#READ_ERROR},
	 *         {@link BundleException#DUPLICATE_BUNDLE_ERROR},
	 *         {@link BundleException#MANIFEST_ERROR},
	 *         {@link BundleException#NATIVECODE_ERROR},
	 *         {@link BundleException#RESOLVE_ERROR},
	 *         {@link BundleException#STATECHANGE_ERROR}, and
	 *         {@link BundleException#ACTIVATOR_ERROR}.
	 * @throws IllegalStateException If this bundle has been uninstalled or this
	 *         bundle tries to change its own state.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,LIFECYCLE]} for both the current
	 *         bundle and the updated bundle, and the Java Runtime Environment
	 *         supports permissions.
	 * @see #update(InputStream)
	 */
	void update() throws BundleException;

	/**
	 * Uninstalls this bundle.
	 * 
	 * <p>
	 * This method causes the Framework to notify other bundles that this bundle
	 * is being uninstalled, and then puts this bundle into the
	 * {@code UNINSTALLED} state. The Framework must remove any resources
	 * related to this bundle that it is able to remove.
	 * 
	 * <p>
	 * If this bundle has exported any packages, the Framework must continue to
	 * make these packages available to their importing bundles until the
	 * {@link FrameworkWiring#refreshBundles(java.util.Collection, FrameworkListener...)
	 * FrameworkWiring.refreshBundles} method has been called or the Framework
	 * is relaunched.
	 * 
	 * <p>
	 * The following steps are required to uninstall a bundle:
	 * <ol>
	 * <li>If this bundle's state is {@code UNINSTALLED} then an
	 * {@code IllegalStateException} is thrown.
	 * 
	 * <li>If this bundle's state is {@code ACTIVE}, {@code STARTING} or
	 * {@code STOPPING}, this bundle is stopped as described in the
	 * {@code Bundle.stop} method. If {@code Bundle.stop} throws an exception, a
	 * Framework event of type {@link FrameworkEvent#ERROR} is fired containing
	 * the exception.
	 * 
	 * <li>This bundle's state is set to {@code UNINSTALLED}.
	 * 
	 * <li>A bundle event of type {@link BundleEvent#UNINSTALLED} is fired.
	 * 
	 * <li>This bundle and any persistent storage area provided for this bundle
	 * by the Framework are removed.
	 * </ol>
	 * 
	 * <b>Preconditions </b>
	 * <ul>
	 * <li>{@code getState()} not in &#x007B; {@code UNINSTALLED} &#x007D;.
	 * </ul>
	 * <b>Postconditions, no exceptions thrown </b>
	 * <ul>
	 * <li>{@code getState()} in &#x007B; {@code UNINSTALLED} &#x007D;.
	 * <li>This bundle has been uninstalled.
	 * </ul>
	 * <b>Postconditions, when an exception is thrown </b>
	 * <ul>
	 * <li>{@code getState()} not in &#x007B; {@code UNINSTALLED} &#x007D;.
	 * <li>This Bundle has not been uninstalled.
	 * </ul>
	 * 
	 * @throws BundleException If the uninstall failed. This can occur if
	 *         another thread is attempting to change this bundle's state and
	 *         does not complete in a timely manner. BundleException types
	 *         thrown by this method include:
	 *         {@link BundleException#STATECHANGE_ERROR}
	 * @throws IllegalStateException If this bundle has been uninstalled or this
	 *         bundle tries to change its own state.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,LIFECYCLE]}, and the Java Runtime
	 *         Environment supports permissions.
	 * @see #stop()
	 */
	void uninstall() throws BundleException;

	/**
	 * Returns this bundle's Manifest headers and values. This method returns
	 * all the Manifest headers and values from the main section of this
	 * bundle's Manifest file; that is, all lines prior to the first blank line.
	 * 
	 * <p>
	 * Manifest header names are case-insensitive. The methods of the returned
	 * {@code Dictionary} object must operate on header names in a
	 * case-insensitive manner.
	 * 
	 * If a Manifest header value starts with &quot;%&quot;, it must be
	 * localized according to the default locale. If no localization is found
	 * for a header value, the header value without the leading &quot;%&quot; is
	 * returned.
	 * 
	 * <p>
	 * For example, the following Manifest headers and values are included if
	 * they are present in the Manifest file:
	 * 
	 * <pre>
	 *     Bundle-Name
	 *     Bundle-Vendor
	 *     Bundle-Version
	 *     Bundle-Description
	 *     Bundle-DocURL
	 *     Bundle-ContactAddress
	 * </pre>
	 * 
	 * <p>
	 * This method must continue to return Manifest header information while
	 * this bundle is in the {@code UNINSTALLED} state.
	 * 
	 * @return An unmodifiable {@code Dictionary} object containing this
	 *         bundle's Manifest headers and values.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,METADATA]}, and the Java Runtime
	 *         Environment supports permissions.
	 * @see Constants#BUNDLE_LOCALIZATION
	 */
	Dictionary<String, String> getHeaders();

	/**
	 * Returns this bundle's unique identifier. This bundle is assigned a unique
	 * identifier by the Framework when it was installed in the OSGi
	 * environment.
	 * 
	 * <p>
	 * A bundle's unique identifier has the following attributes:
	 * <ul>
	 * <li>Is unique and persistent.
	 * <li>Is a {@code long}.
	 * <li>Its value is not reused for another bundle, even after a bundle is
	 * uninstalled.
	 * <li>Does not change while a bundle remains installed.
	 * <li>Does not change when a bundle is updated.
	 * </ul>
	 * 
	 * <p>
	 * This method must continue to return this bundle's unique identifier while
	 * this bundle is in the {@code UNINSTALLED} state.
	 * 
	 * @return The unique identifier of this bundle.
	 */
	long getBundleId();

	/**
	 * Returns this bundle's location identifier.
	 * 
	 * <p>
	 * The location identifier is the location passed to {@code
	 * BundleContext.installBundle} when a bundle is installed. The location
	 * identifier does not change while this bundle remains installed, even if
	 * this bundle is updated.
	 * 
	 * <p>
	 * This method must continue to return this bundle's location identifier
	 * while this bundle is in the {@code UNINSTALLED} state.
	 * 
	 * @return The string representation of this bundle's location identifier.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,METADATA]}, and the Java Runtime
	 *         Environment supports permissions.
	 */
	String getLocation();

	/**
	 * Returns this bundle's {@code ServiceReference} list for all services it
	 * has registered or {@code null} if this bundle has no registered services.
	 * 
	 * <p>
	 * If the Java runtime supports permissions, a {@code ServiceReference}
	 * object to a service is included in the returned list only if the caller
	 * has the {@code ServicePermission} to get the service using at least one
	 * of the named classes the service was registered under.
	 * 
	 * <p>
	 * The list is valid at the time of the call to this method, however, as the
	 * Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 * 
	 * @return An array of {@code ServiceReference} objects or {@code null}.
	 * @throws IllegalStateException If this bundle has been uninstalled.
	 * @see ServiceRegistration
	 * @see ServiceReference
	 * @see ServicePermission
	 */
	ServiceReference< ? >[] getRegisteredServices();

	/**
	 * Returns this bundle's {@code ServiceReference} list for all services it
	 * is using or returns {@code null} if this bundle is not using any
	 * services. A bundle is considered to be using a service if its use count
	 * for that service is greater than zero.
	 * 
	 * <p>
	 * If the Java Runtime Environment supports permissions, a {@code
	 * ServiceReference} object to a service is included in the returned list
	 * only if the caller has the {@code ServicePermission} to get the service
	 * using at least one of the named classes the service was registered under.
	 * <p>
	 * The list is valid at the time of the call to this method, however, as the
	 * Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 * 
	 * @return An array of {@code ServiceReference} objects or {@code null}.
	 * @throws IllegalStateException If this bundle has been uninstalled.
	 * @see ServiceReference
	 * @see ServicePermission
	 */
	ServiceReference< ? >[] getServicesInUse();

	/**
	 * Determines if this bundle has the specified permissions.
	 * 
	 * <p>
	 * If the Java Runtime Environment does not support permissions, this method
	 * always returns {@code true}.
	 * <p>
	 * {@code permission} is of type {@code Object} to avoid referencing the
	 * {@code java.security.Permission} class directly. This is to allow the
	 * Framework to be implemented in Java environments which do not support
	 * permissions.
	 * 
	 * <p>
	 * If the Java Runtime Environment does support permissions, this bundle and
	 * all its resources including embedded JAR files, belong to the same
	 * {@code java.security.ProtectionDomain}; that is, they must share the same
	 * set of permissions.
	 * 
	 * @param permission The permission to verify.
	 * @return {@code true} if this bundle has the specified permission or the
	 *         permissions possessed by this bundle imply the specified
	 *         permission; {@code false} if this bundle does not have the
	 *         specified permission or {@code permission} is not an {@code
	 *         instanceof} {@code java.security.Permission}.
	 * @throws IllegalStateException If this bundle has been uninstalled.
	 */
	boolean hasPermission(Object permission);

	/**
	 * Find the specified resource from this bundle's class loader.
	 * 
	 * This bundle's class loader is called to search for the specified
	 * resource. If this bundle's state is {@code INSTALLED}, this method must
	 * attempt to resolve this bundle before attempting to get the specified
	 * resource. If this bundle cannot be resolved, then only this bundle must
	 * be searched for the specified resource. Imported packages cannot be
	 * searched when this bundle has not been resolved. If this bundle is a
	 * fragment bundle then {@code null} is returned.
	 * <p>
	 * Note: Jar and zip files are not required to include directory entries.
	 * URLs to directory entries will not be returned if the bundle contents do
	 * not contain directory entries.
	 * 
	 * @param name The name of the resource. See {@code ClassLoader.getResource}
	 *        for a description of the format of a resource name.
	 * @return A URL to the named resource, or {@code null} if the resource
	 *         could not be found or if this bundle is a fragment bundle or if
	 *         the caller does not have the appropriate {@code
	 *         AdminPermission[this,RESOURCE]}, and the Java Runtime Environment
	 *         supports permissions.
	 * @throws IllegalStateException If this bundle has been uninstalled.
	 * @see #getEntry
	 * @see #findEntries
	 * @since 1.1
	 */
	URL getResource(String name);

	/**
	 * Returns this bundle's Manifest headers and values localized to the
	 * specified locale.
	 * 
	 * <p>
	 * This method performs the same function as {@code Bundle.getHeaders()}
	 * except the manifest header values are localized to the specified locale.
	 * 
	 * <p>
	 * If a Manifest header value starts with &quot;%&quot;, it must be
	 * localized according to the specified locale. If a locale is specified and
	 * cannot be found, then the header values must be returned using the
	 * default locale. Localizations are searched for in the following order:
	 * 
	 * <pre>
	 *   bn + &quot;_&quot; + Ls + &quot;_&quot; + Cs + &quot;_&quot; + Vs
	 *   bn + &quot;_&quot; + Ls + &quot;_&quot; + Cs
	 *   bn + &quot;_&quot; + Ls
	 *   bn + &quot;_&quot; + Ld + &quot;_&quot; + Cd + &quot;_&quot; + Vd
	 *   bn + &quot;_&quot; + Ld + &quot;_&quot; + Cd
	 *   bn + &quot;_&quot; + Ld
	 *   bn
	 * </pre>
	 * 
	 * Where {@code bn} is this bundle's localization basename, {@code Ls},
	 * {@code Cs} and {@code Vs} are the specified locale (language, country,
	 * variant) and {@code Ld}, {@code Cd} and {@code Vd} are the default locale
	 * (language, country, variant).
	 * 
	 * If {@code null} is specified as the locale string, the header values must
	 * be localized using the default locale. If the empty string (&quot;&quot;)
	 * is specified as the locale string, the header values must not be
	 * localized and the raw (unlocalized) header values, including any leading
	 * &quot;%&quot;, must be returned. If no localization is found for a header
	 * value, the header value without the leading &quot;%&quot; is returned.
	 * 
	 * <p>
	 * This method must continue to return Manifest header information while
	 * this bundle is in the {@code UNINSTALLED} state, however the header
	 * values must only be available in the raw and default locale values.
	 * 
	 * @param locale The locale name into which the header values are to be
	 *        localized. If the specified locale is {@code null} then the locale
	 *        returned by {@code java.util.Locale.getDefault} is used. If the
	 *        specified locale is the empty string, this method will return the
	 *        raw (unlocalized) manifest headers including any leading
	 *        &quot;%&quot;.
	 * @return An unmodifiable {@code Dictionary} object containing this
	 *         bundle's Manifest headers and values.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,METADATA]}, and the Java Runtime
	 *         Environment supports permissions.
	 * @see #getHeaders()
	 * @see Constants#BUNDLE_LOCALIZATION
	 * @since 1.3
	 */
	Dictionary<String, String> getHeaders(String locale);

	/**
	 * Returns the symbolic name of this bundle as specified by its {@code
	 * Bundle-SymbolicName} manifest header. The bundle symbolic name should be
	 * based on the reverse domain name naming convention like that used for
	 * java packages.
	 * 
	 * <p>
	 * This method must continue to return this bundle's symbolic name while
	 * this bundle is in the {@code UNINSTALLED} state.
	 * 
	 * @return The symbolic name of this bundle or {@code null} if this bundle
	 *         does not have a symbolic name.
	 * @since 1.3
	 */
	String getSymbolicName();

	/**
	 * Loads the specified class using this bundle's class loader.
	 * 
	 * <p>
	 * If this bundle is a fragment bundle then this method must throw a {@code
	 * ClassNotFoundException}.
	 * 
	 * <p>
	 * If this bundle's state is {@code INSTALLED}, this method must attempt to
	 * resolve this bundle before attempting to load the class.
	 * 
	 * <p>
	 * If this bundle cannot be resolved, a Framework event of type
	 * {@link FrameworkEvent#ERROR} is fired containing a {@code
	 * BundleException} with details of the reason this bundle could not be
	 * resolved. This method must then throw a {@code ClassNotFoundException}.
	 * 
	 * <p>
	 * If this bundle's state is {@code UNINSTALLED}, then an {@code
	 * IllegalStateException} is thrown.
	 * 
	 * @param name The name of the class to load.
	 * @return The Class object for the requested class.
	 * @throws ClassNotFoundException If no such class can be found or if this
	 *         bundle is a fragment bundle or if the caller does not have the
	 *         appropriate {@code AdminPermission[this,CLASS]}, and the Java
	 *         Runtime Environment supports permissions.
	 * @throws IllegalStateException If this bundle has been uninstalled.
	 * @since 1.3
	 */
	Class< ? > loadClass(String name) throws ClassNotFoundException;

	/**
	 * Find the specified resources from this bundle's class loader.
	 * 
	 * This bundle's class loader is called to search for the specified
	 * resources. If this bundle's state is {@code INSTALLED}, this method must
	 * attempt to resolve this bundle before attempting to get the specified
	 * resources. If this bundle cannot be resolved, then only this bundle must
	 * be searched for the specified resources. Imported packages cannot be
	 * searched when a bundle has not been resolved. If this bundle is a
	 * fragment bundle then {@code null} is returned.
	 * <p>
	 * Note: Jar and zip files are not required to include directory entries.
	 * URLs to directory entries will not be returned if the bundle contents do
	 * not contain directory entries.
	 * 
	 * @param name The name of the resource. See {@code
	 *        ClassLoader.getResources} for a description of the format of a
	 *        resource name.
	 * @return An enumeration of URLs to the named resources, or {@code null} if
	 *         the resource could not be found or if this bundle is a fragment
	 *         bundle or if the caller does not have the appropriate {@code
	 *         AdminPermission[this,RESOURCE]}, and the Java Runtime Environment
	 *         supports permissions.
	 * @throws IllegalStateException If this bundle has been uninstalled.
	 * @throws IOException If there is an I/O error.
	 * @since 1.3
	 */
	Enumeration<URL> getResources(String name) throws IOException;

	/**
	 * Returns an Enumeration of all the paths ({@code String} objects) to
	 * entries within this bundle whose longest sub-path matches the specified
	 * path. This bundle's class loader is not used to search for entries. Only
	 * the contents of this bundle are searched.
	 * <p>
	 * The specified path is always relative to the root of this bundle and may
	 * begin with a &quot;/&quot;. A path value of &quot;/&quot; indicates the
	 * root of this bundle.
	 * <p>
	 * Returned paths indicating subdirectory paths end with a &quot;/&quot;.
	 * The returned paths are all relative to the root of this bundle and must
	 * not begin with &quot;/&quot;.
	 * <p>
	 * Note: Jar and zip files are not required to include directory entries.
	 * Paths to directory entries will not be returned if the bundle contents do
	 * not contain directory entries.
	 * 
	 * @param path The path name for which to return entry paths.
	 * @return An Enumeration of the entry paths ({@code String} objects) or
	 *         {@code null} if no entry could be found or if the caller does not
	 *         have the appropriate {@code AdminPermission[this,RESOURCE]} and
	 *         the Java Runtime Environment supports permissions.
	 * @throws IllegalStateException If this bundle has been uninstalled.
	 * @since 1.3
	 */
	Enumeration<String> getEntryPaths(String path);

	/**
	 * Returns a URL to the entry at the specified path in this bundle. This
	 * bundle's class loader is not used to search for the entry. Only the
	 * contents of this bundle are searched for the entry.
	 * <p>
	 * The specified path is always relative to the root of this bundle and may
	 * begin with &quot;/&quot;. A path value of &quot;/&quot; indicates the
	 * root of this bundle.
	 * <p>
	 * Note: Jar and zip files are not required to include directory entries.
	 * URLs to directory entries will not be returned if the bundle contents do
	 * not contain directory entries.
	 * 
	 * @param path The path name of the entry.
	 * @return A URL to the entry, or {@code null} if no entry could be found or
	 *         if the caller does not have the appropriate {@code
	 *         AdminPermission[this,RESOURCE]} and the Java Runtime Environment
	 *         supports permissions.
	 * @throws IllegalStateException If this bundle has been uninstalled.
	 * @since 1.3
	 */
	URL getEntry(String path);

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
	long getLastModified();

	/**
	 * Returns entries in this bundle and its attached fragments. This bundle's
	 * class loader is not used to search for entries. Only the contents of this
	 * bundle and its attached fragments are searched for the specified entries.
	 * 
	 * If this bundle's state is {@code INSTALLED}, this method must attempt to
	 * resolve this bundle before attempting to find entries.
	 * 
	 * <p>
	 * This method is intended to be used to obtain configuration, setup,
	 * localization and other information from this bundle. This method takes
	 * into account that the &quot;contents&quot; of this bundle can be extended
	 * with fragments. This &quot;bundle space&quot; is not a name space with
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
	 * 
	 * <pre>
	 * // List all XML files in the OSGI-INF directory and below
	 * Enumeration e = b.findEntries(&quot;OSGI-INF&quot;, &quot;*.xml&quot;, true);
	 * 
	 * // Find a specific localization file
	 * Enumeration e = b
	 * 		.findEntries(&quot;OSGI-INF/l10n&quot;, &quot;bundle_nl_DU.properties&quot;, false);
	 * if (e.hasMoreElements())
	 * 	return (URL) e.nextElement();
	 * </pre>
	 * 
	 * <p>
	 * Note: Jar and zip files are not required to include directory entries.
	 * URLs to directory entries will not be returned if the bundle contents do
	 * not contain directory entries.
	 * 
	 * @param path The path name in which to look. The path is always relative
	 *        to the root of this bundle and may begin with &quot;/&quot;. A
	 *        path value of &quot;/&quot; indicates the root of this bundle.
	 * @param filePattern The file name pattern for selecting entries in the
	 *        specified path. The pattern is only matched against the last
	 *        element of the entry path. If the entry is a directory then the
	 *        trailing &quot;/&quot; is not used for pattern matching. Substring
	 *        matching is supported, as specified in the Filter specification,
	 *        using the wildcard character (&quot;*&quot;). If null is
	 *        specified, this is equivalent to &quot;*&quot; and matches all
	 *        files.
	 * @param recurse If {@code true}, recurse into subdirectories. Otherwise
	 *        only return entries from the specified path.
	 * @return An enumeration of URL objects for each matching entry, or
	 *         {@code null} if no matching entry could not be found or if the
	 *         caller does not have the appropriate
	 *         {@code AdminPermission[this,RESOURCE]}, and the Java Runtime
	 *         Environment supports permissions. The URLs are sorted such that
	 *         entries from this bundle are returned first followed by the
	 *         entries from attached fragments in attachment order. If this
	 *         bundle is a fragment, then only matching entries in this fragment
	 *         are returned.
	 * @throws IllegalStateException If this bundle has been uninstalled.
	 * @since 1.3
	 */
	Enumeration<URL> findEntries(String path, String filePattern,
			boolean recurse);

	/**
	 * Returns this bundle's {@link BundleContext}. The returned {@code
	 * BundleContext} can be used by the caller to act on behalf of this bundle.
	 * 
	 * <p>
	 * If this bundle is not in the {@link #STARTING}, {@link #ACTIVE}, or
	 * {@link #STOPPING} states or this bundle is a fragment bundle, then this
	 * bundle has no valid {@code BundleContext}. This method will return
	 * {@code null} if this bundle has no valid {@code BundleContext}.
	 * 
	 * @return A {@code BundleContext} for this bundle or {@code null} if this
	 *         bundle has no valid {@code BundleContext}.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdminPermission[this,CONTEXT]}, and the Java Runtime
	 *         Environment supports permissions.
	 * @since 1.4
	 */
	BundleContext getBundleContext();

	/**
	 * Return the certificates for the signers of this bundle and the
	 * certificate chains for those signers.
	 * 
	 * @param signersType If {@link #SIGNERS_ALL} is specified, then information
	 *        on all signers of this bundle is returned. If
	 *        {@link #SIGNERS_TRUSTED} is specified, then only information on
	 *        the signers of this bundle trusted by the framework is returned.
	 * @return The {@code X509Certificate}s for the signers of this bundle and
	 *         the {@code X509Certificate} chains for those signers. The keys of
	 *         the {@code Map} are the {@code X509Certificate}s of the signers
	 *         of this bundle. The value for a key is a {@code List} containing
	 *         the {@code X509Certificate} chain for the signer. The first item
	 *         in the {@code List} is the signer's {@code X509Certificate} which
	 *         is then followed by the rest of the {@code X509Certificate}
	 *         chain. The returned {@code Map} will be empty if there are no
	 *         signers. The returned {@code Map} is the property of the caller
	 *         who is free to modify it.
	 * @throws IllegalArgumentException If the specified {@code signersType} is
	 *         not {@link #SIGNERS_ALL} or {@link #SIGNERS_TRUSTED}.
	 * @since 1.5
	 */
	Map<X509Certificate, List<X509Certificate>> getSignerCertificates(
			int signersType);

	/**
	 * Returns the version of this bundle as specified by its {@code
	 * Bundle-Version} manifest header. If this bundle does not have a specified
	 * version then {@link Version#emptyVersion} is returned.
	 * 
	 * <p>
	 * This method must continue to return this bundle's version while this
	 * bundle is in the {@code UNINSTALLED} state.
	 * 
	 * @return The version of this bundle.
	 * @since 1.5
	 */
	Version getVersion();

	/**
	 * Adapt this bundle to the specified type.
	 * 
	 * <p>
	 * Adapting this bundle to the specified type may require certain checks,
	 * including security checks, to succeed. If a check does not succeed, then
	 * this bundle cannot be adapted and {@code null} is returned.
	 * 
	 * @param <A> The type to which this bundle is to be adapted.
	 * @param type Class object for the type to which this bundle is to be
	 *        adapted.
	 * @return The object, of the specified type, to which this bundle has been
	 *         adapted or {@code null} if this bundle cannot be adapted to the
	 *         specified type.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code AdaptPermission[type,this,ADAPT]}, and the Java Runtime
	 *         Environment supports permissions.
	 * @since 1.6
	 */
	<A> A adapt(Class<A> type);

	/**
	 * Creates a {@code File} object for a file in the persistent storage area
	 * provided for this bundle by the Framework. This method will return
	 * {@code null} if the platform does not have file system support or this
	 * bundle is a fragment bundle.
	 * 
	 * <p>
	 * A {@code File} object for the base directory of the persistent storage
	 * area provided for this bundle by the Framework can be obtained by calling
	 * this method with an empty string as {@code filename}.
	 * 
	 * <p>
	 * If the Java Runtime Environment supports permissions, the Framework will
	 * ensure that this bundle has the {@code java.io.FilePermission} with
	 * actions {@code read},{@code write},{@code delete} for all files
	 * (recursively) in the persistent storage area provided for this bundle.
	 * 
	 * @param filename A relative name to the file to be accessed.
	 * @return A {@code File} object that represents the requested file or
	 *         {@code null} if the platform does not have file system support or
	 *         this bundle is a fragment bundle.
	 * @throws IllegalStateException If this bundle has been uninstalled.
	 * @since 1.6
	 */
	File getDataFile(String filename);
}
