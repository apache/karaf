/*
 * Copyright (c) OSGi Alliance (2002, 2009). All Rights Reserved.
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

package org.osgi.service.startlevel;

import org.osgi.framework.Bundle;

/**
 * The StartLevel service allows management agents to manage a start level
 * assigned to each bundle and the active start level of the Framework. There is
 * at most one StartLevel service present in the OSGi environment.
 * 
 * <p>
 * A start level is defined to be a state of execution in which the Framework
 * exists. StartLevel values are defined as unsigned integers with 0 (zero)
 * being the state where the Framework is not launched. Progressively higher
 * integral values represent progressively higher start levels. e.g. 2 is a
 * higher start level than 1.
 * <p>
 * Access to the StartLevel service is protected by corresponding
 * <code>ServicePermission</code>. In addition <code>AdminPermission</code>
 * is required to actually modify start level information.
 * <p>
 * Start Level support in the Framework includes the ability to control the
 * beginning start level of the Framework, to modify the active start level of
 * the Framework and to assign a specific start level to a bundle. How the
 * beginning start level of a Framework is specified is implementation
 * dependent. It may be a command line argument when invoking the Framework
 * implementation.
 * <p>
 * When the Framework is first started it must be at start level zero. In this
 * state, no bundles are running. This is the initial state of the Framework
 * before it is launched.
 * 
 * When the Framework is launched, the Framework will enter start level one and
 * all bundles which are assigned to start level one and whose autostart setting
 * indicates the bundle should be started are started as described in the
 * <code>Bundle.start</code> method. The Framework will continue to increase
 * the start level, starting bundles at each start level, until the Framework
 * has reached a beginning start level. At this point the Framework has
 * completed starting bundles and will then fire a Framework event of type
 * <code>FrameworkEvent.STARTED</code> to announce it has completed its
 * launch.
 * 
 * <p>
 * Within a start level, bundles may be started in an order defined by the
 * Framework implementation. This may be something like ascending
 * <code>Bundle.getBundleId</code> order or an order based upon dependencies
 * between bundles. A similar but reversed order may be used when stopping
 * bundles within a start level.
 * 
 * <p>
 * The StartLevel service can be used by management bundles to alter the active
 * start level of the framework.
 * 
 * @ThreadSafe
 * @version $Revision: 6747 $
 */
public interface StartLevel {
	/**
	 * Return the active start level value of the Framework.
	 * 
	 * If the Framework is in the process of changing the start level this
	 * method must return the active start level if this differs from the
	 * requested start level.
	 * 
	 * @return The active start level value of the Framework.
	 */
	public int getStartLevel();

	/**
	 * Modify the active start level of the Framework.
	 * 
	 * <p>
	 * The Framework will move to the requested start level. This method will
	 * return immediately to the caller and the start level change will occur
	 * asynchronously on another thread.
	 * 
	 * <p>
	 * If the specified start level is higher than the active start level, the
	 * Framework will continue to increase the start level until the Framework
	 * has reached the specified start level.
	 * 
	 * At each intermediate start level value on the way to and including the
	 * target start level, the Framework must:
	 * <ol>
	 * <li>Change the active start level to the intermediate start level value.
	 * <li>Start bundles at the intermediate start level whose autostart
	 * setting indicate they must be started. They are started as described in
	 * the {@link Bundle#start(int)} method using the
	 * {@link Bundle#START_TRANSIENT} option. The
	 * {@link Bundle#START_ACTIVATION_POLICY} option must also be used if
	 * {@link #isBundleActivationPolicyUsed(Bundle)} returns <code>true</code>
	 * for the bundle.
	 * </ol>
	 * When this process completes after the specified start level is reached,
	 * the Framework will fire a Framework event of type
	 * <code>FrameworkEvent.STARTLEVEL_CHANGED</code> to announce it has moved
	 * to the specified start level.
	 * 
	 * <p>
	 * If the specified start level is lower than the active start level, the
	 * Framework will continue to decrease the start level until the Framework
	 * has reached the specified start level.
	 * 
	 * At each intermediate start level value on the way to and including the
	 * specified start level, the framework must:
	 * <ol>
	 * <li>Stop bundles at the intermediate start level as described in the
	 * {@link Bundle#stop(int)} method using the {@link Bundle#STOP_TRANSIENT}
	 * option.
	 * <li>Change the active start level to the intermediate start level value.
	 * </ol>
	 * When this process completes after the specified start level is reached,
	 * the Framework will fire a Framework event of type
	 * <code>FrameworkEvent.STARTLEVEL_CHANGED</code> to announce it has moved
	 * to the specified start level.
	 * 
	 * <p>
	 * If the specified start level is equal to the active start level, then no
	 * bundles are started or stopped, however, the Framework must fire a
	 * Framework event of type <code>FrameworkEvent.STARTLEVEL_CHANGED</code>
	 * to announce it has finished moving to the specified start level. This
	 * event may arrive before this method return.
	 * 
	 * @param startlevel The requested start level for the Framework.
	 * @throws IllegalArgumentException If the specified start level is less
	 *         than or equal to zero.
	 * @throws SecurityException If the caller does not have
	 *         <code>AdminPermission[System Bundle,STARTLEVEL]</code> and the
	 *         Java runtime environment supports permissions.
	 */
	public void setStartLevel(int startlevel);

	/**
	 * Return the assigned start level value for the specified Bundle.
	 * 
	 * @param bundle The target bundle.
	 * @return The start level value of the specified Bundle.
	 * @throws java.lang.IllegalArgumentException If the specified bundle has
	 *         been uninstalled or if the specified bundle was not created by
	 *         the same framework instance that registered this
	 *         <code>StartLevel</code> service.
	 */
	public int getBundleStartLevel(Bundle bundle);

	/**
	 * Assign a start level value to the specified Bundle.
	 * 
	 * <p>
	 * The specified bundle will be assigned the specified start level. The
	 * start level value assigned to the bundle will be persistently recorded by
	 * the Framework.
	 * <p>
	 * If the new start level for the bundle is lower than or equal to the
	 * active start level of the Framework and the bundle's autostart setting
	 * indicates the bundle must be started, the Framework will start the
	 * specified bundle as described in the {@link Bundle#start(int)} method
	 * using the {@link Bundle#START_TRANSIENT} option. The
	 * {@link Bundle#START_ACTIVATION_POLICY} option must also be used if
	 * {@link #isBundleActivationPolicyUsed(Bundle)} returns <code>true</code>
	 * for the bundle. The actual starting of this bundle must occur
	 * asynchronously.
	 * <p>
	 * If the new start level for the bundle is higher than the active start
	 * level of the Framework, the Framework will stop the specified bundle as
	 * described in the {@link Bundle#stop(int)} method using the
	 * {@link Bundle#STOP_TRANSIENT} option. The actual stopping of this bundle
	 * must occur asynchronously.
	 * 
	 * @param bundle The target bundle.
	 * @param startlevel The new start level for the specified Bundle.
	 * @throws IllegalArgumentException If the specified bundle has been
	 *         uninstalled, or if the specified start level is less than or
	 *         equal to zero, or if the specified bundle is the system bundle,
	 *         or if the specified bundle was not created by the same framework
	 *         instance that registered this <code>StartLevel</code> service.
	 * @throws SecurityException If the caller does not have
	 *         <code>AdminPermission[bundle,EXECUTE]</code> and the Java runtime
	 *         environment supports permissions.
	 */
	public void setBundleStartLevel(Bundle bundle, int startlevel);

	/**
	 * Return the initial start level value that is assigned to a Bundle when it
	 * is first installed.
	 * 
	 * @return The initial start level value for Bundles.
	 * @see #setInitialBundleStartLevel
	 */
	public int getInitialBundleStartLevel();

	/**
	 * Set the initial start level value that is assigned to a Bundle when it is
	 * first installed.
	 * 
	 * <p>
	 * The initial bundle start level will be set to the specified start level.
	 * The initial bundle start level value will be persistently recorded by the
	 * Framework.
	 * 
	 * <p>
	 * When a Bundle is installed via <code>BundleContext.installBundle</code>,
	 * it is assigned the initial bundle start level value.
	 * 
	 * <p>
	 * The default initial bundle start level value is 1 unless this method has
	 * been called to assign a different initial bundle start level value.
	 * 
	 * <p>
	 * This method does not change the start level values of installed bundles.
	 * 
	 * @param startlevel The initial start level for newly installed bundles.
	 * @throws IllegalArgumentException If the specified start level is less
	 *         than or equal to zero.
	 * @throws SecurityException If the caller does not have
	 *         <code>AdminPermission[System Bundle,STARTLEVEL]</code> and the
	 *         Java runtime environment supports permissions.
	 */
	public void setInitialBundleStartLevel(int startlevel);

	/**
	 * Returns whether the specified bundle's autostart setting indicates the
	 * bundle must be started.
	 * <p>
	 * The autostart setting of a bundle indicates whether the bundle is to be
	 * started when its start level is reached.
	 * 
	 * @param bundle The bundle whose autostart setting is to be examined.
	 * @return <code>true</code> if the autostart setting of the bundle
	 *         indicates the bundle is to be started. <code>false</code>
	 *         otherwise.
	 * @throws java.lang.IllegalArgumentException If the specified bundle has
	 *         been uninstalled or if the specified bundle was not created by
	 *         the same framework instance that registered this
	 *         <code>StartLevel</code> service.
	 * @see Bundle#START_TRANSIENT
	 */
	public boolean isBundlePersistentlyStarted(Bundle bundle);

	/**
	 * Returns whether the specified bundle's autostart setting indicates that
	 * the activation policy declared in the bundle's manifest must be used.
	 * <p>
	 * The autostart setting of a bundle indicates whether the bundle's declared
	 * activation policy is to be used when the bundle is started.
	 * 
	 * @param bundle The bundle whose autostart setting is to be examined.
	 * @return <code>true</code> if the bundle's autostart setting indicates the
	 *         activation policy declared in the manifest must be used.
	 *         <code>false</code> if the bundle must be eagerly activated.
	 * @throws java.lang.IllegalArgumentException If the specified bundle has
	 *         been uninstalled or if the specified bundle was not created by
	 *         the same framework instance that registered this
	 *         <code>StartLevel</code> service.
	 * @since 1.1
	 * @see Bundle#START_ACTIVATION_POLICY
	 */
	public boolean isBundleActivationPolicyUsed(Bundle bundle);
}
