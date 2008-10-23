/*
 * Copyright (c) OSGi Alliance (2008). All Rights Reserved.
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

package org.osgi.framework.launch;

import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;

/**
 * The System Bundle for a Framework instance.
 * 
 * The <i>main</i> class of a framework implementation must implement this
 * interface. The instantiator of the framework implementation class then has a
 * System Bundle object and can then use the methods of this interface to manage
 * and control the created framework instance.
 * 
 * <p>
 * The <i>main</i> class of a framework implementation must provide a public
 * constructor that takes a single argument of type <code>Map</code>. This
 * configuration argument provides this System Bundle with framework properties
 * to configure the framework instance. The framework instance must also examine
 * the system properties for framework properties which are not set in the
 * configuration argument. A framework property in the configuration argument
 * with a <code>null</code> value indicates the framework property is
 * <i>unset</i> and the system properties must not be examined for a value of
 * the framework property. This allows the configuration argument to
 * <i>unset</i> framework properties in the system properties .
 * <p>
 * If framework properties are not provided by the configuration argument or the
 * system properties, this System Bundle must use some reasonable default
 * configuration appropriate for the current VM. For example, the system
 * packages for the current execution environment should be properly exported.
 * The configuration argument may be <code>null</code>. The framework instance
 * must copy any information needed from the configuration argument since the
 * configuration argument can be changed after the framework instance has been
 * created.
 * 
 * <p>
 * A newly constructed System Bundle must be in the {@link #INSTALLED} state.
 * 
 * @ThreadSafe
 * @version $Revision: 5686 $
 */
public interface Framework extends Bundle {

	/**
	 * Initialize this System Bundle. After calling this method, this System
	 * Bundle must:
	 * <ul>
	 * <li>Be in the {@link #STARTING} state.</li>
	 * <li>Have a valid Bundle Context.</li>
	 * <li>Have its framework instance be at start level 0.</li>
	 * <li>Have event handling enabled in its framework instance.</li>
	 * <li>Have reified Bundle objects for all bundles installed in its
	 * framework instance.</li>
	 * <li>Register framework services. For example, <code>PackageAdmin</code>,
	 * <code>ConditionalPermissionAdmin</code>, <code>StartLevel</code>.</li>
	 * </ul>
	 * 
	 * <p>
	 * This System Bundle will not actually be started until {@link #start()
	 * start} is called.
	 * 
	 * <p>
	 * This method does nothing if called when this System Bundle is in the
	 * {@link #STARTING}, {@link #ACTIVE} or {@link #STOPPING} states.
	 * 
	 * @throws BundleException If this System Bundle could not be initialized.
	 */
	public void init() throws BundleException;

	/**
	 * Wait until this System Bundle has completely stopped. The
	 * <code>stop</code> and <code>update</code> methods on a System Bundle
	 * performs an asynchronous stop of the System Bundle. This method can be
	 * used to wait until the asynchronous stop of this System Bundle has
	 * completed. This method will only wait if called when this System Bundle
	 * is in the {@link #STARTING}, {@link #ACTIVE}, or {@link #STOPPING}
	 * states. Otherwise it will return immediately.
	 * <p>
	 * A Framework Event is returned to indicate why this System Bundle has
	 * stopped.
	 * 
	 * @param timeout Maximum number of milliseconds to wait until this System
	 *        Bundle has completely stopped. A value of zero will wait
	 *        indefinitely.
	 * @return A Framework Event indicating the reason this method returned. The
	 *         following <code>FrameworkEvent</code> types may be returned by
	 *         this method.
	 *         <ul>
	 *         <li>{@link FrameworkEvent#STOPPED STOPPED} - This System Bundle
	 *         has been stopped which has shutdown its framework instance. </li>
	 *         <li>{@link FrameworkEvent#STOPPED_UPDATE STOPPED_UPDATE} - This
	 *         System Bundle has been updated which has shutdown and will
	 *         restart its framework instance.</li> <li>
	 *         {@link FrameworkEvent#STOPPED_BOOTCLASSPATH_MODIFIED
	 *         STOPPED_BOOTCLASSPATH_MODIFIED} - This System Bundle has been
	 *         stopped which has shutdown its framework instance and a
	 *         bootclasspath extension bundle has been installed or updated. The
	 *         VM must be restarted in order for the changed boot class path to
	 *         take affect. </li> <li>{@link FrameworkEvent#ERROR ERROR} - The
	 *         Framework encountered an error while shutting down or an error
	 *         has occurred which forced the framework to shutdown. </li> <li>
	 *         {@link FrameworkEvent#INFO INFO} - This method has timed out and
	 *         returned before this System Bundle has stopped.</li>
	 *         </ul>
	 * @throws InterruptedException If another thread interrupted the current
	 *         thread before or while the current thread was waiting for this
	 *         System Bundle to completely stop. The <i>interrupted status</i>
	 *         of the current thread is cleared when this exception is thrown.
	 * @throws IllegalArgumentException If the value of timeout is negative.
	 */
	public FrameworkEvent waitForStop(long timeout) throws InterruptedException;

	/**
	 * Start this System Bundle.
	 * 
	 * <p>
	 * The following steps are taken to start this System Bundle:
	 * <ol>
	 * <li>If this System Bundle is not in the {@link #STARTING} state,
	 * {@link #init() initialize} this System Bundle.</li>
	 * <li>All installed bundles must be started in accordance with each
	 * bundle's persistent <i>autostart setting</i>. This means some bundles
	 * will not be started, some will be started with <i>eager activation</i>
	 * and some will be started with their <i>declared activation</i> policy.
	 * <ul>
	 * <li>If this System Bundle implements the optional <i>Start Level Service
	 * Specification</i>, then the start level of this System Bundle's framework
	 * instance is moved to the start level specified by the
	 * {@link Constants#FRAMEWORK_BEGINNING_STARTLEVEL beginning start level}
	 * framework property, as described in the <i>Start Level Service
	 * Specification</i>. If this framework property is not specified, then the
	 * start level of this System Bundle's framework instance is moved to start
	 * level one (1).</li>
	 * </ul>
	 * Any exceptions that occur during bundle starting must be wrapped in a
	 * {@link BundleException} and then published as a framework event of type
	 * {@link FrameworkEvent#ERROR}</li>
	 * </li>
	 * <li>This System Bundle's state is set to {@link #ACTIVE}.</li>
	 * <li>A framework event of type {@link FrameworkEvent#STARTED} is fired</li>
	 * </ol>
	 * 
	 * @throws BundleException If this System Bundle could not be started.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[this,EXECUTE]</code>, and the
	 *         Java Runtime Environment supports permissions.
	 * @see "Start Level Service Specification"
	 */
	public void start() throws BundleException;

	/**
	 * Start this System Bundle.
	 * 
	 * <p>
	 * Calling this method is the same as calling {@link #start()}. There are no
	 * start options for the System Bundle.
	 * 
	 * @param options Ignored. There are no start options for the System Bundle.
	 * @throws BundleException If this System Bundle could not be started.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[this,EXECUTE]</code>, and the
	 *         Java Runtime Environment supports permissions.
	 * @see #start()
	 */
	public void start(int options) throws BundleException;

	/**
	 * Stop this System Bundle.
	 * 
	 * <p>
	 * The method returns immediately to the caller after initiating the
	 * following steps to be taken on another thread.
	 * <ol>
	 * <li>This System Bundle's state is set to {@link #STOPPING}.</li>
	 * <li>All installed bundles must be stopped without changing each bundle's
	 * persistent <i>autostart setting</i>.
	 * <ul>
	 * <li>If this System Bundle implements the optional <i>Start Level Service
	 * Specification</i>, then the start level of this System Bundle's framework
	 * instance is moved to start level zero (0), as described in the <i>Start
	 * Level Service Specification</i>.</li>
	 * </ul>
	 * Any exceptions that occur during bundle stopping must be wrapped in a
	 * {@link BundleException} and then published as a framework event of type
	 * {@link FrameworkEvent#ERROR}</li>
	 * </li>
	 * <li>Unregister all services registered by this System Bundle.</li>
	 * <li>Event handling is disabled in this System Bundle's framework
	 * instance.</li>
	 * <li>This System Bundle's state is set to {@link #RESOLVED}.</li>
	 * <li>All resources held by this System Bundle's framework instance are
	 * released. This includes threads, bundle class loaders, open files, etc.</li>
	 * <li>Notify all threads that are waiting at {@link #waitForStop(long)
	 * waitForStop} that the stop operation has completed.</li>
	 * </ol>
	 * <p>
	 * After being stopped, this System Bundle may be discarded, initialized or
	 * started.
	 * 
	 * @throws BundleException If stopping this System Bundle could not be
	 *         initiated.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[this,EXECUTE]</code>, and the
	 *         Java Runtime Environment supports permissions.
	 * @see "Start Level Service Specification"
	 */
	public void stop() throws BundleException;

	/**
	 * Stop this System Bundle.
	 * 
	 * <p>
	 * Calling this method is the same as calling {@link #stop()}. There are no
	 * stop options for the System Bundle.
	 * 
	 * @param options Ignored. There are no stop options for the System Bundle.
	 * @throws BundleException If stopping this System Bundle could not be
	 *         initiated.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[this,EXECUTE]</code>, and the
	 *         Java Runtime Environment supports permissions.
	 * @see #stop()
	 */
	public void stop(int options) throws BundleException;

	/**
	 * The System Bundle cannot be uninstalled.
	 * 
	 * <p>
	 * This method always throws a BundleException.
	 * 
	 * @throws BundleException This System Bundle cannot be uninstalled.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[this,LIFECYCLE]</code>, and the
	 *         Java Runtime Environment supports permissions.
	 */
	public void uninstall() throws BundleException;

	/**
	 * Stop and restart this System Bundle.
	 * 
	 * <p>
	 * The method returns immediately to the caller after initiating the
	 * following steps to be taken on another thread.
	 * <ol>
	 * <li>Perform the steps in the {@link #stop()} method to stop this System
	 * Bundle.</li>
	 * <li>Perform the steps in the {@link #start()} method to start this System
	 * Bundle.</li>
	 * </ol>
	 * 
	 * @throws BundleException If stopping and restarting this System Bundle
	 *         could not be initiated.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[this,LIFECYCLE]</code>, and the
	 *         Java Runtime Environment supports permissions.
	 */
	public void update() throws BundleException;

	/**
	 * Stop and restart this System Bundle.
	 * 
	 * <p>
	 * Calling this method is the same as calling {@link #update()} except that
	 * any provided InputStream is immediately closed.
	 * 
	 * @param in Any provided InputStream is immediately closed before returning
	 *        from this method and otherwise ignored.
	 * @throws BundleException If stopping and restarting this System Bundle
	 *         could not be initiated.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[this,LIFECYCLE]</code>, and the
	 *         Java Runtime Environment supports permissions.
	 */
	public void update(InputStream in) throws BundleException;

	/**
	 * Returns the System Bundle unique identifier. This System Bundle is
	 * assigned the unique identifier zero (0).
	 * 
	 * @return 0.
	 * @see Bundle#getBundleId()
	 */
	public long getBundleId();

	/**
	 * Returns the System Bundle location identifier.
	 * 
	 * @return The string &quot;<code>System Bundle</code>&quot;.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[this,METADATA]</code>, and the
	 *         Java Runtime Environment supports permissions.
	 * @see Bundle#getLocation()
	 * @see Constants#SYSTEM_BUNDLE_LOCATION
	 */
	public String getLocation();

	/**
	 * Returns the symbolic name of this System Bundle. The symbolic name is
	 * unique for the implementation of the framework. However, the symbolic
	 * name &quot;<code>system.bundle</code>&quot; must be recognized as an
	 * alias to the implementation-defined symbolic name.
	 * 
	 * @return The symbolic name of this System Bundle.
	 * @see Bundle#getSymbolicName()
	 * @see Constants#SYSTEM_BUNDLE_SYMBOLICNAME
	 */
	public String getSymbolicName();
}
