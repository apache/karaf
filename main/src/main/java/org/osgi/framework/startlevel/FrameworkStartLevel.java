/*
 * Copyright (c) OSGi Alliance (2002, 2010). All Rights Reserved.
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

package org.osgi.framework.startlevel;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkListener;

/**
 * Query and modify the start level information for the framework. The start
 * level object for the framework can be obtained by calling
 * {@link Bundle#adapt(Class) bundle.adapt(FrameworkStartLevel.class)} on the
 * system bundle. Only the system bundle can be adapted to a FrameworkStartLevel
 * object.
 * 
 * <p>
 * The system bundle associated with this FrameworkStartLevel object can be
 * obtained by calling {@link BundleReference#getBundle()}.
 * 
 * @ThreadSafe
 * @noimplement
 * @version $Id: 2bca22671674ba50b8c6801d5d1df8e291fe2a9d $
 */
public interface FrameworkStartLevel extends BundleReference {
	/**
	 * Return the active start level value of the Framework.
	 * 
	 * If the Framework is in the process of changing the start level this
	 * method must return the active start level if this differs from the
	 * requested start level.
	 * 
	 * @return The active start level value of the Framework.
	 */
	int getStartLevel();

	/**
	 * Modify the active start level of the Framework and notify when complete.
	 * 
	 * <p>
	 * The Framework will move to the requested start level. This method will
	 * return immediately to the caller and the start level change will occur
	 * asynchronously on another thread. The specified {@code FrameworkListener}
	 * s are notified, in the order specified, when the start level change is
	 * complete. When the start level change completes normally, each specified
	 * {@code FrameworkListener} will be called with a Framework event of type
	 * {@code FrameworkEvent.STARTLEVEL_CHANGED}. If the start level change does
	 * not complete normally, each specified {@code FrameworkListener} will be
	 * called with a Framework event of type {@code FrameworkEvent.ERROR}.
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
	 * <li>Start bundles at the intermediate start level whose autostart setting
	 * indicate they must be started. They are started as described in the
	 * {@link Bundle#start(int)} method using the {@link Bundle#START_TRANSIENT}
	 * option. The {@link Bundle#START_ACTIVATION_POLICY} option must also be
	 * used if {@link BundleStartLevel#isActivationPolicyUsed()} returns
	 * {@code true} for the bundle.
	 * </ol>
	 * When this process completes after the specified start level is reached,
	 * the Framework will fire a Framework event of type
	 * {@code FrameworkEvent.STARTLEVEL_CHANGED} to announce it has moved to the
	 * specified start level.
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
	 * {@code FrameworkEvent.STARTLEVEL_CHANGED} to announce it has moved to the
	 * specified start level.
	 * 
	 * <p>
	 * If the specified start level is equal to the active start level, then no
	 * bundles are started or stopped, however, the Framework must fire a
	 * Framework event of type {@code FrameworkEvent.STARTLEVEL_CHANGED} to
	 * announce it has finished moving to the specified start level. This event
	 * may arrive before this method returns.
	 * 
	 * @param startlevel The requested start level for the Framework.
	 * @param listeners Zero or more listeners to be notified when the start
	 *        level change has been completed. The specified listeners do not
	 *        need to be otherwise registered with the framework. If a specified
	 *        listener is already registered with the framework, it will be
	 *        notified twice.
	 * @throws IllegalArgumentException If the specified start level is less
	 *         than or equal to zero.
	 * @throws SecurityException If the caller does not have
	 *         {@code AdminPermission[System Bundle,STARTLEVEL]} and the Java
	 *         runtime environment supports permissions.
	 */
	void setStartLevel(int startlevel, FrameworkListener... listeners);

	/**
	 * Return the initial start level value that is assigned to a Bundle when it
	 * is first installed.
	 * 
	 * @return The initial start level value for Bundles.
	 * @see #setInitialBundleStartLevel
	 */
	int getInitialBundleStartLevel();

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
	 * When a Bundle is installed via {@code BundleContext.installBundle}, it is
	 * assigned the initial bundle start level value.
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
	 *         {@code AdminPermission[System Bundle,STARTLEVEL]} and the Java
	 *         runtime environment supports permissions.
	 */
	void setInitialBundleStartLevel(int startlevel);
}
